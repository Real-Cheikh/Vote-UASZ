import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="login-wrapper">
      <div class="login-card card">
        <div class="login-header">
          <div class="login-badge">UASZ SÉNÉGAL</div>
          <h2>VoteUASZ</h2>
          <p class="login-subtitle">Plateforme Électorale Universitaire Sécurisée</p>
        </div>

        <!-- Onglets Navigation Connexion / Inscription -->
        <div class="auth-tabs">
          <button
            type="button"
            class="tab-btn"
            [class.active]="activeTab === 'login'"
            (click)="switchTab('login')"
          >
            Se Connecter
          </button>
          <button
            type="button"
            class="tab-btn"
            [class.active]="activeTab === 'register'"
            (click)="switchTab('register')"
          >
            Inscription Étudiante (@zig.univ.sn)
          </button>
        </div>

        @if (successMessage) {
          <div class="alert alert-success">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            <span>{{ successMessage }}</span>
          </div>
        }

        @if (errorMessage) {
          <div class="alert alert-danger">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span>{{ errorMessage }}</span>
          </div>
        }

        <!-- FORMULAIRE DE CONNEXION -->
        @if (activeTab === 'login') {
          <form (ngSubmit)="onSubmitLogin()" class="login-form">
            <div class="form-group">
              <label class="form-label">Adresse Email Institutionnelle</label>
              <input
                type="email"
                class="form-control"
                [(ngModel)]="email"
                name="email"
                placeholder="ex: prenom.nom@uasz.sn ou @zig.univ.sn"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">Mot de Passe</label>
              <input
                type="password"
                class="form-control"
                [(ngModel)]="password"
                name="password"
                placeholder="••••••••••••"
                required
              />
            </div>

            <button type="submit" class="btn btn-primary btn-block" [disabled]="loading">
              @if (loading) {
                <span>Connexion en cours...</span>
              } @else {
                <span>Se Connecter</span>
              }
            </button>
          </form>

          <div class="demo-section">
            <div class="demo-title">
              <span>Comptes de Démonstration Académique (1-clic)</span>
            </div>
            <div class="demo-grid">
              <button type="button" class="btn btn-outline btn-xs" (click)="fillDemo('admin.electoral@uasz.sn', 'AdminUasz2026!Securise')">
                👑 Super Admin
              </button>
              <button type="button" class="btn btn-outline btn-xs" (click)="fillDemo('commission@uasz.sn', 'Commission2026!')">
                ⚖️ Commission
              </button>
              <button type="button" class="btn btn-outline btn-xs" (click)="fillDemo('etudiant1@zig.univ.sn', 'Etudiant2026!')">
                🎓 Étudiant Électeur
              </button>
              <button type="button" class="btn btn-outline btn-xs" (click)="fillDemo('candidat1@zig.univ.sn', 'Candidat2026!')">
                📢 Candidat
              </button>
              <button type="button" class="btn btn-outline btn-xs" (click)="fillDemo('enseignant1@uasz.sn', 'Enseignant2026!')">
                👨‍🏫 Enseignant
              </button>
            </div>
          </div>
        }

        <!-- FORMULAIRE D'INSCRIPTION ÉTUDIANTE -->
        @if (activeTab === 'register') {
          <form (ngSubmit)="onSubmitRegister()" class="login-form">
            <div class="grid grid-cols-2" style="gap: 0.75rem;">
              <div class="form-group">
                <label class="form-label">Prénom *</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="regPrenom"
                  name="regPrenom"
                  placeholder="ex: Cheikh"
                  required
                />
              </div>
              <div class="form-group">
                <label class="form-label">Nom *</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="regNom"
                  name="regNom"
                  placeholder="ex: Diop"
                  required
                />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">Email Institutionnel (@zig.univ.sn) *</label>
              <input
                type="email"
                class="form-control"
                [(ngModel)]="regEmail"
                name="regEmail"
                placeholder="ex: c.diop@zig.univ.sn"
                required
              />
              <small class="field-help">Seul le domaine officiel @zig.univ.sn est admis pour les étudiants.</small>
            </div>

            <div class="form-group">
              <label class="form-label">N° Étudiant / Matricule UASZ *</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="regNumeroId"
                name="regNumeroId"
                placeholder="ex: ETUD-2026-1042"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">Numéro de Téléphone (optionnel)</label>
              <input
                type="tel"
                class="form-control"
                [(ngModel)]="regTelephone"
                name="regTelephone"
                placeholder="+221 77 000 00 00"
              />
            </div>

            <div class="grid grid-cols-2" style="gap: 0.75rem;">
              <div class="form-group">
                <label class="form-label">Mot de Passe *</label>
                <input
                  type="password"
                  class="form-control"
                  [(ngModel)]="regPassword"
                  name="regPassword"
                  placeholder="Min. 8 caractères"
                  required
                />
              </div>
              <div class="form-group">
                <label class="form-label">Confirmation *</label>
                <input
                  type="password"
                  class="form-control"
                  [(ngModel)]="regPasswordConfirm"
                  name="regPasswordConfirm"
                  placeholder="Répétez"
                  required
                />
              </div>
            </div>

            <button type="submit" class="btn btn-primary btn-block" [disabled]="regLoading">
              @if (regLoading) {
                <span>Création du compte...</span>
              } @else {
                <span>S'inscrire comme Électeur</span>
              }
            </button>
          </form>
        }

        <div class="login-footer">
          <p>La sécurité de vos identifiants est garantie par chiffrement Argon2id et session JWT à durée limitée.</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-wrapper {
      min-height: calc(100vh - 200px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem 1rem;
    }
    .login-card {
      width: 100%;
      max-width: 520px;
      padding: 2.5rem;
      border-top: 5px solid var(--primary);
    }
    .login-header {
      text-align: center;
      margin-bottom: 1.5rem;
    }
    .login-badge {
      display: inline-block;
      font-size: 0.75rem;
      font-weight: 800;
      color: var(--secondary);
      background: var(--secondary-light);
      padding: 0.25rem 0.65rem;
      border-radius: 9999px;
      letter-spacing: 0.08em;
      margin-bottom: 0.75rem;
    }
    .login-subtitle {
      color: var(--text-muted);
      font-size: 0.9rem;
    }
    .auth-tabs {
      display: flex;
      border-bottom: 2px solid var(--border-color);
      margin-bottom: 1.5rem;
    }
    .tab-btn {
      flex: 1;
      padding: 0.75rem 0.5rem;
      background: none;
      border: none;
      border-bottom: 3px solid transparent;
      margin-bottom: -2px;
      font-size: 0.9rem;
      font-weight: 600;
      color: var(--text-muted);
      cursor: pointer;
      transition: all 0.2s ease;
      text-align: center;
    }
    .tab-btn.active {
      color: var(--primary);
      border-bottom-color: var(--primary);
      font-weight: 700;
    }
    .field-help {
      display: block;
      color: var(--text-muted);
      font-size: 0.75rem;
      margin-top: 0.25rem;
    }
    .btn-block {
      width: 100%;
      padding: 0.75rem;
      font-size: 1rem;
      margin-top: 0.5rem;
    }
    .demo-section {
      margin-top: 2rem;
      padding-top: 1.5rem;
      border-top: 1px dashed var(--border-color);
    }
    .demo-title {
      font-size: 0.78rem;
      font-weight: 700;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: 0.85rem;
      text-align: center;
    }
    .demo-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.5rem;
    }
    .btn-xs {
      padding: 0.4rem 0.6rem;
      font-size: 0.75rem;
    }
    .login-footer {
      margin-top: 1.5rem;
      text-align: center;
      font-size: 0.75rem;
      color: #94a3b8;
    }
  `]
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  activeTab: 'login' | 'register' = 'login';

  // Login model
  email = '';
  password = '';
  loading = false;
  errorMessage = '';
  successMessage = '';

  // Register model
  regNom = '';
  regPrenom = '';
  regEmail = '';
  regNumeroId = '';
  regTelephone = '';
  regPassword = '';
  regPasswordConfirm = '';
  regLoading = false;

  switchTab(tab: 'login' | 'register'): void {
    this.activeTab = tab;
    this.errorMessage = '';
    this.successMessage = '';
  }

  fillDemo(email: string, pass: string): void {
    this.email = email;
    this.password = pass;
    this.errorMessage = '';
    this.successMessage = '';
    this.activeTab = 'login';
  }

  onSubmitLogin(): void {
    if (!this.email || !this.password) {
      this.errorMessage = 'Veuillez renseigner votre email et mot de passe.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.login(this.email.trim(), this.password)
      .pipe(finalize(() => {
        this.loading = false;
      }))
      .subscribe({
        next: (res) => {
          this.loading = false;
          if (res.success && res.data) {
            const destination = this.authService.isCommission() ? '/admin' : '/dashboard';
            this.router.navigate([destination]).then((success) => {
              if (!success) {
                console.warn('Navigation vers ' + destination + ' redirigée vers /dashboard');
                this.router.navigate(['/dashboard']);
              }
            }).catch(() => {
              this.router.navigate(['/dashboard']);
            });
          } else {
            this.errorMessage = res.message || 'Authentification échouée.';
          }
        },
        error: (err) => {
          this.loading = false;
          if (err.status === 401) {
            this.errorMessage = err.error?.message || 'Identifiants invalides (email ou mot de passe incorrect).';
          } else if (err.status === 403) {
            this.errorMessage = err.error?.message || 'Accès refusé. Compte non autorisé ou suspendu.';
          } else if (err.status === 405) {
            this.errorMessage = 'Méthode non autorisée (405). La requête a ciblé une ressource statique au lieu du backend Render.';
          } else if (err.status === 0) {
            this.errorMessage = 'Impossible de joindre le serveur backend (Render). Vérifiez votre connexion ou patientez quelques secondes pendant le réveil du serveur.';
          } else if (err.status >= 500) {
            this.errorMessage = 'Erreur interne du serveur backend. Veuillez réessayer dans quelques instants.';
          } else {
            this.errorMessage = (typeof err.error === 'object' && err.error?.message)
              ? err.error.message
              : `Erreur de connexion (${err.status || 'réseau'}). Veuillez réessayer.`;
          }
        }
      });
  }

  onSubmitRegister(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.regPrenom || !this.regNom || !this.regEmail || !this.regNumeroId || !this.regPassword) {
      this.errorMessage = 'Veuillez renseigner tous les champs obligatoires (*).';
      return;
    }

    const trimmedEmail = this.regEmail.trim().toLowerCase();
    const institutionalRegex = /^[a-zA-Z0-9._%+-]+@zig\.univ\.sn$/;
    if (!institutionalRegex.test(trimmedEmail)) {
      this.errorMessage = "Seules les adresses institutionnelles officielles se terminant par @zig.univ.sn sont autorisées pour l'inscription étudiante.";
      return;
    }

    if (this.regPassword.length < 8) {
      this.errorMessage = 'Le mot de passe doit comporter au moins 8 caractères.';
      return;
    }

    if (this.regPassword !== this.regPasswordConfirm) {
      this.errorMessage = 'Les deux mots de passe saisis ne sont pas identiques.';
      return;
    }

    this.regLoading = true;
    this.authService.register({
      email: trimmedEmail,
      password: this.regPassword,
      nom: this.regNom.trim(),
      prenom: this.regPrenom.trim(),
      numeroIdentification: this.regNumeroId.trim(),
      telephone: this.regTelephone ? this.regTelephone.trim() : undefined
    }).pipe(finalize(() => {
      this.regLoading = false;
    })).subscribe({
      next: (res) => {
        this.successMessage = 'Compte électeur créé avec succès ! Vous pouvez maintenant vous connecter.';
        this.email = trimmedEmail;
        this.password = '';
        this.activeTab = 'login';
        // Réinitialiser les champs d'inscription
        this.regNom = '';
        this.regPrenom = '';
        this.regEmail = '';
        this.regNumeroId = '';
        this.regTelephone = '';
        this.regPassword = '';
        this.regPasswordConfirm = '';
      },
      error: (err) => {
        if (err.status === 400 || err.status === 409) {
          this.errorMessage = err.error?.message || 'Erreur lors de la création du compte (doublon ou données non conformes).';
        } else if (err.status === 0) {
          this.errorMessage = 'Impossible de contacter le serveur backend. Veuillez vérifier votre connexion.';
        } else {
          this.errorMessage = (typeof err.error === 'object' && err.error?.message)
            ? err.error.message
            : `Erreur lors de l'inscription (${err.status || 'réseau'}).`;
        }
      }
    });
  }
}
