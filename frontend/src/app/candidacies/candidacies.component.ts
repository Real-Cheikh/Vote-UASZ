import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApiService } from '../core/services/api.service';
import { AuthService } from '../core/services/auth.service';
import { Candidacy, Election } from '../core/models/models';

@Component({
  selector: 'app-candidacies',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container candidacies-container">
      <div class="header-section">
        <div>
          <h2>Candidatures & Professions de Foi</h2>
          <p class="subtitle">Consultez les programmes électoraux officiels ou déposez votre dossier de candidature.</p>
        </div>
        @if (authService.isAuthenticated()) {
          <button class="btn btn-primary btn-sm" (click)="activeTab = 'APPLY'">
            + Déposer une Candidature
          </button>
        }
      </div>

      <!-- Navigation Onglets -->
      <div class="tabs-bar">
        <button class="tab-btn" [class.active]="activeTab === 'LIST'" (click)="activeTab = 'LIST'">
          📋 Liste Officielle des Candidats
        </button>
        @if (authService.isAuthenticated()) {
          <button class="tab-btn" [class.active]="activeTab === 'APPLY'" (click)="activeTab = 'APPLY'">
            ✍️ Postuler à un Scrutin
          </button>
        }
      </div>

      @if (activeTab === 'LIST') {
        <!-- Sélecteur d'élection -->
        <div class="election-select-box card">
          <label class="form-label">Sélectionner un scrutin pour afficher les candidats :</label>
          <select class="form-control" [(ngModel)]="selectedElectionId" (change)="onElectionChange()">
            @for (e of elections; track e.id) {
              <option [value]="e.id">{{ e.titre }} ({{ e.type }})</option>
            }
          </select>
        </div>

        @if (loadingCandidates) {
          <div class="card text-center" style="padding: 3rem;">
            <p>Chargement des candidats validés...</p>
          </div>
        } @else if (candidates.length === 0) {
          <div class="card text-center" style="padding: 3rem;">
            <p>Aucun candidat officiellement validé pour ce scrutin à ce stade.</p>
          </div>
        } @else {
          <div class="grid grid-cols-2">
            @for (c of candidates; track c.id) {
              <div class="card candidate-card">
                <div class="candidate-header">
                  <div class="avatar-large">
                    @if (c.photoUrl) {
                      <img [src]="c.photoUrl" alt="Photo" />
                    } @else {
                      <div class="avatar-ph">{{ c.candidat.prenom.charAt(0) }}{{ c.candidat.nom.charAt(0) }}</div>
                    }
                  </div>
                  <div>
                    <span class="badge badge-success">Candidature Validée</span>
                    <h3>{{ c.candidat.prenom }} {{ c.candidat.nom }}</h3>
                    <p class="category">{{ c.candidat.categorie }}</p>
                  </div>
                </div>

                <div class="candidacy-body">
                  @if (c.professionFoi) {
                    <div class="quote-box">
                      <strong>Profession de foi :</strong>
                      <p>« {{ c.professionFoi }} »</p>
                    </div>
                  }

                  @if (c.biographie) {
                    <p class="bio">{{ c.biographie }}</p>
                  }
                </div>

                <div class="candidacy-footer">
                  <small class="text-muted">Dossier déposé le {{ c.dateSoumission | date:'dd/MM/yyyy' }}</small>
                </div>
              </div>
            }
          </div>
        }
      } @else if (activeTab === 'APPLY') {
        <!-- FORMULAIRE DE DÉPÔT DE CANDIDATURE -->
        <div class="card form-card">
          <h3>Dépôt de Dossier de Candidature</h3>
          <p class="subtitle" style="margin-bottom: 1.5rem;">
            Soumettez votre profession de foi et vos pièces justificatives pour instruction par la Commission électorale.
          </p>

          @if (applySuccess) {
            <div class="alert alert-success">{{ applySuccess }}</div>
          }
          @if (applyError) {
            <div class="alert alert-danger">{{ applyError }}</div>
          }

          <form (ngSubmit)="submitCandidacy()">
            <div class="form-group">
              <label class="form-label">Scrutin Visé</label>
              <select class="form-control" [(ngModel)]="newCandidacy.electionId" name="electionId" required>
                <option [ngValue]="null" disabled>Sélectionnez une élection ouverte aux dépôts</option>
                @for (e of candidaciesOpenElections; track e.id) {
                  <option [value]="e.id">{{ e.titre }} ({{ e.college?.nom }})</option>
                }
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">Profession de foi (Programme électoral)</label>
              <textarea
                class="form-control"
                rows="4"
                [(ngModel)]="newCandidacy.professionFoi"
                name="professionFoi"
                placeholder="Décrivez vos engagements, vos propositions et vos axes de travail..."
                required
              ></textarea>
            </div>

            <div class="form-group">
              <label class="form-label">Biographie / Présentation</label>
              <textarea
                class="form-control"
                rows="3"
                [(ngModel)]="newCandidacy.biographie"
                name="biographie"
                placeholder="Présentez votre parcours académique, vos mandats passés..."
              ></textarea>
            </div>

            <button type="submit" class="btn btn-primary btn-block btn-lg" [disabled]="submitting || !newCandidacy.electionId || !newCandidacy.professionFoi">
              @if (submitting) {
                <span>Transmission du dossier...</span>
              } @else {
                <span>Déposer officiellement ma Candidature</span>
              }
            </button>
          </form>
        </div>
      }
    </div>
  `,
  styles: [`
    .candidacies-container {
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
    .election-select-box {
      margin-bottom: 2rem;
      padding: 1.25rem;
    }
    .candidate-card {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .candidate-header {
      display: flex;
      gap: 1.25rem;
      align-items: center;
      margin-bottom: 1.25rem;
    }
    .avatar-large img, .avatar-ph {
      width: 70px;
      height: 70px;
      border-radius: 50%;
      object-fit: cover;
    }
    .avatar-ph {
      background: var(--primary);
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.5rem;
      font-weight: 800;
    }
    .category {
      font-size: 0.8rem;
      color: var(--text-muted);
    }
    .quote-box {
      background: #f8fafc;
      padding: 1rem;
      border-left: 3px solid var(--secondary);
      border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
      font-size: 0.875rem;
      margin-bottom: 1rem;
    }
    .quote-box p {
      margin-top: 0.35rem;
      font-style: italic;
    }
    .bio {
      font-size: 0.85rem;
      color: var(--text-muted);
      line-height: 1.5;
    }
    .candidacy-footer {
      border-top: 1px solid var(--border-color);
      padding-top: 0.75rem;
      margin-top: 1rem;
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
export class CandidaciesComponent implements OnInit {
  authService = inject(AuthService);
  private apiService = inject(ApiService);

  activeTab: 'LIST' | 'APPLY' = 'LIST';
  elections: Election[] = [];
  candidaciesOpenElections: Election[] = [];
  selectedElectionId: number | null = null;
  candidates: Candidacy[] = [];
  loadingCandidates = false;

  newCandidacy = {
    electionId: null as number | null,
    professionFoi: '',
    biographie: ''
  };
  submitting = false;
  applySuccess = '';
  applyError = '';

  ngOnInit(): void {
    this.apiService.getPublicElections().subscribe((res) => {
      if (res.success && res.data) {
        this.elections = res.data;
        this.candidaciesOpenElections = res.data.filter(e => e.statut === 'CANDIDATURES_OPEN');
        if (this.elections.length > 0) {
          this.selectedElectionId = this.elections[0].id;
          this.loadCandidates();
        }
      }
    });
  }

  onElectionChange(): void {
    if (this.selectedElectionId) {
      this.loadCandidates();
    }
  }

  loadCandidates(): void {
    if (!this.selectedElectionId) return;
    this.loadingCandidates = true;
    this.apiService.getPublicCandidates(this.selectedElectionId).subscribe({
      next: (res) => {
        this.loadingCandidates = false;
        this.candidates = res.data || [];
      },
      error: () => {
        this.loadingCandidates = false;
      }
    });
  }

  submitCandidacy(): void {
    if (!this.newCandidacy.electionId || !this.newCandidacy.professionFoi) return;

    this.submitting = true;
    this.applySuccess = '';
    this.applyError = '';

    this.apiService.applyCandidacy({
      electionId: this.newCandidacy.electionId,
      professionFoi: this.newCandidacy.professionFoi,
      biographie: this.newCandidacy.biographie
    }).subscribe({
      next: () => {
        this.submitting = false;
        this.applySuccess = 'Votre candidature a été soumise avec succès ! Elle est désormais transmise à la Commission électorale pour instruction.';
        this.newCandidacy.professionFoi = '';
        this.newCandidacy.biographie = '';
      },
      error: (err) => {
        this.submitting = false;
        this.applyError = err.error?.message || 'Erreur lors du dépôt de candidature.';
      }
    });
  }
}
