import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApiService } from '../core/services/api.service';
import { AuthService } from '../core/services/auth.service';
import { Complaint, Election } from '../core/models/models';

@Component({
  selector: 'app-complaints',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container complaints-container">
      <div class="header-section">
        <div>
          <h2>Contentieux & Réclamations Électorales</h2>
          <p class="subtitle">Déposez et suivez vos contestations auprès de la Commission Électorale Universitaire.</p>
        </div>
        <button class="btn btn-primary btn-sm" (click)="activeTab = 'SUBMIT'">
          + Nouvelle Réclamation
        </button>
      </div>

      <!-- Navigation Onglets -->
      <div class="tabs-bar">
        <button class="tab-btn" [class.active]="activeTab === 'MY_LIST'" (click)="activeTab = 'MY_LIST'">
          📑 Mes Réclamations Déposées
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'SUBMIT'" (click)="activeTab = 'SUBMIT'">
          ✍️ Rédiger une Contestation
        </button>
      </div>

      @if (activeTab === 'MY_LIST') {
        @if (loading) {
          <div class="card text-center" style="padding: 3rem;">
            <p>Chargement de vos réclamations...</p>
          </div>
        } @else if (complaints.length === 0) {
          <div class="card text-center" style="padding: 3rem;">
            <p>Vous n'avez déposé aucune contestation pour le moment.</p>
          </div>
        } @else {
          <div class="complaints-list">
            @for (c of complaints; track c.id) {
              <div class="card complaint-card">
                <div class="card-header flex-between">
                  <div>
                    <span class="badge" [ngClass]="getBadgeClass(c.statut)">{{ c.statut }}</span>
                    <h3 style="margin-top: 0.5rem;">{{ c.objet }}</h3>
                  </div>
                  <small class="text-muted">Déposée le {{ c.dateDepot | date:'dd/MM/yyyy HH:mm' }}</small>
                </div>

                <p class="complaint-body">{{ c.description }}</p>

                @if (c.reponseCommission) {
                  <div class="commission-response">
                    <strong>Décision motivée de la Commission :</strong>
                    <p>{{ c.reponseCommission }}</p>
                    <small class="text-muted">Traitée le {{ c.dateTraitement | date:'dd/MM/yyyy HH:mm' }}</small>
                  </div>
                } @else {
                  <div class="pending-notice">
                    ⏳ Dossier en cours d'instruction par la Commission électorale.
                  </div>
                }
              </div>
            }
          </div>
        }
      } @else if (activeTab === 'SUBMIT') {
        <!-- FORMULAIRE DE DÉPÔT -->
        <div class="card form-card">
          <h3>Formulaire Officiel de Réclamation</h3>
          <p class="subtitle" style="margin-bottom: 1.5rem;">
            Conformément au règlement électoral de l'UASZ, toute contestation doit être motivée et comporter des faits précis.
          </p>

          @if (submitSuccess) {
            <div class="alert alert-success">{{ submitSuccess }}</div>
          }
          @if (submitError) {
            <div class="alert alert-danger">{{ submitError }}</div>
          }

          <form (ngSubmit)="onSubmitComplaint()">
            <div class="form-group">
              <label class="form-label">Scrutin Concerné</label>
              <select class="form-control" [(ngModel)]="newComplaint.electionId" name="electionId" required>
                <option [ngValue]="null" disabled>Sélectionnez l'élection faisant l'objet du recours</option>
                @for (e of elections; track e.id) {
                  <option [value]="e.id">{{ e.titre }} ({{ e.type }})</option>
                }
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">Objet du Recours</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="newComplaint.objet"
                name="objet"
                placeholder="Ex: Contestation de la liste d'émargement / Incident de vote"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">Description Détaillée des Faits</label>
              <textarea
                class="form-control"
                rows="5"
                [(ngModel)]="newComplaint.description"
                name="description"
                placeholder="Exposez avec précision les motifs de votre réclamation, les dates, heures et circonstances..."
                required
              ></textarea>
            </div>

            <button
              type="submit"
              class="btn btn-primary btn-block btn-lg"
              [disabled]="submitting || !newComplaint.electionId || !newComplaint.objet || !newComplaint.description"
            >
              @if (submitting) {
                <span>Enregistrement du recours...</span>
              } @else {
                <span>Transmettre ma Réclamation à la Commission</span>
              }
            </button>
          </form>
        </div>
      }
    </div>
  `,
  styles: [`
    .complaints-container {
      padding: 2.5rem 1.25rem;
    }
    .header-section {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }
    .subtitle {
      color: var(--text-muted);
      font-size: 0.95rem;
    }
    .tabs-bar {
      display: flex;
      gap: 1rem;
      border-bottom: 2px solid var(--border-color);
      margin-bottom: 2rem;
    }
    .tab-btn {
      padding: 0.75rem 1.25rem;
      background: none;
      border: none;
      border-bottom: 3px solid transparent;
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--text-muted);
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .tab-btn:hover, .tab-btn.active {
      color: var(--primary);
      border-bottom-color: var(--primary);
    }
    .complaints-list {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }
    .complaint-card {
      border-left: 4px solid var(--primary);
    }
    .flex-between {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }
    .complaint-body {
      font-size: 0.95rem;
      color: var(--text-main);
      margin-bottom: 1.25rem;
      line-height: 1.6;
    }
    .commission-response {
      background: var(--primary-light);
      padding: 1rem 1.25rem;
      border-radius: var(--radius-sm);
      border-left: 3px solid var(--primary);
      font-size: 0.9rem;
    }
    .commission-response p {
      margin: 0.5rem 0;
    }
    .pending-notice {
      background: #f8fafc;
      padding: 0.75rem 1rem;
      border-radius: var(--radius-sm);
      font-size: 0.85rem;
      color: var(--text-muted);
    }
    .form-card {
      max-width: 700px;
      margin: 0 auto;
      padding: 2.5rem;
    }
    .btn-block {
      width: 100%;
    }
  `]
})
export class ComplaintsComponent implements OnInit {
  authService = inject(AuthService);
  private apiService = inject(ApiService);

  activeTab: 'MY_LIST' | 'SUBMIT' = 'MY_LIST';
  complaints: Complaint[] = [];
  elections: Election[] = [];
  loading = true;

  newComplaint = {
    electionId: null as number | null,
    objet: '',
    description: ''
  };
  submitting = false;
  submitSuccess = '';
  submitError = '';

  ngOnInit(): void {
    this.loadMyComplaints();
    this.apiService.getPublicElections().subscribe((res) => {
      if (res.success && res.data) {
        this.elections = res.data;
      }
    });
  }

  loadMyComplaints(): void {
    this.loading = true;
    this.apiService.getMyComplaints().subscribe({
      next: (res) => {
        this.loading = false;
        this.complaints = res.data || [];
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onSubmitComplaint(): void {
    if (!this.newComplaint.electionId || !this.newComplaint.objet || !this.newComplaint.description) return;

    this.submitting = true;
    this.submitSuccess = '';
    this.submitError = '';

    this.apiService.submitComplaint({
      electionId: this.newComplaint.electionId,
      objet: this.newComplaint.objet,
      description: this.newComplaint.description
    }).subscribe({
      next: () => {
        this.submitting = false;
        this.submitSuccess = 'Votre réclamation a été transmise à la Commission électorale. Vous recevrez une réponse motivée dès son instruction.';
        this.newComplaint.objet = '';
        this.newComplaint.description = '';
        this.loadMyComplaints();
      },
      error: (err) => {
        this.submitting = false;
        this.submitError = err.error?.message || 'Erreur lors du dépôt de la réclamation.';
      }
    });
  }

  getBadgeClass(statut: string): string {
    switch (statut) {
      case 'RESOLVED': return 'badge-success';
      case 'UNDER_REVIEW': return 'badge-warning';
      case 'REJECTED': return 'badge-danger';
      default: return 'badge-secondary';
    }
  }
}
