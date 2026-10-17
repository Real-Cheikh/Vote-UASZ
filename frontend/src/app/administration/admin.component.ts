import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApiService } from '../core/services/api.service';
import { AuthService } from '../core/services/auth.service';
import { AuditEvent, Candidacy, College, Complaint, Election, ElectionResult } from '../core/models/models';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container admin-container">
      <div class="admin-header">
        <div>
          <span class="badge badge-primary">Espace Réservé • Commission Électorale & Super Admin</span>
          <h2>Tableau de Bord Administratif VoteUASZ</h2>
          <p class="subtitle">Supervision des scrutins, validation des listes, dépouillement officiel et audit.</p>
        </div>
      </div>

      <!-- Navigation des onglets d'administration -->
      <div class="admin-tabs">
        <button class="tab-btn" [class.active]="tab === 'ELECTIONS'" (click)="tab = 'ELECTIONS'">
          🗳️ Scrutins & Workflow
        </button>
        <button class="tab-btn" [class.active]="tab === 'CANDIDACIES'" (click)="tab = 'CANDIDACIES'">
          📋 Instruction Candidatures
        </button>
        <button class="tab-btn" [class.active]="tab === 'COUNTING'" (click)="tab = 'COUNTING'">
          ⚡ Dépouillement & PV
        </button>
        <button class="tab-btn" [class.active]="tab === 'COMPLAINTS'" (click)="tab = 'COMPLAINTS'">
          ⚖️ Contentieux ({{ pendingComplaintsCount }})
        </button>
        @if (authService.isSuperAdmin()) {
          <button class="tab-btn" [class.active]="tab === 'AUDIT'" (click)="tab = 'AUDIT'">
            🛡️ Registre d'Audit Chaîné
          </button>
        }
      </div>

      <!-- ONGLET 1 : SCRUTINS & CYCLE DE VIE -->
      @if (tab === 'ELECTIONS') {
        <div class="card" style="margin-bottom: 2rem;">
          <div class="flex-between">
            <h3>Scrutins Électoraux Configurés</h3>
            <button class="btn btn-primary btn-sm" (click)="showCreateModal = !showCreateModal">
              + Configurer un Nouveau Scrutin
            </button>
          </div>

          @if (showCreateModal) {
            <div class="create-election-form card" style="margin-top: 1.5rem; background: #f8fafc;">
              <h4>Nouveau Scrutin Universitaire</h4>
              <form (ngSubmit)="onCreateElection()" style="margin-top: 1rem;">
                <div class="grid grid-cols-2">
                  <div class="form-group">
                    <label class="form-label">Titre du Scrutin</label>
                    <input type="text" class="form-control" [(ngModel)]="newElection.titre" name="titre" required />
                  </div>
                  <div class="form-group">
                    <label class="form-label">Type d'Élection</label>
                    <select class="form-control" [(ngModel)]="newElection.type" name="type">
                      <option value="DELEGUE">Délégués Étudiants</option>
                      <option value="DIRECTEUR_UFR">Directeur d'UFR</option>
                      <option value="VICE_RECTEUR">Vice-Recteur</option>
                      <option value="AUTRE">Autre Scrutin</option>
                    </select>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Collège Électoral</label>
                    <select class="form-control" [(ngModel)]="newElection.collegeId" name="collegeId" required>
                      @for (c of colleges; track c.id) {
                        <option [value]="c.id">{{ c.nom }} ({{ c.categorie }})</option>
                      }
                    </select>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Vote Blanc Autorisé</label>
                    <select class="form-control" [(ngModel)]="newElection.permettreVoteBlanc" name="permettreVoteBlanc">
                      <option [ngValue]="true">Oui (Option Vote Blanc permise)</option>
                      <option [ngValue]="false">Non</option>
                    </select>
                  </div>
                  <div class="form-group">
                    <label class="form-label">Date d'Ouverture du Vote</label>
                    <input type="datetime-local" class="form-control" [(ngModel)]="newElection.dateOuverture" name="dateOuverture" required />
                  </div>
                  <div class="form-group">
                    <label class="form-label">Date de Clôture du Vote</label>
                    <input type="datetime-local" class="form-control" [(ngModel)]="newElection.dateCloture" name="dateCloture" required />
                  </div>
                </div>
                <div class="form-group">
                  <label class="form-label">Description & Règles</label>
                  <textarea class="form-control" rows="2" [(ngModel)]="newElection.description" name="description"></textarea>
                </div>
                <div style="display: flex; gap: 0.75rem;">
                  <button type="submit" class="btn btn-primary">Enregistrer le Scrutin</button>
                  <button type="button" class="btn btn-outline" (click)="showCreateModal = false">Annuler</button>
                </div>
              </form>
            </div>
          }

          <div class="table-responsive" style="margin-top: 1.5rem;">
            <table class="table">
              <thead>
                <tr>
                  <th>Titre</th>
                  <th>Type</th>
                  <th>Collège</th>
                  <th>Statut Actuel</th>
                  <th>Action Workflow</th>
                </tr>
              </thead>
              <tbody>
                @for (e of elections; track e.id) {
                  <tr>
                    <td><strong>{{ e.titre }}</strong></td>
                    <td><span class="badge badge-primary">{{ e.type }}</span></td>
                    <td>{{ e.college?.nom }}</td>
                    <td>
                      <span class="badge" [ngClass]="getStatusBadge(e.statut)">{{ e.statut }}</span>
                    </td>
                    <td>
                      <div class="status-actions">
                        @if (e.statut === 'DRAFT') {
                          <button class="btn btn-outline btn-xs" (click)="changeStatus(e.id, 'CANDIDATURES_OPEN')">Ouvrir Candidatures</button>
                          <button class="btn btn-outline btn-xs" (click)="changeStatus(e.id, 'READY_TO_VOTE')">Prêt à Voter</button>
                        } @else if (e.statut === 'CANDIDATURES_OPEN') {
                          <button class="btn btn-outline btn-xs" (click)="changeStatus(e.id, 'CANDIDATURES_CLOSED')">Clôturer Dépôt</button>
                        } @else if (e.statut === 'CANDIDATURES_CLOSED') {
                          <button class="btn btn-outline btn-xs" (click)="changeStatus(e.id, 'VALIDATION')">Passer en Validation</button>
                        } @else if (e.statut === 'VALIDATION') {
                          <button class="btn btn-primary btn-xs" (click)="changeStatus(e.id, 'READY_TO_VOTE')">Valider Listes Définitives</button>
                        } @else if (e.statut === 'READY_TO_VOTE') {
                          <button class="btn btn-primary btn-xs" (click)="changeStatus(e.id, 'VOTING_OPEN')">🟢 Ouvrir le Scrutin</button>
                        } @else if (e.statut === 'VOTING_OPEN') {
                          <button class="btn btn-danger btn-xs" (click)="changeStatus(e.id, 'VOTING_CLOSED')">🛑 Clôturer le Vote</button>
                        } @else if (e.statut === 'VOTING_CLOSED') {
                          <span class="text-muted" style="font-size: 0.8rem;">Prêt pour dépouillement</span>
                        } @else if (e.statut === 'RESULTS_PUBLISHED') {
                          <button class="btn btn-outline btn-xs" (click)="changeStatus(e.id, 'ARCHIVED')">Archiver</button>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }

      <!-- ONGLET 2 : INSTRUCTION DES CANDIDATURES -->
      @if (tab === 'CANDIDACIES') {
        <div class="card">
          <div class="flex-between">
            <h3>Dossiers de Candidature Soumis</h3>
            <select class="form-control" style="width: auto;" [(ngModel)]="candidacyElectionId" (change)="loadAdminCandidacies()">
              @for (e of elections; track e.id) {
                <option [value]="e.id">{{ e.titre }}</option>
              }
            </select>
          </div>

          @if (adminCandidacies.length === 0) {
            <p style="padding: 2rem 0; text-align: center; color: var(--text-muted);">
              Aucun dossier déposé pour cette élection.
            </p>
          } @else {
            <div class="table-responsive" style="margin-top: 1.5rem;">
              <table class="table">
                <thead>
                  <tr>
                    <th>Candidat</th>
                    <th>Catégorie</th>
                    <th>Profession de Foi</th>
                    <th>Statut</th>
                    <th>Décision de la Commission</th>
                  </tr>
                </thead>
                <tbody>
                  @for (c of adminCandidacies; track c.id) {
                    <tr>
                      <td>
                        <strong>{{ c.candidat.prenom }} {{ c.candidat.nom }}</strong><br />
                        <small class="text-muted">{{ c.candidat.email }}</small>
                      </td>
                      <td>{{ c.candidat.categorie }}</td>
                      <td style="max-width: 250px;">
                        <small>« {{ c.professionFoi }} »</small>
                      </td>
                      <td>
                        <span class="badge" [ngClass]="c.statut === 'VALIDATED' ? 'badge-success' : (c.statut === 'REJECTED' ? 'badge-danger' : 'badge-warning')">
                          {{ c.statut }}
                        </span>
                      </td>
                      <td>
                        @if (c.statut === 'SUBMITTED') {
                          <div style="display: flex; gap: 0.5rem;">
                            <button class="btn btn-primary btn-xs" (click)="decideCandidacy(c.id, 'VALIDATED')">✓ Valider</button>
                            <button class="btn btn-danger btn-xs" (click)="decideCandidacy(c.id, 'REJECTED')">✕ Rejeter</button>
                          </div>
                        } @else {
                          <small class="text-muted">Décision arrêtée</small>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      }

      <!-- ONGLET 3 : DÉPOUILLEMENT & PROCÈS-VERBAL -->
      @if (tab === 'COUNTING') {
        <div class="card">
          <h3>Dépouillement des Scrutins & Procès-Verbaux PDF</h3>
          <p class="subtitle" style="margin-bottom: 1.5rem;">
            Seuls les scrutins formellement clôturés (VOTING_CLOSED) peuvent être dépouillés.
          </p>

          <div class="table-responsive">
            <table class="table">
              <thead>
                <tr>
                  <th>Scrutin</th>
                  <th>Statut</th>
                  <th>Clôture</th>
                  <th>Opérations de Scrutin</th>
                </tr>
              </thead>
              <tbody>
                @for (e of elections; track e.id) {
                  <tr>
                    <td><strong>{{ e.titre }}</strong></td>
                    <td><span class="badge" [ngClass]="getStatusBadge(e.statut)">{{ e.statut }}</span></td>
                    <td>{{ e.dateCloture | date:'dd/MM/yyyy HH:mm' }}</td>
                    <td>
                      <div style="display: flex; gap: 0.5rem; align-items: center;">
                        @if (e.statut === 'VOTING_CLOSED' || e.statut === 'COUNTING') {
                          <button class="btn btn-primary btn-xs" (click)="runCounting(e.id)">
                            ⚡ Lancer le Dépouillement
                          </button>
                        }
                        @if (e.statut === 'COUNTING') {
                          <button class="btn btn-secondary btn-xs" (click)="publishOfficialResults(e.id)">
                            📢 Publier les Résultats
                          </button>
                        }
                        @if (e.statut === 'RESULTS_PUBLISHED' || e.statut === 'COUNTING') {
                          <button class="btn btn-outline btn-xs" (click)="downloadPdf(e.id)">
                            📄 Télécharger le PV PDF
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }

      <!-- ONGLET 4 : CONTENTIEUX -->
      @if (tab === 'COMPLAINTS') {
        <div class="card">
          <h3>Arbitrage des Réclamations Électorales</h3>
          <p class="subtitle" style="margin-bottom: 1.5rem;">Instruction et motivation des décisions sur les recours.</p>

          @if (complaints.length === 0) {
            <p style="padding: 2rem 0; text-align: center; color: var(--text-muted);">
              Aucune réclamation enregistrée.
            </p>
          } @else {
            <div class="table-responsive">
              <table class="table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Électeur</th>
                    <th>Objet</th>
                    <th>Description</th>
                    <th>Statut</th>
                    <th>Arbitrage</th>
                  </tr>
                </thead>
                <tbody>
                  @for (c of complaints; track c.id) {
                    <tr>
                      <td><small>{{ c.dateDepot | date:'dd/MM/yyyy' }}</small></td>
                      <td><strong>{{ c.user?.prenom }} {{ c.user?.nom }}</strong></td>
                      <td><strong>{{ c.objet }}</strong></td>
                      <td style="max-width: 250px;"><small>{{ c.description }}</small></td>
                      <td>
                        <span class="badge" [ngClass]="c.statut === 'RESOLVED' ? 'badge-success' : (c.statut === 'REJECTED' ? 'badge-danger' : 'badge-warning')">
                          {{ c.statut }}
                        </span>
                      </td>
                      <td>
                        @if (c.statut === 'SUBMITTED' || c.statut === 'UNDER_REVIEW') {
                          <div style="display: flex; flex-direction: column; gap: 0.35rem;">
                            <button class="btn btn-primary btn-xs" (click)="resolveComplaintPrompt(c.id, 'RESOLVED')">Motiver Accord</button>
                            <button class="btn btn-danger btn-xs" (click)="resolveComplaintPrompt(c.id, 'REJECTED')">Motiver Rejet</button>
                          </div>
                        } @else {
                          <small class="text-muted">Dossier clôturé</small>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      }

      <!-- ONGLET 5 : AUDIT IMMUABLE -->
      @if (tab === 'AUDIT' && authService.isSuperAdmin()) {
        <div class="card">
          <div class="flex-between">
            <div>
              <h3>Registre d'Audit Immuable (Hash-Chained)</h3>
              <p class="subtitle">Traçabilité cryptographique complète des opérations administratives et électorales.</p>
            </div>
            <button class="btn btn-outline btn-sm" (click)="loadAuditLogs()">Actualiser</button>
          </div>

          <div class="table-responsive" style="margin-top: 1.5rem;">
            <table class="table" style="font-size: 0.825rem;">
              <thead>
                <tr>
                  <th>Horodatage</th>
                  <th>Événement</th>
                  <th>Acteur</th>
                  <th>Résultat</th>
                  <th>Détails</th>
                  <th>Empreinte SHA-256 Chaînée</th>
                </tr>
              </thead>
              <tbody>
                @for (a of auditLogs; track a.id) {
                  <tr>
                    <td><small>{{ a.horodatage | date:'dd/MM/yyyy HH:mm:ss' }}</small></td>
                    <td><span class="badge badge-primary">{{ a.eventType }}</span></td>
                    <td><strong>{{ a.acteur }}</strong></td>
                    <td>
                      <span class="badge" [ngClass]="a.resultat === 'SUCCESS' ? 'badge-success' : 'badge-danger'">
                        {{ a.resultat }}
                      </span>
                    </td>
                    <td>{{ a.details }}</td>
                    <td><code style="font-size: 0.72rem; color: #0284c7;">{{ a.empreinteChaine?.substring(0, 16) }}...</code></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .admin-container {
      padding: 2.5rem 1.25rem;
    }
    .admin-header {
      margin-bottom: 2rem;
    }
    .subtitle {
      color: var(--text-muted);
      font-size: 0.95rem;
      margin-top: 0.25rem;
    }
    .admin-tabs {
      display: flex;
      gap: 0.75rem;
      border-bottom: 2px solid var(--border-color);
      margin-bottom: 2rem;
      overflow-x: auto;
    }
    .tab-btn {
      padding: 0.75rem 1.25rem;
      background: none;
      border: none;
      border-bottom: 3px solid transparent;
      font-size: 0.925rem;
      font-weight: 700;
      color: var(--text-muted);
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s ease;
    }
    .tab-btn:hover, .tab-btn.active {
      color: var(--primary);
      border-bottom-color: var(--primary);
    }
    .flex-between {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .status-actions {
      display: flex;
      gap: 0.35rem;
      flex-wrap: wrap;
    }
    .btn-xs {
      padding: 0.3rem 0.6rem;
      font-size: 0.75rem;
    }
  `]
})
export class AdminComponent implements OnInit {
  authService = inject(AuthService);
  private apiService = inject(ApiService);

  tab: 'ELECTIONS' | 'CANDIDACIES' | 'COUNTING' | 'COMPLAINTS' | 'AUDIT' = 'ELECTIONS';

  elections: Election[] = [];
  colleges: College[] = [];
  adminCandidacies: Candidacy[] = [];
  candidacyElectionId: number | null = null;
  complaints: Complaint[] = [];
  auditLogs: AuditEvent[] = [];

  showCreateModal = false;
  newElection = {
    titre: '',
    type: 'DELEGUE',
    collegeId: null as number | null,
    permettreVoteBlanc: true,
    dateOuverture: '',
    dateCloture: '',
    description: ''
  };

  ngOnInit(): void {
    this.loadElections();
    this.loadColleges();
    this.loadComplaints();
    if (this.authService.isSuperAdmin()) {
      this.loadAuditLogs();
    }
  }

  loadElections(): void {
    this.apiService.getAllElections().subscribe((res) => {
      if (res.success && res.data) {
        this.elections = res.data;
        if (this.elections.length > 0 && !this.candidacyElectionId) {
          this.candidacyElectionId = this.elections[0].id;
          this.loadAdminCandidacies();
        }
      }
    });
  }

  loadColleges(): void {
    this.apiService.getColleges().subscribe((res) => {
      if (res.success && res.data) {
        this.colleges = res.data;
        if (this.colleges.length > 0 && !this.newElection.collegeId) {
          this.newElection.collegeId = this.colleges[0].id;
        }
      }
    });
  }

  loadAdminCandidacies(): void {
    if (!this.candidacyElectionId) return;
    this.apiService.getAllCandidatesByElection(this.candidacyElectionId).subscribe((res) => {
      if (res.success && res.data) {
        this.adminCandidacies = res.data;
      }
    });
  }

  loadComplaints(): void {
    this.apiService.getPublicElections().subscribe((res) => {
      if (res.success && res.data && res.data.length > 0) {
        this.apiService.getComplaintsByElection(res.data[0].id).subscribe((cRes) => {
          if (cRes.success && cRes.data) {
            this.complaints = cRes.data;
          }
        });
      }
    });
  }

  loadAuditLogs(): void {
    this.apiService.getAuditEvents(0, 30).subscribe((res) => {
      if (res.success && res.data && res.data.content) {
        this.auditLogs = res.data.content;
      }
    });
  }

  changeStatus(id: number, statut: string): void {
    const motif = prompt('Motif officiel du changement de statut du scrutin :', 'Instruction de la Commission Électorale');
    if (!motif) return;

    this.apiService.updateElectionStatus(id, statut, motif).subscribe({
      next: () => {
        this.loadElections();
      },
      error: (err) => {
        alert(err.error?.message || 'Erreur lors du changement de statut.');
      }
    });
  }

  onCreateElection(): void {
    if (!this.newElection.titre || !this.newElection.collegeId) return;

    const payload = {
      ...this.newElection,
      dateOuverture: new Date(this.newElection.dateOuverture).toISOString(),
      dateCloture: new Date(this.newElection.dateCloture).toISOString()
    };

    this.apiService.createElection(payload).subscribe({
      next: () => {
        this.showCreateModal = false;
        this.loadElections();
      },
      error: (err) => {
        alert(err.error?.message || "Erreur lors de la création de l'élection.");
      }
    });
  }

  decideCandidacy(id: number, decision: 'VALIDATED' | 'REJECTED'): void {
    const motif = prompt(`Motif de la décision (${decision}) :`, decision === 'VALIDATED' ? "Dossier conforme aux critères d'éligibilité" : 'Pièces non conformes');
    if (!motif) return;

    this.apiService.reviewCandidacy(id, decision, motif).subscribe({
      next: () => {
        this.loadAdminCandidacies();
      },
      error: (err) => {
        alert(err.error?.message || 'Erreur lors de la validation.');
      }
    });
  }

  runCounting(electionId: number): void {
    if (!confirm('Êtes-vous sûr de vouloir lancer le dépouillement officiel des bulletins anonymes scellés ?')) return;

    this.apiService.countVotes(electionId).subscribe({
      next: (res) => {
        alert(`Dépouillement terminé avec succès ! ${res.data.votantsTotal} bulletins ont été dépouillés.`);
        this.loadElections();
      },
      error: (err) => {
        alert(err.error?.message || 'Erreur lors du dépouillement.');
      }
    });
  }

  publishOfficialResults(electionId: number): void {
    if (!confirm('Confirmez-vous la publication officielle et la diffusion en direct des résultats aux électeurs ?')) return;

    this.apiService.publishResults(electionId).subscribe({
      next: () => {
        alert('Résultats officiellement publiés et diffusés en temps réel !');
        this.loadElections();
      },
      error: (err) => {
        alert(err.error?.message || 'Erreur lors de la publication.');
      }
    });
  }

  downloadPdf(electionId: number): void {
    this.apiService.downloadReportPdf(electionId).subscribe((blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `PV_Officiel_UASZ_${electionId}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  resolveComplaintPrompt(id: number, statut: 'RESOLVED' | 'REJECTED'): void {
    const reponse = prompt('Motivation de la décision de la Commission :', 'Réclamation instruite conformément au code électoral.');
    if (!reponse) return;

    this.apiService.resolveComplaint(id, statut, reponse).subscribe({
      next: () => {
        this.loadComplaints();
      },
      error: (err) => {
        alert(err.error?.message || "Erreur lors de l'arbitrage.");
      }
    });
  }

  get pendingComplaintsCount(): number {
    return this.complaints.filter(c => c.statut === 'SUBMITTED').length;
  }

  getStatusBadge(statut: string): string {
    switch (statut) {
      case 'VOTING_OPEN': return 'badge-warning';
      case 'RESULTS_PUBLISHED': return 'badge-success';
      case 'CANDIDATURES_OPEN': return 'badge-primary';
      default: return 'badge-secondary';
    }
  }
}
