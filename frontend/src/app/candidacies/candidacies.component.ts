import { Component, OnInit, inject, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { timeout, finalize, catchError, of } from 'rxjs';
import { ApiService } from '../core/services/api.service';
import { AuthService } from '../core/services/auth.service';
import { Candidacy, Election } from '../core/models/models';
import { environment } from '../../environments/environment';

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
        <button class="tab-btn" [class.active]="activeTab() === 'LIST'" (click)="setTab('LIST')">
          📋 Liste des Candidats
        </button>
        <button class="tab-btn" [class.active]="activeTab() === 'APPLY'" (click)="setTab('APPLY')">
          ✍️ Postuler à un Scrutin
        </button>
        <button class="tab-btn" [class.active]="activeTab() === 'MY_DOSSIER'" (click)="setTab('MY_DOSSIER')">
          📁 Mes Candidatures @if (authService.isAuthenticated() && myCandidacies().length > 0) { ({{ myCandidacies().length }}) }
        </button>
      </div>

      <!-- ========================================== -->
      <!-- ONGLET 1 : LISTE DES CANDIDATS (PUBLIQUE)   -->
      <!-- ========================================== -->
      @if (activeTab() === 'LIST') {
        <div class="election-select-box card">
          <div class="flex-between" style="flex-wrap: wrap; gap: 1rem;">
            <div style="flex: 1; min-width: 250px;">
              <label class="form-label">Filtrer par scrutin :</label>
              <select class="form-control" [ngModel]="selectedElectionId()" (ngModelChange)="onElectionFilterChange($event)">
                <option [ngValue]="null">-- Tous les scrutins confondus --</option>
                @for (e of elections(); track e.id) {
                  <option [ngValue]="e.id">{{ e.titre }} ({{ e.college ? e.college.nom : e.type }})</option>
                }
              </select>
            </div>
            <div>
              <button class="btn btn-outline btn-sm" (click)="loadCandidates()" [disabled]="loadingCandidates()" style="margin-top: 1.5rem;">
                <span [class.spin]="loadingCandidates()">🔄</span> Actualiser la liste
              </button>
            </div>
          </div>
        </div>

        @if (loadingCandidates()) {
          <div class="card text-center loading-card" style="padding: 3.5rem;">
            <div class="spinner"></div>
            <p style="margin-top: 1.25rem; color: var(--text-muted); font-size: 1.05rem;">
              Chargement des candidatures officielles depuis le serveur...
            </p>
          </div>
        } @else if (candidatesError()) {
          <div class="card error-card" style="padding: 2rem; border-left: 5px solid #dc2626;">
            <div class="flex-between" style="flex-wrap: wrap; gap: 1rem;">
              <div>
                <p class="text-danger" style="margin: 0; font-weight: 600;">{{ candidatesError() }}</p>
                <small class="text-muted">Vérifiez votre connexion au serveur électoral UASZ.</small>
              </div>
              <button class="btn btn-primary btn-sm" (click)="loadCandidates()">🔄 Réessayer</button>
            </div>
          </div>
        } @else if (candidates().length === 0) {
          <div class="card text-center" style="padding: 3.5rem;">
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
            <span><strong>{{ candidates().length }}</strong> candidature(s) enregistrée(s)</span>
          </div>

          <div class="grid grid-cols-2" style="gap: 1.5rem;">
            @for (c of candidates(); track c.id) {
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
      @if (activeTab() === 'APPLY') {
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

            @if (applySuccess()) {
              <div class="alert alert-success" style="margin-bottom: 1.5rem;">
                {{ applySuccess() }}
              </div>
            }
            @if (applyError()) {
              <div class="alert alert-danger" style="margin-bottom: 1.5rem;">
                {{ applyError() }}
              </div>
            }

            @if (isAlreadyAppliedForSelected()) {
              <div class="alert alert-warning" style="margin-bottom: 1.5rem;">
                <div style="font-weight: 700; margin-bottom: 0.35rem;">
                  ⚠️ Candidature déjà enregistrée pour ce scrutin
                </div>
                <p style="margin: 0 0 0.75rem 0; font-size: 0.9rem;">
                  Vous avez déjà transmis un dossier pour ce scrutin le {{ alreadyAppliedCandidacy()?.dateSoumission | date:'dd/MM/yyyy à HH:mm' }}.
                  Statut : <strong>{{ getStatusLabel(alreadyAppliedCandidacy()?.statut) }}</strong>.
                </p>
                <button type="button" class="btn btn-outline btn-sm" (click)="setTab('MY_DOSSIER')">
                  Consulter mon dossier complet dans « Mes Candidatures » →
                </button>
              </div>
            }

            <form (ngSubmit)="submitCandidacy()">
              <!-- Indicateur d'erreur de chargement des scrutins -->
              @if (openElectionsError()) {
                <div class="alert alert-danger" style="margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
                  <span>⚠️ {{ openElectionsError() }}</span>
                  <button type="button" class="btn btn-outline btn-xs" (click)="loadOpenElectionsData(true)">🔄 Réessayer</button>
                </div>
              }

              <!-- Indicateur de chargement des scrutins ouverts -->
              @if (loadingOpenElections()) {
                <div style="padding: 1.5rem; text-align: center; background: #f8fafc; border-radius: var(--radius-sm); border: 1px dashed #cbd5e1; margin-bottom: 1.25rem;">
                  <div class="spinner" style="width: 28px; height: 28px; margin: 0 auto 0.5rem auto;"></div>
                  <span style="font-size: 0.9rem; color: var(--text-muted);">Recherche des scrutins en phase ouverte de candidatures...</span>
                </div>
              }

              <!-- Sélection du scrutin ouvert -->
              <div class="form-group">
                <div class="flex-between" style="margin-bottom: 0.35rem;">
                  <label class="form-label" style="margin-bottom: 0;">Sélectionner un scrutin électoral ouvert *</label>
                  <button type="button" class="btn btn-outline btn-xs" (click)="loadOpenElectionsData(true)" [disabled]="loadingOpenElections()" title="Actualiser les scrutins disponibles">
                    <span [class.spin]="loadingOpenElections()">🔄</span> Actualiser
                  </button>
                </div>
                <select
                  class="form-control"
                  [(ngModel)]="newCandidacy.electionId"
                  name="electionId"
                  (ngModelChange)="onElectionSelectChange($event)"
                  required
                  [disabled]="loadingOpenElections()"
                >
                  <option [ngValue]="null" disabled selected>-- Sélectionnez un scrutin électoral ouvert --</option>
                  @for (e of candidaciesOpenElections(); track e.id) {
                    <option [ngValue]="e.id">
                      {{ e.titre }} — Collège : {{ e.college ? e.college.nom : 'Général' }} ({{ e.college ? e.college.categorie : 'Toutes catégories' }})
                    </option>
                  }
                </select>

                @if (!loadingOpenElections() && candidaciesOpenElections().length === 0) {
                  <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: var(--radius-sm); padding: 1.25rem; margin-top: 0.75rem; text-align: center;">
                    <p style="color: #64748b; font-size: 0.95rem; margin: 0 0 0.5rem 0;">
                      ⚠️ Aucun scrutin n'est actuellement en phase ouverte de dépôt de candidatures pour votre profil (Catégorie : <strong>{{ authService.currentUser()?.categorie || 'Non définie' }}</strong>).
                    </p>
                    <small style="color: #94a3b8; display: block; margin-bottom: 0.75rem;">
                      Si un administrateur vient de créer un scrutin, vérifiez que le statut de l'élection a bien été basculé sur « CANDIDATURES_OPEN ».
                    </small>
                    <button type="button" class="btn btn-outline btn-sm" (click)="loadOpenElectionsData(true)">
                      🔄 Actualiser la liste des scrutins
                    </button>
                  </div>
                }
              </div>

              <!-- Fiche d'information détaillée du scrutin sélectionné -->
              @if (selectedOpenElection(); as election) {
                <div class="scrutin-details-box">
                  <div class="flex-between" style="margin-bottom: 0.5rem;">
                    <strong style="color: var(--primary);">📋 Détails du Scrutin Sélectionné</strong>
                    <span class="badge badge-success">Candidatures Ouvertes</span>
                  </div>
                  <div class="grid grid-cols-2" style="font-size: 0.85rem; gap: 0.5rem; margin-bottom: 0.5rem;">
                    <div><strong>Collège électoral :</strong> {{ election.college ? election.college.nom : 'Général' }}</div>
                    <div><strong>Catégorie concernée :</strong> {{ election.college ? election.college.categorie : 'Toutes' }}</div>
                    <div><strong>Ouverture :</strong> {{ election.dateOuverture | date:'dd/MM/yyyy HH:mm' }}</div>
                    <div><strong>Clôture :</strong> {{ election.dateCloture | date:'dd/MM/yyyy HH:mm' }}</div>
                  </div>
                  @if (election.description) {
                    <p style="font-size: 0.85rem; color: #475569; margin: 0.25rem 0 0 0;">
                      <em>{{ election.description }}</em>
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
                        ⚠️ Attention : Votre catégorie ({{ authService.currentUser()?.categorie }}) ne correspond pas au collège requis ({{ election.college ? election.college.categorie : 'Général' }}).
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
                  [disabled]="submitting() || isAlreadyAppliedForSelected()"
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
                  [disabled]="submitting() || isAlreadyAppliedForSelected()"
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
                    <input type="file" class="form-control" (change)="onFileSelected($event, 'CV')" accept=".pdf,.png,.jpg,.jpeg" [disabled]="submitting() || isAlreadyAppliedForSelected()" />
                    @if (selectedCvFile) {
                      <small style="color: var(--primary); font-weight: 600; display: block; margin-top: 0.25rem;">
                        ✓ {{ selectedCvFile.name }} ({{ (selectedCvFile.size / 1024).toFixed(1) }} Ko)
                      </small>
                    }
                  </div>
                  <div class="form-group" style="margin-bottom: 0;">
                    <label class="form-label" style="font-size: 0.85rem;">Photo d'identité / Campagne (PNG, JPG)</label>
                    <input type="file" class="form-control" (change)="onFileSelected($event, 'PHOTO')" accept=".png,.jpg,.jpeg" [disabled]="submitting() || isAlreadyAppliedForSelected()" />
                    @if (selectedPhotoFile) {
                      <small style="color: var(--primary); font-weight: 600; display: block; margin-top: 0.25rem;">
                        ✓ {{ selectedPhotoFile.name }} ({{ (selectedPhotoFile.size / 1024).toFixed(1) }} Ko)
                      </small>
                    }
                  </div>
                </div>
              </div>

              <button
                type="submit"
                class="btn btn-primary btn-block btn-lg"
                [disabled]="submitting() || !newCandidacy.electionId || !newCandidacy.professionFoi || isAlreadyAppliedForSelected()"
              >
                @if (submitting()) {
                  <span><span class="spin">⏳</span> Transmission en cours…</span>
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
      @if (activeTab() === 'MY_DOSSIER') {
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
              <button class="btn btn-outline btn-sm" (click)="loadMyCandidacies()" [disabled]="loadingMyCandidacies()">
                <span [class.spin]="loadingMyCandidacies()">🔄</span> Actualiser
              </button>
            </div>

            @if (loadingMyCandidacies()) {
              <div class="text-center loading-card" style="padding: 3.5rem;">
                <div class="spinner"></div>
                <p style="margin-top: 1.25rem; color: var(--text-muted); font-size: 1.05rem;">
                  Chargement de vos dossiers de candidature...
                </p>
              </div>
            } @else if (myCandidaciesError()) {
              <div class="card error-card flex-between" style="padding: 2rem; border-left: 5px solid #dc2626; margin-top: 1.5rem;">
                <div>
                  <p class="text-danger" style="margin: 0; font-weight: 600;">{{ myCandidaciesError() }}</p>
                  <small class="text-muted">Vérifiez votre connexion au serveur électoral.</small>
                </div>
                <button class="btn btn-primary btn-sm" (click)="loadMyCandidacies()">🔄 Réessayer</button>
              </div>
            } @else if (myCandidacies().length === 0) {
              <div class="text-center" style="padding: 3.5rem;">
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
                @for (c of myCandidacies(); track c.id) {
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
                              <a [href]="getDocumentUrl(d.cheminFichier)" target="_blank" class="btn btn-outline btn-xs" style="padding: 0.1rem 0.35rem;">
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
    .spinner {
      width: 36px;
      height: 36px;
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
    .text-danger {
      color: #dc2626;
    }
  `]
})
export class CandidaciesComponent implements OnInit {
  authService = inject(AuthService);
  private apiService = inject(ApiService);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  // Signaux réactifs pour le mode Zoneless Angular 21
  activeTab = signal<'LIST' | 'APPLY' | 'MY_DOSSIER'>('LIST');

  // Onglet 1 : LISTE
  elections = signal<Election[]>([]);
  selectedElectionId = signal<number | null>(null);
  candidates = signal<Candidacy[]>([]);
  loadingCandidates = signal<boolean>(true);
  candidatesError = signal<string>('');

  // Onglet 2 : POSTULER
  candidaciesOpenElections = signal<Election[]>([]);
  loadingOpenElections = signal<boolean>(false);
  openElectionsError = signal<string>('');
  selectedOpenElection = signal<Election | null>(null);
  isAlreadyAppliedForSelected = signal<boolean>(false);
  alreadyAppliedCandidacy = signal<Candidacy | null>(null);

  // Onglet 3 : MES CANDIDATURES
  myCandidacies = signal<Candidacy[]>([]);
  loadingMyCandidacies = signal<boolean>(false);
  myCandidaciesError = signal<string>('');

  newCandidacy = {
    electionId: null as number | null,
    professionFoi: '',
    biographie: ''
  };

  selectedCvFile: File | null = null;
  selectedPhotoFile: File | null = null;

  submitting = signal<boolean>(false);
  applySuccess = signal<string>('');
  applyError = signal<string>('');

  ngOnInit(): void {
    // 1. Lire les queryParams pour navigation directe (ex: ?tab=APPLY&electionId=8)
    this.route.queryParamMap.subscribe((params) => {
      const tabParam = params.get('tab');
      const electionIdParam = params.get('electionId');

      if (tabParam === 'APPLY' || tabParam === 'MY_DOSSIER' || tabParam === 'LIST') {
        this.activeTab.set(tabParam);
      }
      if (electionIdParam) {
        const parsedId = Number(electionIdParam);
        this.selectedElectionId.set(parsedId);
        this.newCandidacy.electionId = parsedId;
      }
      this.cdr.markForCheck();
    });

    // 2. Charger les données d'élections et candidatures
    this.loadElectionsData();
    this.loadOpenElectionsData(false);
    if (this.authService.isAuthenticated()) {
      this.loadMyCandidacies();
    }
  }

  setTab(tab: 'LIST' | 'APPLY' | 'MY_DOSSIER'): void {
    this.activeTab.set(tab);
    this.applySuccess.set('');
    this.applyError.set('');

    if (tab === 'LIST') {
      this.loadCandidates();
    } else if (tab === 'APPLY') {
      this.loadOpenElectionsData(true);
      if (this.authService.isAuthenticated()) {
        this.loadMyCandidacies();
      }
    } else if (tab === 'MY_DOSSIER') {
      this.loadMyCandidacies();
    }
    this.cdr.markForCheck();
  }

  loadOpenElectionsData(forceReload: boolean = true): void {
    this.loadingOpenElections.set(true);
    this.openElectionsError.set('');
    if (forceReload) {
      this.apiService.invalidateElectionsCache();
    }

    // Si authentifié, utilise getOpenElectionsForCandidacy avec repli automatique sur getPublicElections
    const openStream$ = this.authService.isAuthenticated()
      ? this.apiService.getOpenElectionsForCandidacy().pipe(
          timeout(12000),
          catchError(() => this.apiService.getPublicElections(true).pipe(timeout(12000)))
        )
      : this.apiService.getPublicElections(true).pipe(timeout(12000));

    openStream$.pipe(
      finalize(() => {
        this.loadingOpenElections.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const userCat = this.authService.currentUser()?.categorie;
          const isPrivileged = this.authService.isSuperAdmin() || this.authService.isCommission();

          // Retenir uniquement les élections en phase CANDIDATURES_OPEN
          let list = res.data.filter(e => e.statut === 'CANDIDATURES_OPEN');

          // Si l'utilisateur a une catégorie définie et n'est pas admin, filtrer les scrutins compatibles
          if (!isPrivileged && userCat) {
            list = list.filter(e => !e.college || !e.college.categorie || e.college.categorie === userCat);
          }

          this.candidaciesOpenElections.set(list);

          // Conserver la sélection si elle est présente dans la liste, sinon prendre la première
          if (this.newCandidacy.electionId && !list.some(e => Number(e.id) === Number(this.newCandidacy.electionId))) {
            this.newCandidacy.electionId = null;
          }

          if (list.length > 0 && !this.newCandidacy.electionId) {
            this.newCandidacy.electionId = list[0].id;
          }

          this.updateSelectedOpenElection();
          this.checkIfAlreadyApplied(this.newCandidacy.electionId);
        } else {
          this.candidaciesOpenElections.set([]);
          this.openElectionsError.set(res?.message || 'Aucun scrutin ouvert trouvé.');
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.candidaciesOpenElections.set([]);
        this.openElectionsError.set(err?.error?.message || 'Impossible de charger la liste des scrutins ouverts aux candidatures.');
        this.cdr.markForCheck();
      }
    });
  }

  loadElectionsData(): void {
    this.apiService.getPublicElections().pipe(
      timeout(12000),
      catchError((err) => {
        return of({ success: false, message: 'Erreur réseau', data: [] });
      })
    ).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.elections.set(res.data);
          this.loadCandidates();
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.candidatesError.set('Impossible de charger la liste des scrutins électoraux.');
        this.cdr.markForCheck();
      }
    });
  }

  loadCandidates(): void {
    this.loadingCandidates.set(true);
    this.candidatesError.set('');

    const currentElectionId = this.selectedElectionId();
    const request$ = currentElectionId
      ? this.apiService.getPublicCandidates(currentElectionId)
      : this.apiService.getAllPublicCandidates();

    request$.pipe(
      timeout(12000),
      catchError((err) => {
        return of({
          success: false,
          message: err?.name === 'TimeoutError'
            ? 'Le délai d’attente a expiré lors de la récupération des candidatures.'
            : (err?.error?.message || 'Erreur lors du chargement des candidatures officielles.'),
          data: []
        });
      }),
      finalize(() => {
        this.loadingCandidates.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.candidates.set(res.data);
        } else if (!res.success) {
          this.candidates.set([]);
          this.candidatesError.set(res.message || 'Erreur lors du chargement des candidatures.');
        } else {
          this.candidates.set([]);
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.candidates.set([]);
        this.candidatesError.set(err?.error?.message || 'Erreur lors du chargement des candidatures officielles.');
        this.cdr.markForCheck();
      }
    });
  }

  loadMyCandidacies(): void {
    if (!this.authService.isAuthenticated()) return;
    this.loadingMyCandidacies.set(true);
    this.myCandidaciesError.set('');

    this.apiService.getMyCandidacies().pipe(
      timeout(12000),
      catchError((err) => {
        return of({
          success: false,
          message: err?.name === 'TimeoutError'
            ? 'Le délai d’attente a expiré pour récupérer vos candidatures.'
            : (err?.error?.message || 'Impossible de récupérer vos dossiers de candidature.'),
          data: []
        });
      }),
      finalize(() => {
        this.loadingMyCandidacies.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.myCandidacies.set(res.data);
          if (this.newCandidacy.electionId) {
            this.checkIfAlreadyApplied(this.newCandidacy.electionId);
          }
        } else if (!res.success) {
          this.myCandidacies.set([]);
          this.myCandidaciesError.set(res.message || 'Impossible de récupérer vos dossiers de candidature.');
        } else {
          this.myCandidacies.set([]);
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.myCandidacies.set([]);
        this.myCandidaciesError.set(err?.error?.message || 'Impossible de récupérer vos dossiers de candidature.');
        this.cdr.markForCheck();
      }
    });
  }

  onElectionFilterChange(newId: any): void {
    const parsedId = newId ? Number(newId) : null;
    this.selectedElectionId.set(parsedId);
    this.loadCandidates();
  }

  onElectionSelectChange(electionId: number | null): void {
    this.newCandidacy.electionId = electionId ? Number(electionId) : null;
    this.updateSelectedOpenElection();
    this.checkIfAlreadyApplied(this.newCandidacy.electionId);
    this.cdr.markForCheck();
  }

  private updateSelectedOpenElection(): void {
    if (!this.newCandidacy.electionId) {
      this.selectedOpenElection.set(null);
      return;
    }
    const found = this.candidaciesOpenElections().find(e => Number(e.id) === Number(this.newCandidacy.electionId)) || null;
    this.selectedOpenElection.set(found);
    this.cdr.markForCheck();
  }

  isUserEligibleForSelectedElection(): boolean {
    const election = this.selectedOpenElection();
    if (!election) return true;
    const currentUser = this.authService.currentUser();
    if (!currentUser) return false;
    // Les super admins et commission électorale peuvent postuler pour tests
    if (this.authService.isSuperAdmin() || this.authService.isCommission()) return true;
    // Vérification de catégorie avec le collège
    if (election.college && election.college.categorie) {
      return election.college.categorie === currentUser.categorie;
    }
    return true;
  }

  private checkIfAlreadyApplied(electionId: number | null): void {
    if (!electionId) {
      this.isAlreadyAppliedForSelected.set(false);
      this.alreadyAppliedCandidacy.set(null);
      return;
    }
    const found = this.myCandidacies().find(c => Number(c.electionId) === Number(electionId));
    if (found) {
      this.isAlreadyAppliedForSelected.set(true);
      this.alreadyAppliedCandidacy.set(found);
    } else {
      this.isAlreadyAppliedForSelected.set(false);
      this.alreadyAppliedCandidacy.set(null);
    }
    this.cdr.markForCheck();
  }

  onFileSelected(event: Event, type: 'CV' | 'PHOTO'): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const maxBytes = 10 * 1024 * 1024; // 10 Mo
      if (file.size > maxBytes) {
        this.applyError.set(`Le fichier « ${file.name} » dépasse la taille maximale autorisée (10 Mo).`);
        input.value = '';
        if (type === 'CV') this.selectedCvFile = null;
        if (type === 'PHOTO') this.selectedPhotoFile = null;
        this.cdr.markForCheck();
        return;
      }
      const ext = '.' + (file.name.split('.').pop() || '').toLowerCase();
      const allowedCv = ['.pdf', '.png', '.jpg', '.jpeg'];
      const allowedPhoto = ['.png', '.jpg', '.jpeg'];
      const allowed = type === 'CV' ? allowedCv : allowedPhoto;
      if (!allowed.includes(ext)) {
        this.applyError.set(`Format non autorisé pour « ${file.name} ». Formats acceptés : ${allowed.join(', ')}`);
        input.value = '';
        if (type === 'CV') this.selectedCvFile = null;
        if (type === 'PHOTO') this.selectedPhotoFile = null;
        this.cdr.markForCheck();
        return;
      }
      this.applyError.set('');
      if (type === 'CV') {
        this.selectedCvFile = file;
      } else if (type === 'PHOTO') {
        this.selectedPhotoFile = file;
      }
      this.cdr.markForCheck();
    }
  }

  submitCandidacy(): void {
    // 1. Anti-double soumission immédiat
    if (this.submitting()) return;

    // 2. Vérification des données requises
    if (!this.newCandidacy.electionId) {
      this.applyError.set('Veuillez sélectionner un scrutin électoral.');
      return;
    }
    if (!this.newCandidacy.professionFoi || !this.newCandidacy.professionFoi.trim()) {
      this.applyError.set('Veuillez renseigner votre profession de foi (programme électoral).');
      return;
    }
    if (this.isAlreadyAppliedForSelected()) {
      this.applyError.set('Une candidature a déjà été transmise pour ce scrutin. Les doublons ne sont pas autorisés.');
      return;
    }

    this.submitting.set(true);
    this.applySuccess.set('');
    this.applyError.set('');

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
        this.submitting.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (res) => {
        const candidacyId = res.data?.id;
        this.applySuccess.set(`Votre candidature a été transmise avec succès ! (Dossier #${candidacyId} enregistré dans PostgreSQL).`);
        this.newCandidacy.professionFoi = '';
        this.newCandidacy.biographie = '';
        this.selectedCvFile = null;
        this.selectedPhotoFile = null;

        // Rechargement des listes
        this.loadCandidates();
        this.loadMyCandidacies();
        this.cdr.markForCheck();

        setTimeout(() => {
          this.setTab('MY_DOSSIER');
        }, 1500);
      },
      error: (err) => {
        if (err.name === 'TimeoutError') {
          this.applyError.set('Le délai d’attente a expiré. Veuillez vérifier dans l’onglet « Mes Candidatures » si votre dossier a été enregistré avant de réessayer.');
        } else {
          this.applyError.set(err.error?.message || err.message || 'Erreur lors de la transmission du dossier de candidature.');
        }
        this.loadMyCandidacies();
        this.cdr.markForCheck();
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
      this.apiService.uploadCandidacyDocument(candidacyId, type, file).pipe(
        timeout(20000)
      ).subscribe({
        next: () => {
          this.loadMyCandidacies();
          this.cdr.markForCheck();
        },
        error: (err) => {
          alert(err.error?.message || 'Erreur lors du téléversement du document.');
          this.cdr.markForCheck();
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

  getDocumentUrl(path: string): string {
    return `${environment.apiUrl}/candidacies/documents/file/${path}`;
  }
}
