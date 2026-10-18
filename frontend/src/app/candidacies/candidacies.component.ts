import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { timeout, finalize } from 'rxjs';
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
          <h2>Espace Candidats & Professions de Foi</h2>
          <p class="subtitle">Consultez les candidatures enregistrées, postulez aux scrutins ouverts ou suivez vos dossiers en temps réel.</p>
        </div>
        @if (authService.isAuthenticated()) {
          <button class="btn btn-primary btn-sm" (click)="setTab('APPLY')">
            + Déposer une Candidature
          </button>
        }
      </div>

      <!-- Navigation Onglets -->
      <div class="tabs-bar">
        <button class="tab-btn" [class.active]="activeTab === 'LIST'" (click)="setTab('LIST')">
          📋 Liste des Candidats
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'APPLY'" (click)="setTab('APPLY')">
          ✍️ Postuler à un Scrutin
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'MY_DOSSIER'" (click)="setTab('MY_DOSSIER')">
          📁 Mes Candidatures @if (authService.isAuthenticated() && myCandidacies.length > 0) { ({{ myCandidacies.length }}) }
        </button>
      </div>

      <!-- ========================================== -->
      <!-- ONGLET 1 : LISTE DES CANDIDATS (PUBLIQUE)   -->
      <!-- ========================================== -->
      @if (activeTab === 'LIST') {
        <div class="election-select-box card">
          <div class="flex-between" style="flex-wrap: wrap; gap: 1rem;">
            <div style="flex: 1; min-width: 250px;">
              <label class="form-label">Filtrer par scrutin :</label>
              <select class="form-control" [(ngModel)]="selectedElectionId" (change)="onElectionChange()">
                <option [ngValue]="null">-- Tous les scrutins confondus --</option>
                @for (e of elections; track e.id) {
                  <option [ngValue]="e.id">{{ e.titre }} ({{ e.college?.nom || e.type }})</option>
                }
              </select>
            </div>
            <div>
              <button class="btn btn-outline btn-sm" (click)="loadCandidates()" [disabled]="loadingCandidates" style="margin-top: 1.5rem;">
                🔄 Actualiser la liste
              </button>
            </div>
          </div>
        </div>

        @if (loadingCandidates) {
          <div class="card text-center" style="padding: 3rem;">
            <p>Chargement des candidatures officielles depuis le serveur...</p>
          </div>
        } @else if (candidatesError) {
          <div class="alert alert-danger flex-between">
            <span>{{ candidatesError }}</span>
            <button class="btn btn-secondary btn-sm" (click)="loadCandidates()">Réessayer</button>
          </div>
        } @else if (candidates.length === 0) {
          <div class="card text-center" style="padding: 3rem;">
            <div style="font-size: 2.5rem; margin-bottom: 1rem;">📋</div>
            <h4>Aucun candidat enregistré pour ce scrutin à ce stade</h4>
            <p class="text-muted" style="max-width: 500px; margin: 0.5rem auto 1.5rem;">
              Les dossiers soumis apparaîtront ici dès leur dépôt officiel par les candidats.
            </p>
            @if (authService.isAuthenticated()) {
              <button class="btn btn-primary btn-sm" (click)="setTab('APPLY')">
                Être le premier à postuler →
              </button>
            }
          </div>
        } @else {
          <div class="candidates-count-bar">
            <span><strong>{{ candidates.length }}</strong> candidature(s) enregistrée(s)</span>
          </div>

          <div class="grid grid-cols-2" style="gap: 1.5rem;">
            @for (c of candidates; track c.id) {
              <div class="card candidate-card">
                <div>
                  <div class="candidate-header">
                    <div class="avatar-large">
                      @if (c.photoUrl) {
                        <img [src]="c.photoUrl" alt="Photo" />
                      } @else {
                        <div class="avatar-ph">{{ c.candidat.prenom.charAt(0) }}{{ c.candidat.nom.charAt(0) }}</div>
                      }
                    </div>
                    <div style="flex: 1;">
                      <div class="flex-between" style="margin-bottom: 0.25rem;">
                        <span class="badge" [ngClass]="getStatusBadge(c.statut)">
                          {{ getStatusLabel(c.statut) }}
                        </span>
                        <small class="text-muted">Dossier #{{ c.id }}</small>
                      </div>
                      <h3 style="margin: 0 0 0.25rem 0;">{{ c.candidat.prenom }} {{ c.candidat.nom }}</h3>
                      <p class="category" style="margin: 0;">Catégorie : {{ c.candidat.categorie }}</p>
                    </div>
                  </div>

                  <div class="candidate-meta-bar">
                    <div><strong>Scrutin :</strong> {{ c.electionTitre || 'Scrutin #' + c.electionId }}</div>
                    @if (c.collegeNom) {
                      <div><strong>Collège :</strong> {{ c.collegeNom }}</div>
                    }
                  </div>

                  <div class="candidacy-body">
                    @if (c.professionFoi) {
                      <div class="quote-box">
                        <strong>Profession de foi (Programme) :</strong>
                        <p>« {{ c.professionFoi }} »</p>
                      </div>
                    }

                    @if (c.biographie) {
                      <p class="bio"><strong>Présentation :</strong> {{ c.biographie }}</p>
                    }
                  </div>
                </div>

                <div class="candidacy-footer flex-between">
                  <small class="text-muted">Déposé le {{ c.dateSoumission | date:'dd/MM/yyyy à HH:mm' }}</small>
                  @if (c.photoUrl) {
                    <a [href]="c.photoUrl" target="_blank" class="btn btn-outline btn-xs">
                      🖼️ Affiche officielle
                    </a>
                  }
                </div>
              </div>
            }
          </div>
        }
      }

      <!-- ========================================== -->
      <!-- ONGLET 2 : POSTULER À UN SCRUTIN          -->
      <!-- ========================================== -->
      @if (activeTab === 'APPLY') {
        @if (!authService.isAuthenticated()) {
          <div class="card text-center" style="padding: 3rem; max-width: 650px; margin: 0 auto;">
            <div style="font-size: 3rem; margin-bottom: 1rem;">🔒</div>
            <h3>Connexion requise</h3>
            <p class="text-muted" style="margin-bottom: 1.5rem;">
              Pour postuler à un scrutin universitaire officiel, vous devez être authentifié avec votre compte électeur ou candidat de l'UASZ.
            </p>
            <a routerLink="/login" class="btn btn-primary">
              Se connecter à mon compte →
            </a>
          </div>
        } @else {
          <div class="card form-card">
            <h3>Dépôt de Dossier de Candidature</h3>
            <p class="subtitle" style="margin-bottom: 1.5rem;">
              Sélectionnez un scrutin actuellement ouvert et transmettez votre profession de foi ainsi que vos pièces justificatives.
            </p>

            @if (applySuccess) {
              <div class="alert alert-success" style="margin-bottom: 1.5rem;">
                {{ applySuccess }}
              </div>
            }
            @if (applyError) {
              <div class="alert alert-danger" style="margin-bottom: 1.5rem;">
                {{ applyError }}
              </div>
            }

            @if (isAlreadyAppliedForSelected) {
              <div class="alert alert-warning" style="margin-bottom: 1.5rem;">
                <div style="font-weight: 700; margin-bottom: 0.35rem;">
                  ⚠️ Candidature déjà enregistrée pour ce scrutin
                </div>
                <p style="margin: 0 0 0.75rem 0; font-size: 0.9rem;">
                  Vous avez déjà transmis un dossier pour ce scrutin le {{ alreadyAppliedCandidacy?.dateSoumission | date:'dd/MM/yyyy à HH:mm' }}.
                  Statut : <strong>{{ getStatusLabel(alreadyAppliedCandidacy?.statut) }}</strong>.
                </p>
                <button type="button" class="btn btn-outline btn-sm" (click)="setTab('MY_DOSSIER')">
                  Consulter mon dossier complet dans « Mes Candidatures » →
                </button>
              </div>
            }

            <form (ngSubmit)="submitCandidacy()">
              <!-- Sélection du scrutin ouvert -->
              <div class="form-group">
                <label class="form-label">Scrutin Visé (Phase Ouverte aux Candidatures) *</label>
                <select class="form-control" [(ngModel)]="newCandidacy.electionId" name="electionId" (ngModelChange)="onElectionSelectChange($event)" required>
                  <option [ngValue]="null" disabled selected>-- Sélectionnez un scrutin électoral ouvert --</option>
                  @for (e of candidaciesOpenElections; track e.id) {
                    <option [ngValue]="e.id">
                      {{ e.titre }} — Collège : {{ e.college?.nom }} ({{ e.college?.categorie }})
                    </option>
                  }
                </select>

                @if (candidaciesOpenElections.length === 0) {
                  <small style="color: #64748b; display: block; margin-top: 0.35rem;">
                    Aucun scrutin n'est actuellement en phase ouverte de dépôt de candidatures.
                  </small>
                }
              </div>

              <!-- Fiche d'information détaillée du scrutin sélectionné -->
              @if (selectedOpenElection) {
                <div class="scrutin-details-box">
                  <div class="flex-between" style="margin-bottom: 0.5rem;">
                    <strong style="color: var(--primary);">📋 Détails du Scrutin Sélectionné</strong>
                    <span class="badge badge-success">Candidatures Ouvertes</span>
                  </div>
                  <div class="grid grid-cols-2" style="font-size: 0.85rem; gap: 0.5rem; margin-bottom: 0.5rem;">
                    <div><strong>Collège électoral :</strong> {{ selectedOpenElection.college?.nom }}</div>
                    <div><strong>Catégorie concernée :</strong> {{ selectedOpenElection.college?.categorie }}</div>
                    <div><strong>Ouverture :</strong> {{ selectedOpenElection.dateOuverture | date:'dd/MM/yyyy HH:mm' }}</div>
                    <div><strong>Clôture :</strong> {{ selectedOpenElection.dateCloture | date:'dd/MM/yyyy HH:mm' }}</div>
                  </div>
                  @if (selectedOpenElection.description) {
                    <p style="font-size: 0.85rem; color: #475569; margin: 0.25rem 0 0 0;">
                      <em>{{ selectedOpenElection.description }}</em>
                    </p>
                  }

                  <!-- Indicateur d'éligibilité -->
                  <div style="margin-top: 0.75rem; padding-top: 0.5rem; border-top: 1px solid #e2e8f0;">
                    @if (isUserEligibleForSelectedElection()) {
                      <span style="color: #16a34a; font-weight: 600; font-size: 0.85rem;">
                        ✓ Vous appartenez au collège autorisé (Votre catégorie : {{ authService.currentUser()?.categorie }}).
                      </span>
                    } @else {
                      <span style="color: #dc2626; font-weight: 600; font-size: 0.85rem;">
                        ⚠️ Attention : Votre catégorie ({{ authService.currentUser()?.categorie }}) ne correspond pas au collège requis ({{ selectedOpenElection.college?.categorie }}).
                      </span>
                    }
                  </div>
                </div>
              }

              <!-- Profession de foi (obligatoire) -->
              <div class="form-group">
                <label class="form-label">Profession de foi (Programme électoral officiel) *</label>
                <textarea
                  class="form-control"
                  rows="4"
                  [(ngModel)]="newCandidacy.professionFoi"
                  name="professionFoi"
                  placeholder="Décrivez vos engagements, vos propositions pour l'UASZ et les axes de votre mandat..."
                  required
                  [disabled]="submitting || isAlreadyAppliedForSelected"
                ></textarea>
                <small class="text-muted">Ce programme sera rendu public pour informer les électeurs.</small>
              </div>

              <!-- Biographie / Présentation (optionnel) -->
              <div class="form-group">
                <label class="form-label">Biographie / Présentation personnelle</label>
                <textarea
                  class="form-control"
                  rows="3"
                  [(ngModel)]="newCandidacy.biographie"
                  name="biographie"
                  placeholder="Présentez votre parcours académique à l'UASZ, vos expériences, vos mandats passés..."
                  [disabled]="submitting || isAlreadyAppliedForSelected"
                ></textarea>
              </div>

              <!-- Pièces justificatives -->
              <div class="file-upload-section">
                <h4 style="font-size: 0.95rem; margin-bottom: 0.5rem;">Pièces Justificatives (Téléversement sécurisé)</h4>
                <p style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 1rem;">
                  Formats autorisés : PDF, PNG, JPG (maximum 10 Mo par document).
                </p>

                <div class="grid grid-cols-2" style="gap: 1rem;">
                  <div class="form-group" style="margin-bottom: 0;">
                    <label class="form-label" style="font-size: 0.85rem;">CV / Justificatif académique (PDF, PNG, JPG)</label>
                    <input type="file" class="form-control" (change)="onFileSelected($event, 'CV')" accept=".pdf,.png,.jpg,.jpeg" [disabled]="submitting || isAlreadyAppliedForSelected" />
                    @if (selectedCvFile) {
                      <small style="color: var(--primary); font-weight: 600; display: block; margin-top: 0.25rem;">
                        ✓ {{ selectedCvFile.name }} ({{ (selectedCvFile.size / 1024).toFixed(1) }} Ko)
                      </small>
                    }
                  </div>
                  <div class="form-group" style="margin-bottom: 0;">
                    <label class="form-label" style="font-size: 0.85rem;">Photo d'identité / Campagne (PNG, JPG)</label>
                    <input type="file" class="form-control" (change)="onFileSelected($event, 'PHOTO')" accept=".png,.jpg,.jpeg" [disabled]="submitting || isAlreadyAppliedForSelected" />
                    @if (selectedPhotoFile) {
                      <small style="color: var(--primary); font-weight: 600; display: block; margin-top: 0.25rem;">
                        ✓ {{ selectedPhotoFile.name }} ({{ (selectedPhotoFile.size / 1024).toFixed(1) }} Ko)
                      </small>
                    }
                  </div>
                </div>
              </div>

              @if (applyError) {
                <div class="alert alert-danger" style="margin-bottom: 1.25rem;">
                  {{ applyError }}
                </div>
              }

              <button
                type="submit"
                class="btn btn-primary btn-block btn-lg"
                [disabled]="submitting || !newCandidacy.electionId || !newCandidacy.professionFoi || isAlreadyAppliedForSelected"
              >
                @if (submitting) {
                  <span>⏳ Transmission en cours…</span>
                } @else {
                  <span>Transmission de ma candidature</span>
                }
              </button>
            </form>
          </div>
        }
      }

      <!-- ========================================== -->
      <!-- ONGLET 3 : MES CANDIDATURES               -->
      <!-- ========================================== -->
      @if (activeTab === 'MY_DOSSIER') {
        @if (!authService.isAuthenticated()) {
          <div class="card text-center" style="padding: 3rem; max-width: 650px; margin: 0 auto;">
            <div style="font-size: 3rem; margin-bottom: 1rem;">🔒</div>
            <h3>Connexion requise</h3>
            <p class="text-muted" style="margin-bottom: 1.5rem;">
              Connectez-vous pour consulter vos dossiers de candidature déposés à l'Université Assane Seck de Ziguinchor.
            </p>
            <a routerLink="/login" class="btn btn-primary">
              Se connecter à mon compte →
            </a>
          </div>
        } @else {
          <div class="card" style="margin-bottom: 2rem;">
            <div class="flex-between">
              <div>
                <h3>Mes Dossiers de Candidature</h3>
                <p class="subtitle">Suivez l'état d'instruction de vos dossiers par la Commission électorale de l'UASZ.</p>
              </div>
              <button class="btn btn-outline btn-sm" (click)="loadMyCandidacies()" [disabled]="loadingMyCandidacies">
                🔄 Actualiser
              </button>
            </div>

            @if (loadingMyCandidacies) {
              <div class="text-center" style="padding: 3rem;">
                <p>Chargement de vos dossiers de candidature...</p>
              </div>
            } @else if (myCandidaciesError) {
              <div class="alert alert-danger flex-between" style="margin-top: 1.5rem;">
                <span>{{ myCandidaciesError }}</span>
                <button class="btn btn-secondary btn-sm" (click)="loadMyCandidacies()">Réessayer</button>
              </div>
            } @else if (myCandidacies.length === 0) {
              <div class="text-center" style="padding: 3rem;">
                <div style="font-size: 3rem; margin-bottom: 1rem;">📁</div>
                <h4>Vous n'avez pas encore déposé de candidature</h4>
                <p style="color: var(--text-muted); margin-bottom: 1.5rem;">
                  Consultez les scrutins en phase ouverte pour déposer votre profession de foi.
                </p>
                <button class="btn btn-primary btn-sm" (click)="setTab('APPLY')">
                  + Déposer une première Candidature
                </button>
              </div>
            } @else {
              <div style="display: flex; flex-direction: column; gap: 1.5rem; margin-top: 1.5rem;">
                @for (c of myCandidacies; track c.id) {
                  <div class="card" style="background: #ffffff; border-left: 5px solid; box-shadow: 0 2px 4px rgba(0,0,0,0.05);" [style.border-left-color]="getStatusColor(c.statut)">
                    <div class="flex-between" style="margin-bottom: 1rem;">
                      <div>
                        <h4 style="margin: 0; font-size: 1.15rem;">{{ c.electionTitre || 'Scrutin #' + c.electionId }}</h4>
                        <small class="text-muted">
                          Dossier #{{ c.id }} — Déposé le {{ c.dateSoumission | date:'dd/MM/yyyy à HH:mm' }}
                          @if (c.collegeNom) {
                            — Collège : <strong>{{ c.collegeNom }}</strong>
                          }
                        </small>
                      </div>
                      <div>
                        <span class="badge" [ngClass]="getStatusBadge(c.statut)">
                          {{ getStatusLabel(c.statut) }}
                        </span>
                      </div>
                    </div>

                    <div class="quote-box" style="margin-bottom: 1rem;">
                      <strong>Profession de foi déclarée :</strong>
                      <p>« {{ c.professionFoi }} »</p>
                    </div>

                    @if (c.biographie) {
                      <p class="bio" style="margin-bottom: 1rem;"><strong>Présentation :</strong> {{ c.biographie }}</p>
                    }

                    @if (c.motifDecision) {
                      <div style="background: #fffbeb; border: 1px solid #fef3c7; border-radius: var(--radius-sm); padding: 0.75rem; margin-bottom: 1rem;">
                        <strong style="font-size: 0.85rem; color: #92400e;">Motif officiel notifié par la Commission :</strong>
                        <p style="margin: 0.25rem 0 0 0; font-size: 0.85rem; color: #78350f;">{{ c.motifDecision }}</p>
                      </div>
                    }

                    <!-- Pièces justificatives attachées -->
                    <div style="border-top: 1px solid var(--border-color); padding-top: 0.75rem;">
                      <div class="flex-between" style="align-items: center; margin-bottom: 0.5rem;">
                        <strong style="font-size: 0.85rem;">Documents joints ({{ c.documents?.length || 0 }}) :</strong>
                        @if (c.statut === 'SUBMITTED') {
                          <label class="btn btn-outline btn-xs" style="cursor: pointer; margin-bottom: 0;">
                            + Ajouter une pièce justificative
                            <input type="file" style="display: none;" (change)="onUploadAdditionalDoc($event, c.id)" accept=".pdf,.png,.jpg,.jpeg" />
                          </label>
                        }
                      </div>

                      @if (!c.documents || c.documents.length === 0) {
                        <small class="text-muted">Aucune pièce justificative jointe pour ce dossier.</small>
                      } @else {
                        <div style="display: flex; gap: 0.75rem; flex-wrap: wrap;">
                          @for (d of c.documents; track d.id) {
                            <div class="document-badge flex-between" style="gap: 0.5rem; background: #f1f5f9; padding: 0.35rem 0.65rem; border-radius: 4px; font-size: 0.8rem;">
                              <span>📄 <strong>{{ d.typeDocument }} :</strong> {{ d.nomFichierOriginal }}</span>
                              <a [href]="'/api/v1/candidacies/documents/file/' + d.cheminFichier" target="_blank" class="btn btn-outline btn-xs" style="padding: 0.1rem 0.35rem;">
                                📥 Voir
                              </a>
                            </div>
                          }
                        </div>
                      }
                    </div>
                  </div>
                }
              </div>
            }
          </div>
        }
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
    .candidates-count-bar {
      margin-bottom: 1rem;
      color: var(--text-muted);
      font-size: 0.9rem;
    }
    .candidate-card {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }
    .candidate-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    }
    .candidate-header {
      display: flex;
      gap: 1.25rem;
      align-items: center;
      margin-bottom: 1rem;
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
    .candidate-meta-bar {
      background: #f8fafc;
      padding: 0.5rem 0.75rem;
      border-radius: var(--radius-sm);
      font-size: 0.8rem;
      margin-bottom: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
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
      max-width: 750px;
      margin: 0 auto;
      padding: 2.5rem;
    }
    .scrutin-details-box {
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      border-radius: var(--radius-sm);
      padding: 1rem;
      margin-bottom: 1.5rem;
    }
    .file-upload-section {
      background: #f8fafc;
      border: 1px dashed var(--border-color);
      border-radius: var(--radius-sm);
      padding: 1.25rem;
      margin-bottom: 1.5rem;
    }
    .btn-block {
      width: 100%;
    }
    .flex-between {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .btn-xs {
      padding: 0.25rem 0.5rem;
      font-size: 0.75rem;
    }
  `]
})
export class CandidaciesComponent implements OnInit {
  authService = inject(AuthService);
  private apiService = inject(ApiService);
  private route = inject(ActivatedRoute);

  activeTab: 'LIST' | 'APPLY' | 'MY_DOSSIER' = 'LIST';
  elections: Election[] = [];
  candidaciesOpenElections: Election[] = [];
  selectedElectionId: number | null = null;
  selectedOpenElection: Election | null = null;
  candidates: Candidacy[] = [];
  loadingCandidates = false;
  candidatesError = '';

  myCandidacies: Candidacy[] = [];
  loadingMyCandidacies = false;
  myCandidaciesError = '';

  newCandidacy = {
    electionId: null as number | null,
    professionFoi: '',
    biographie: ''
  };

  selectedCvFile: File | null = null;
  selectedPhotoFile: File | null = null;

  submitting = false;
  applySuccess = '';
  applyError = '';

  isAlreadyAppliedForSelected = false;
  alreadyAppliedCandidacy: Candidacy | null = null;

  ngOnInit(): void {
    // 1. Lire les queryParams pour navigation directe (ex: ?tab=APPLY&electionId=8)
    this.route.queryParamMap.subscribe((params) => {
      const tabParam = params.get('tab');
      const electionIdParam = params.get('electionId');

      if (tabParam === 'APPLY' || tabParam === 'MY_DOSSIER' || tabParam === 'LIST') {
        this.activeTab = tabParam;
      }
      if (electionIdParam) {
        const parsedId = Number(electionIdParam);
        this.selectedElectionId = parsedId;
        this.newCandidacy.electionId = parsedId;
      }
    });

    // 2. Charger les données d'élections et candidatures
    this.loadElectionsData();
    if (this.authService.isAuthenticated()) {
      this.loadMyCandidacies();
    }
  }

  setTab(tab: 'LIST' | 'APPLY' | 'MY_DOSSIER'): void {
    this.activeTab = tab;
    this.applySuccess = '';
    this.applyError = '';

    if (tab === 'LIST') {
      this.loadCandidates();
    } else if (tab === 'APPLY') {
      if (this.elections.length === 0) {
        this.loadElectionsData();
      }
      if (this.authService.isAuthenticated()) {
        this.loadMyCandidacies();
      }
    } else if (tab === 'MY_DOSSIER') {
      this.loadMyCandidacies();
    }
  }

  loadElectionsData(): void {
    this.apiService.getPublicElections().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.elections = res.data;
          this.candidaciesOpenElections = res.data.filter(e => e.statut === 'CANDIDATURES_OPEN');

          // Scrutin ouvert par défaut pour le formulaire de candidature
          if (this.candidaciesOpenElections.length > 0 && !this.newCandidacy.electionId) {
            this.newCandidacy.electionId = this.candidaciesOpenElections[0].id;
          }
          this.updateSelectedOpenElection();
          this.checkIfAlreadyApplied(this.newCandidacy.electionId);

          // Chargement initial des candidats
          this.loadCandidates();
        }
      },
      error: () => {
        this.candidatesError = 'Impossible de charger la liste des scrutins électoraux.';
      }
    });
  }

  loadCandidates(): void {
    this.loadingCandidates = true;
    this.candidatesError = '';

    const request$ = this.selectedElectionId
      ? this.apiService.getPublicCandidates(this.selectedElectionId)
      : this.apiService.getAllPublicCandidates();

    request$.pipe(
      finalize(() => {
        this.loadingCandidates = false;
      })
    ).subscribe({
      next: (res) => {
        this.candidates = res.data || [];
      },
      error: (err) => {
        this.candidatesError = err.error?.message || 'Erreur lors du chargement des candidatures officielles.';
      }
    });
  }

  loadMyCandidacies(): void {
    if (!this.authService.isAuthenticated()) return;
    this.loadingMyCandidacies = true;
    this.myCandidaciesError = '';

    this.apiService.getMyCandidacies().pipe(
      finalize(() => {
        this.loadingMyCandidacies = false;
      })
    ).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.myCandidacies = res.data;
          if (this.newCandidacy.electionId) {
            this.checkIfAlreadyApplied(this.newCandidacy.electionId);
          }
        }
      },
      error: (err) => {
        this.myCandidaciesError = err.error?.message || 'Impossible de récupérer vos dossiers de candidature.';
      }
    });
  }

  onElectionChange(): void {
    this.loadCandidates();
  }

  onElectionSelectChange(electionId: number | null): void {
    this.updateSelectedOpenElection();
    this.checkIfAlreadyApplied(electionId);
  }

  private updateSelectedOpenElection(): void {
    if (!this.newCandidacy.electionId) {
      this.selectedOpenElection = null;
      return;
    }
    this.selectedOpenElection = this.candidaciesOpenElections.find(e => e.id === Number(this.newCandidacy.electionId)) || null;
  }

  isUserEligibleForSelectedElection(): boolean {
    if (!this.selectedOpenElection) return true;
    const currentUser = this.authService.currentUser();
    if (!currentUser) return false;
    // Les super admins peuvent postuler pour tests
    if (this.authService.isSuperAdmin() || this.authService.isCommission()) return true;
    // Vérification de catégorie avec le collège
    if (this.selectedOpenElection.college && this.selectedOpenElection.college.categorie) {
      return this.selectedOpenElection.college.categorie === currentUser.categorie;
    }
    return true;
  }

  private checkIfAlreadyApplied(electionId: number | null): void {
    if (!electionId) {
      this.isAlreadyAppliedForSelected = false;
      this.alreadyAppliedCandidacy = null;
      return;
    }
    const found = this.myCandidacies.find(c => Number(c.electionId) === Number(electionId));
    if (found) {
      this.isAlreadyAppliedForSelected = true;
      this.alreadyAppliedCandidacy = found;
    } else {
      this.isAlreadyAppliedForSelected = false;
      this.alreadyAppliedCandidacy = null;
    }
  }

  onFileSelected(event: Event, type: 'CV' | 'PHOTO'): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const maxBytes = 10 * 1024 * 1024; // 10 Mo
      if (file.size > maxBytes) {
        this.applyError = `Le fichier « ${file.name} » dépasse la taille maximale autorisée (10 Mo).`;
        input.value = '';
        if (type === 'CV') this.selectedCvFile = null;
        if (type === 'PHOTO') this.selectedPhotoFile = null;
        return;
      }
      const ext = '.' + (file.name.split('.').pop() || '').toLowerCase();
      const allowedCv = ['.pdf', '.png', '.jpg', '.jpeg'];
      const allowedPhoto = ['.png', '.jpg', '.jpeg'];
      const allowed = type === 'CV' ? allowedCv : allowedPhoto;
      if (!allowed.includes(ext)) {
        this.applyError = `Format non autorisé pour « ${file.name} ». Formats acceptés : ${allowed.join(', ')}`;
        input.value = '';
        if (type === 'CV') this.selectedCvFile = null;
        if (type === 'PHOTO') this.selectedPhotoFile = null;
        return;
      }
      this.applyError = '';
      if (type === 'CV') {
        this.selectedCvFile = file;
      } else if (type === 'PHOTO') {
        this.selectedPhotoFile = file;
      }
    }
  }

  submitCandidacy(): void {
    // 1. Anti-double soumission immédiat
    if (this.submitting) return;

    // 2. Vérification des données requises
    if (!this.newCandidacy.electionId) {
      this.applyError = 'Veuillez sélectionner un scrutin électoral.';
      return;
    }
    if (!this.newCandidacy.professionFoi || !this.newCandidacy.professionFoi.trim()) {
      this.applyError = 'Veuillez renseigner votre profession de foi (programme électoral).';
      return;
    }
    if (this.isAlreadyAppliedForSelected) {
      this.applyError = 'Une candidature a déjà été transmise pour ce scrutin. Les doublons ne sont pas autorisés.';
      return;
    }

    this.submitting = true;
    this.applySuccess = '';
    this.applyError = '';

    const formData = new FormData();
    formData.append('electionId', String(this.newCandidacy.electionId));
    formData.append('professionFoi', this.newCandidacy.professionFoi.trim());
    if (this.newCandidacy.biographie && this.newCandidacy.biographie.trim()) {
      formData.append('biographie', this.newCandidacy.biographie.trim());
    }
    if (this.selectedCvFile) {
      formData.append('cvFile', this.selectedCvFile, this.selectedCvFile.name);
    }
    if (this.selectedPhotoFile) {
      formData.append('photoFile', this.selectedPhotoFile, this.selectedPhotoFile.name);
    }

    // 3. Appel HTTP unique multipart avec timeout(30000) et finalize()
    this.apiService.applyCandidacyMultipart(formData).pipe(
      timeout(30000),
      finalize(() => {
        this.submitting = false;
      })
    ).subscribe({
      next: (res) => {
        const candidacyId = res.data?.id;
        this.applySuccess = `Votre candidature a été transmise avec succès ! (Dossier #${candidacyId} enregistré dans PostgreSQL).`;
        this.newCandidacy.professionFoi = '';
        this.newCandidacy.biographie = '';
        this.selectedCvFile = null;
        this.selectedPhotoFile = null;

        // Rechargement des listes
        this.loadCandidates();
        this.loadMyCandidacies();
        setTimeout(() => {
          this.setTab('MY_DOSSIER');
        }, 1500);
      },
      error: (err) => {
        if (err.name === 'TimeoutError') {
          this.applyError = 'Le délai d’attente a expiré. Veuillez vérifier dans l’onglet « Mes Candidatures » si votre dossier a été enregistré avant de réessayer.';
        } else {
          this.applyError = err.error?.message || err.message || 'Erreur lors de la transmission du dossier de candidature.';
        }
        this.loadMyCandidacies();
      }
    });
  }

  onUploadAdditionalDoc(event: Event, candidacyId: number): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const maxBytes = 10 * 1024 * 1024;
      if (file.size > maxBytes) {
        alert('Le fichier dépasse la taille maximale de 10 Mo.');
        return;
      }
      const type = file.type.startsWith('image/') ? 'PHOTO' : 'CV';
      this.apiService.uploadCandidacyDocument(candidacyId, type, file).subscribe({
        next: () => {
          this.loadMyCandidacies();
        },
        error: (err) => {
          alert(err.error?.message || 'Erreur lors du téléversement du document.');
        }
      });
    }
  }

  getStatusBadge(statut?: string): string {
    switch (statut) {
      case 'VALIDATED': return 'badge-success';
      case 'REJECTED': return 'badge-danger';
      case 'SUBMITTED': return 'badge-warning';
      default: return 'badge-secondary';
    }
  }

  getStatusLabel(statut?: string): string {
    switch (statut) {
      case 'SUBMITTED': return 'Dossier soumis (En cours d\'instruction)';
      case 'VALIDATED': return 'Candidature Validée';
      case 'REJECTED': return 'Dossier Rejeté';
      default: return statut || 'En attente';
    }
  }

  getStatusColor(statut?: string): string {
    switch (statut) {
      case 'VALIDATED': return '#16a34a';
      case 'REJECTED': return '#dc2626';
      case 'SUBMITTED': return '#f59e0b';
      default: return '#94a3b8';
    }
  }
}
