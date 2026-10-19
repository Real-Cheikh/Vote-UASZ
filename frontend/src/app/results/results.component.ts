import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService } from '../core/services/api.service';
import { WebSocketService } from '../core/services/websocket.service';
import { Election, ElectionResult } from '../core/models/models';

@Component({
  selector: 'app-results',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container results-container">
      <div class="results-header flex-between">
        <div>
          <div class="badge badge-secondary">Résultats Officiels Certifiés</div>
          <h2>Dépouillement & Suffrages Exprimés</h2>
          <p class="subtitle">Publication en temps réel supervisée par la Commission Électorale UASZ.</p>
        </div>

        <!-- Indicateur de synchronisation temps réel -->
        <div class="sync-status-box">
          @if (wsConnected) {
            <span class="status-pill connected">
              <span class="dot-indicator green-pulse"></span>
              En Direct (WebSocket Actif)
            </span>
          } @else {
            <span class="status-pill fallback">
              <span class="dot-indicator yellow"></span>
              Synchronisation Auto (10s)
            </span>
          }
          <div class="last-sync-label">
            Dernière mise à jour : <strong>{{ lastUpdated | date:'HH:mm:ss' }}</strong>
          </div>
        </div>
      </div>

      <!-- Sélecteur d'élection -->
      <div class="card selector-card">
        <label class="form-label">Sélectionner un scrutin clôturé avec résultats publiés :</label>
        <select class="form-control" [(ngModel)]="selectedElectionId" (change)="onElectionChange()">
          @for (e of publishedElections; track e.id) {
            <option [value]="e.id">{{ e.titre }}</option>
          }
        </select>
      </div>

      @if (loading) {
        <div class="card text-center" style="padding: 3rem;">
          <p>Chargement des résultats officiels...</p>
        </div>
      } @else if (errorMessage) {
        <div class="card text-center" style="padding: 3rem;">
          <p>{{ errorMessage }}</p>
        </div>
      } @else if (results) {
        <!-- Bannière vainqueur (si candidat en tête) -->
        @if (winnerCandidate) {
          <div class="card winner-card">
            <div class="winner-trophy">🏆</div>
            <div>
              <span class="winner-tag">Candidat en tête des suffrages</span>
              <h3>{{ winnerCandidate.candidatePrenom }} {{ winnerCandidate.candidateNom }}</h3>
              <p class="winner-stats">
                {{ winnerCandidate.voixObtenues }} voix ({{ winnerCandidate.pourcentage }}%) des suffrages exprimés
              </p>
            </div>
          </div>
        }

        <!-- Statistiques de participation -->
        <div class="grid grid-cols-4 stats-grid">
          <div class="card stat-card">
            <span class="stat-value">{{ results.inscritsTotal }}</span>
            <span class="stat-label">Électeurs Inscrits</span>
          </div>
          <div class="card stat-card">
            <span class="stat-value">{{ results.votantsTotal }}</span>
            <span class="stat-label">Votants (Émargements)</span>
          </div>
          <div class="card stat-card">
            <span class="stat-value">{{ results.bulletinsValides }}</span>
            <span class="stat-label">Bulletins Valides</span>
          </div>
          <div class="card stat-card highlight">
            <span class="stat-value">{{ results.tauxParticipation }}%</span>
            <span class="stat-label">Taux de Participation</span>
          </div>
        </div>

        <!-- Détail des voix par candidat -->
        <div class="card candidates-results-card">
          <div class="card-header flex-between">
            <h3>Répartition des Suffrages Exprimés</h3>
            <button (click)="downloadPdf()" class="btn btn-outline btn-sm">
              📄 Télécharger le Procès-Verbal Officiel (PDF)
            </button>
          </div>

          <div class="results-list">
            @for (cand of results.resultatsCandidats; track cand.candidacyId) {
              <div class="result-item">
                <div class="result-info">
                  <strong>{{ cand.candidatePrenom }} {{ cand.candidateNom }}</strong>
                  <span class="votes-count">{{ cand.voixObtenues }} voix • {{ cand.pourcentage }}%</span>
                </div>
                <div class="progress-bar-bg">
                  <div class="progress-bar-fill" [style.width.%]="cand.pourcentage"></div>
                </div>
              </div>
            }

            @if (results.bulletinsBlancs > 0) {
              <div class="result-item blank-result">
                <div class="result-info">
                  <span>Votes Blancs</span>
                  <span class="votes-count">{{ results.bulletinsBlancs }} votes</span>
                </div>
                <div class="progress-bar-bg">
                  <div class="progress-bar-fill blank" [style.width.%]="(results.bulletinsBlancs / results.votantsTotal) * 100"></div>
                </div>
              </div>
            }
          </div>
        </div>

        <div class="alert alert-info" style="margin-top: 1.5rem;">
          <strong>Certification cryptographique :</strong> Les résultats ci-dessus sont certifiés conformes au dépouillement effectué par la Commission électorale UASZ. Le procès-verbal officiel PDF comporte une empreinte numérique SHA-256 infalsifiable.
        </div>
      }
    </div>
  `,
  styles: [`
    .results-container {
      padding: 2.5rem 1.25rem;
    }
    .results-header {
      margin-bottom: 2rem;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .subtitle {
      color: var(--text-muted);
      font-size: 0.95rem;
    }
    .sync-status-box {
      text-align: right;
    }
    .status-pill {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.35rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.02em;
    }
    .status-pill.connected {
      background: #dcfce7;
      color: #15803d;
      border: 1px solid #bbf7d0;
    }
    .status-pill.fallback {
      background: #fef9c3;
      color: #a16207;
      border: 1px solid #fef08a;
    }
    .dot-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      display: inline-block;
    }
    .green-pulse {
      background: #22c55e;
      box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
      animation: pulse 1.6s infinite;
    }
    .yellow {
      background: #eab308;
    }
    @keyframes pulse {
      0% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
      70% { box-shadow: 0 0 0 6px rgba(34, 197, 94, 0); }
      100% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
    }
    .last-sync-label {
      font-size: 0.75rem;
      color: var(--text-muted);
      margin-top: 0.25rem;
    }
    .selector-card {
      margin-bottom: 2rem;
      padding: 1.25rem;
    }
    .winner-card {
      display: flex;
      align-items: center;
      gap: 1.5rem;
      background: linear-gradient(135deg, #fef9e7, #ffffff);
      border: 2px solid var(--secondary);
      margin-bottom: 2rem;
    }
    .winner-trophy {
      font-size: 3rem;
    }
    .winner-tag {
      font-size: 0.75rem;
      font-weight: 700;
      color: #854d0e;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .winner-stats {
      font-size: 0.95rem;
      color: var(--primary);
      font-weight: 700;
      margin-top: 0.25rem;
    }
    .stats-grid {
      margin-bottom: 2rem;
    }
    .stat-card {
      text-align: center;
      padding: 1.5rem 1rem;
    }
    .stat-card.highlight {
      background: var(--primary-light);
      border-color: #86efac;
    }
    .stat-value {
      display: block;
      font-size: 1.75rem;
      font-weight: 800;
      color: var(--dark);
      line-height: 1.1;
      margin-bottom: 0.35rem;
    }
    .stat-card.highlight .stat-value {
      color: var(--primary);
    }
    .stat-label {
      font-size: 0.78rem;
      color: var(--text-muted);
      font-weight: 600;
    }
    .flex-between {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .results-list {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      padding-top: 1rem;
    }
    .result-info {
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.5rem;
      font-size: 0.95rem;
    }
    .votes-count {
      color: var(--text-muted);
      font-weight: 600;
    }
    .progress-bar-bg {
      height: 14px;
      background: #f1f5f9;
      border-radius: 9999px;
      overflow: hidden;
    }
    .progress-bar-fill {
      height: 100%;
      background: linear-gradient(90deg, var(--primary), #16a34a);
      border-radius: 9999px;
      transition: width 1s ease-in-out;
    }
    .progress-bar-fill.blank {
      background: #94a3b8;
    }
  `]
})
export class ResultsComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private apiService = inject(ApiService);
  private wsService = inject(WebSocketService);

  publishedElections: Election[] = [];
  selectedElectionId: number | null = null;
  results: ElectionResult | null = null;
  loading = true;
  errorMessage = '';

  wsConnected = false;
  lastUpdated: Date = new Date();

  private wsSubscription: Subscription | null = null;
  private statusSubscription: Subscription | null = null;
  private pollingTimer: any = null;

  ngOnInit(): void {
    // 1. Initialiser le WebSocket STOMP/SockJS
    this.wsService.connect();

    this.statusSubscription = this.wsService.getConnectionStatus().subscribe((status) => {
      this.wsConnected = status;
    });

    // 2. Définir le fallback polling régulier toutes les 10 secondes si WebSocket indisponible
    this.pollingTimer = setInterval(() => {
      if (!this.wsConnected && this.selectedElectionId) {
        this.refreshResultsQuietly();
      }
    }, 10000);

    // 3. Charger la liste des scrutins publiés
    this.apiService.getPublicElections().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.publishedElections = res.data.filter(e => e.statut === 'RESULTS_PUBLISHED' || e.statut === 'ARCHIVED');
          const idParam = this.route.snapshot.paramMap.get('id');

          if (idParam) {
            this.selectedElectionId = Number(idParam);
          } else if (this.publishedElections.length > 0) {
            this.selectedElectionId = this.publishedElections[0].id;
          }

          if (this.selectedElectionId) {
            this.loadResults();
          } else {
            this.loading = false;
            this.errorMessage = 'Aucun scrutin avec résultats publiés pour le moment.';
          }
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
    }
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
    if (this.statusSubscription) {
      this.statusSubscription.unsubscribe();
    }
    if (this.selectedElectionId) {
      this.wsService.unsubscribeFromElection(this.selectedElectionId);
    }
  }

  onElectionChange(): void {
    if (this.selectedElectionId) {
      this.loadResults();
    }
  }

  loadResults(): void {
    if (!this.selectedElectionId) return;

    this.loading = true;
    this.errorMessage = '';

    this.apiService.getPublicResults(this.selectedElectionId).subscribe({
      next: (res) => {
        this.loading = false;
        this.results = res.data;
        this.lastUpdated = new Date();
        this.subscribeWebSocket();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Résultats non encore publiés pour ce scrutin.';
      }
    });
  }

  refreshResultsQuietly(): void {
    if (!this.selectedElectionId) return;

    this.apiService.getPublicResults(this.selectedElectionId).subscribe({
      next: (res) => {
        if (res.data) {
          this.results = res.data;
          this.lastUpdated = new Date();
        }
      },
      error: () => {}
    });
  }

  subscribeWebSocket(): void {
    if (!this.selectedElectionId) return;
    if (this.wsSubscription) this.wsSubscription.unsubscribe();

    this.wsSubscription = this.wsService.subscribeToElection(this.selectedElectionId).subscribe({
      next: (updatedResult) => {
        this.results = updatedResult;
        this.lastUpdated = new Date();
      }
    });
  }

  get winnerCandidate() {
    if (!this.results || !this.results.resultatsCandidats || this.results.resultatsCandidats.length === 0) {
      return null;
    }
    const sorted = [...this.results.resultatsCandidats].sort((a, b) => b.voixObtenues - a.voixObtenues);
    return sorted[0];
  }

  downloadPdf(): void {
    if (!this.selectedElectionId) return;
    this.apiService.downloadReportPdf(this.selectedElectionId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `PV_Officiel_Election_UASZ_${this.selectedElectionId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        alert('Erreur lors du téléchargement du procès-verbal officiel.');
      }
    });
  }
}
