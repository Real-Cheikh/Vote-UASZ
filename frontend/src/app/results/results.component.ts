import { Component, OnInit, OnDestroy, inject, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subscription, timeout, catchError, of, finalize } from 'rxjs';
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
          @if (wsConnected()) {
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
            Dernière mise à jour : <strong>{{ lastUpdated() | date:'HH:mm:ss' }}</strong>
          </div>
        </div>
      </div>

      <!-- Sélecteur d'élection -->
      <div class="card selector-card">
        <label class="form-label" style="font-weight: 600; margin-bottom: 0.5rem; display: block;">
          Sélectionner un scrutin clôturé avec résultats publiés :
        </label>
        <div class="selector-controls">
          <select
            class="form-control"
            [ngModel]="selectedElectionId()"
            (ngModelChange)="onElectionSelect($event)"
            [disabled]="loading() && publishedElections().length === 0"
            style="flex: 1; min-width: 260px;"
          >
            @for (e of publishedElections(); track e.id) {
              <option [value]="e.id">{{ e.titre }}</option>
            }
          </select>
          <button
            class="btn btn-outline btn-sm"
            (click)="reloadCurrent()"
            [disabled]="loading()"
            title="Actualiser les résultats de ce scrutin"
          >
            <span [class.spin]="loading()">🔄</span> Actualiser
          </button>
        </div>
      </div>

      <!-- État 1 : Chargement avec indicateur visuel -->
      @if (loading()) {
        <div class="card text-center loading-card" style="padding: 3.5rem;">
          <div class="spinner"></div>
          <p style="margin-top: 1.25rem; color: var(--text-muted); font-size: 1.05rem;">
            Chargement des résultats officiels...
          </p>
        </div>
      }

      <!-- État 4 : Erreur API avec message compréhensible et bouton Réessayer -->
      @else if (errorMessage()) {
        <div class="card error-card" style="padding: 2.5rem; text-align: center; border-left: 5px solid #dc2626;">
          <div style="font-size: 2.5rem; margin-bottom: 0.75rem;">⚠️</div>
          <h4 style="margin-bottom: 0.5rem; color: #b91c1c;">Information sur les résultats</h4>
          <p class="text-muted" style="margin-bottom: 1.5rem; max-width: 520px; margin-left: auto; margin-right: auto;">
            {{ errorMessage() }}
          </p>
          <div style="display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap;">
            <button class="btn btn-primary btn-sm" (click)="loadResults()">
              🔄 Réessayer
            </button>
            <a routerLink="/elections" class="btn btn-outline btn-sm">
              Consulter la liste des scrutins
            </a>
          </div>
        </div>
      }

      <!-- État 2 & 3 : Résultats disponibles -->
      @else if (results(); as res) {
        <!-- Bannière vainqueur (si candidat présent) -->
        @if (winnerCandidate; as winner) {
          <div class="card winner-card">
            <div class="winner-trophy">🏆</div>
            <div>
              <span class="winner-tag">
                {{ winner.voixObtenues > 0 ? 'Candidat en tête des suffrages' : 'Candidature enregistrée' }}
              </span>
              <h3>{{ winner.candidatePrenom }} {{ winner.candidateNom }}</h3>
              <p class="winner-stats">
                {{ winner.voixObtenues }} voix ({{ winner.pourcentage }}%) des suffrages exprimés
              </p>
            </div>
          </div>
        }

        <!-- Statistiques de participation -->
        <div class="grid grid-cols-4 stats-grid">
          <div class="card stat-card">
            <span class="stat-value">{{ res.inscritsTotal }}</span>
            <span class="stat-label">Électeurs Inscrits</span>
          </div>
          <div class="card stat-card">
            <span class="stat-value">{{ res.votantsTotal }}</span>
            <span class="stat-label">Votants (Émargements)</span>
          </div>
          <div class="card stat-card">
            <span class="stat-value">{{ res.bulletinsValides }}</span>
            <span class="stat-label">Bulletins Valides</span>
          </div>
          <div class="card stat-card highlight">
            <span class="stat-value">{{ res.tauxParticipation }}%</span>
            <span class="stat-label">Taux de Participation</span>
          </div>
        </div>

        <!-- Détail des voix par candidat -->
        <div class="card candidates-results-card">
          <div class="card-header flex-between">
            <div>
              <h3>Répartition des Suffrages Exprimés</h3>
              <p class="text-muted" style="font-size: 0.85rem; margin-top: 0.25rem;">
                Scrutin : <strong>{{ res.electionTitre }}</strong>
              </p>
            </div>
            <button
              (click)="downloadPdf()"
              class="btn btn-outline btn-sm"
              [disabled]="downloadingPdf()"
            >
              @if (downloadingPdf()) {
                <span class="spin">⏳</span> Génération du PDF...
              } @else {
                📄 Télécharger le Procès-Verbal Officiel (PDF)
              }
            </button>
          </div>

          <div class="results-list">
            @if (res.resultatsCandidats && res.resultatsCandidats.length > 0) {
              @for (cand of res.resultatsCandidats; track cand.candidacyId) {
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
            } @else {
              <div class="card text-center" style="padding: 2rem; background: #f8fafc; border: 1px dashed #cbd5e1;">
                <p class="text-muted" style="margin: 0;">Aucune candidature officielle enregistrée pour ce scrutin.</p>
              </div>
            }

            @if (res.bulletinsBlancs > 0) {
              <div class="result-item blank-result">
                <div class="result-info">
                  <span>Votes Blancs</span>
                  <span class="votes-count">{{ res.bulletinsBlancs }} votes</span>
                </div>
                <div class="progress-bar-bg">
                  <div
                    class="progress-bar-fill blank"
                    [style.width.%]="res.votantsTotal > 0 ? (res.bulletinsBlancs / res.votantsTotal) * 100 : 0"
                  ></div>
                </div>
              </div>
            }
          </div>
        </div>

        <div class="alert alert-info" style="margin-top: 1.5rem;">
          <strong>Certification cryptographique :</strong> Les résultats ci-dessus sont certifiés conformes au dépouillement officiel validé par la Commission électorale UASZ. Le procès-verbal PDF comporte une empreinte numérique SHA-256 infalsifiable.
        </div>
      }

      <!-- État 3 : Aucun résultat publié -->
      @else {
        <div class="card text-center" style="padding: 3.5rem;">
          <div style="font-size: 2.5rem; margin-bottom: 0.75rem;">📋</div>
          <p style="font-size: 1.05rem; color: var(--text-muted); margin-bottom: 1.5rem;">
            Aucun résultat officiel publié n'est disponible pour ce scrutin.
          </p>
          <a routerLink="/elections" class="btn btn-outline">
            Voir tous les scrutins publics
          </a>
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
    .selector-controls {
      display: flex;
      gap: 0.75rem;
      align-items: center;
      flex-wrap: wrap;
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
    .result-item {
      padding: 0.5rem 0;
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
    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #e2e8f0;
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto;
    }
    .spin {
      display: inline-block;
      animation: spin 1s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .error-card {
      background-color: #fef2f2;
      border-color: #fecaca;
    }
  `]
})
export class ResultsComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private apiService = inject(ApiService);
  private wsService = inject(WebSocketService);
  private cdr = inject(ChangeDetectorRef);

  // Signaux réactifs natifs pour compatibilité Zoneless Angular 21
  publishedElections = signal<Election[]>([]);
  selectedElectionId = signal<number | null>(null);
  results = signal<ElectionResult | null>(null);
  loading = signal<boolean>(true);
  errorMessage = signal<string>('');
  wsConnected = signal<boolean>(false);
  lastUpdated = signal<Date>(new Date());
  downloadingPdf = signal<boolean>(false);

  private wsSubscription: Subscription | null = null;
  private statusSubscription: Subscription | null = null;
  private pollingTimer: any = null;

  ngOnInit(): void {
    // 1. Initialiser le WebSocket STOMP/SockJS
    this.wsService.connect();

    this.statusSubscription = this.wsService.getConnectionStatus().subscribe((status) => {
      this.wsConnected.set(status);
      this.cdr.markForCheck();
    });

    // 2. Définir le fallback polling régulier toutes les 10 secondes si WebSocket indisponible
    this.pollingTimer = setInterval(() => {
      if (!this.wsConnected() && this.selectedElectionId()) {
        this.refreshResultsQuietly();
      }
    }, 10000);

    // 3. Charger la liste des scrutins publiés
    this.loadPublishedElections();
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
    const currentId = this.selectedElectionId();
    if (currentId) {
      this.wsService.unsubscribeFromElection(currentId);
    }
  }

  loadPublishedElections(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.apiService.getPublicElections().pipe(
      timeout(12000),
      catchError((err) => {
        return of({
          success: false,
          message: err?.name === 'TimeoutError'
            ? 'Le délai d’attente a expiré lors de la récupération des scrutins.'
            : (err?.error?.message || 'Impossible de joindre le serveur électoral pour récupérer les scrutins.'),
          data: []
        });
      })
    ).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const published = res.data.filter(e => e.statut === 'RESULTS_PUBLISHED' || e.statut === 'ARCHIVED');
          this.publishedElections.set(published);

          const idParam = this.route.snapshot.paramMap.get('id');
          if (idParam) {
            this.selectedElectionId.set(Number(idParam));
          } else if (published.length > 0) {
            this.selectedElectionId.set(published[0].id);
          }

          if (this.selectedElectionId()) {
            this.loadResults();
          } else {
            this.loading.set(false);
            this.errorMessage.set('Aucun scrutin clôturé avec résultats publiés pour le moment.');
            this.cdr.markForCheck();
          }
        } else {
          this.loading.set(false);
          this.errorMessage.set(res.message || 'Impossible de charger la liste des scrutins.');
          this.cdr.markForCheck();
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message || 'Erreur lors du chargement des scrutins.');
        this.cdr.markForCheck();
      }
    });
  }

  onElectionSelect(newId: any): void {
    const parsedId = Number(newId);
    if (!parsedId || parsedId === this.selectedElectionId()) return;

    // Se désabonner de l'ancien canal WebSocket
    const oldId = this.selectedElectionId();
    if (oldId) {
      this.wsService.unsubscribeFromElection(oldId);
    }
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
      this.wsSubscription = null;
    }

    this.selectedElectionId.set(parsedId);
    this.loadResults();
  }

  reloadCurrent(): void {
    this.loadResults();
  }

  loadResults(): void {
    const electionId = this.selectedElectionId();
    if (!electionId) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.apiService.getPublicResults(electionId).pipe(
      timeout(12000),
      catchError((err) => {
        return of({
          success: false,
          message: err?.name === 'TimeoutError'
            ? 'Le délai d’attente a expiré lors du chargement des résultats.'
            : (err?.error?.message || 'Résultats officiels non encore publiés pour ce scrutin.'),
          data: null
        });
      })
    ).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res && res.success && res.data) {
          this.results.set(res.data);
          this.lastUpdated.set(new Date());
          this.subscribeWebSocket(electionId);
        } else {
          this.results.set(null);
          this.errorMessage.set(res?.message || 'Résultats officiels non encore publiés pour ce scrutin.');
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading.set(false);
        this.results.set(null);
        this.errorMessage.set(err?.error?.message || 'Erreur de connexion au serveur pour récupérer les résultats.');
        this.cdr.markForCheck();
      }
    });
  }

  refreshResultsQuietly(): void {
    const electionId = this.selectedElectionId();
    if (!electionId) return;

    this.apiService.getPublicResults(electionId).pipe(
      timeout(8000),
      catchError(() => of(null))
    ).subscribe({
      next: (res) => {
        if (res && res.success && res.data) {
          this.results.set(res.data);
          this.lastUpdated.set(new Date());
          this.cdr.markForCheck();
        }
      }
    });
  }

  subscribeWebSocket(electionId: number): void {
    if (!electionId) return;
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }

    this.wsSubscription = this.wsService.subscribeToElection(electionId).subscribe({
      next: (updatedResult) => {
        if (updatedResult && updatedResult.electionId === this.selectedElectionId()) {
          this.results.set(updatedResult);
          this.lastUpdated.set(new Date());
          this.cdr.markForCheck();
        }
      }
    });
  }

  get winnerCandidate() {
    const res = this.results();
    if (!res || !res.resultatsCandidats || res.resultatsCandidats.length === 0) {
      return null;
    }
    const sorted = [...res.resultatsCandidats].sort((a, b) => b.voixObtenues - a.voixObtenues);
    return sorted[0];
  }

  downloadPdf(): void {
    const electionId = this.selectedElectionId();
    if (!electionId) return;

    this.downloadingPdf.set(true);
    this.apiService.downloadReportPdf(electionId).pipe(
      finalize(() => {
        this.downloadingPdf.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `PV_Officiel_Election_UASZ_${electionId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        alert('Erreur lors du téléchargement du procès-verbal officiel.');
      }
    });
  }
}
