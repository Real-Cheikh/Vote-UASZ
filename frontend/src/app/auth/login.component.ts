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
          <h2>Connexion VoteUASZ</h2>
          <p class="login-subtitle">Accédez à votre espace électoral universitaire sécurisé</p>
        </div>

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

        <form (ngSubmit)="onSubmit()" class="login-form">
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
      max-width: 480px;
      padding: 2.5rem;
      border-top: 5px solid var(--primary);
    }
    .login-header {
      text-align: center;
      margin-bottom: 2rem;
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

  email = '';
  password = '';
  loading = false;
  errorMessage = '';

  fillDemo(email: string, pass: string): void {
    this.email = email;
    this.password = pass;
    this.errorMessage = '';
  }

  onSubmit(): void {
    if (!this.email || !this.password) {
      this.errorMessage = 'Veuillez renseigner votre email et mot de passe.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.email.trim(), this.password)
      .pipe(finalize(() => {
        this.loading = false;
      }))
      .subscribe({
        next: (res) => {
          if (res.success && res.data) {
            const destination = this.authService.isCommission() ? '/admin' : '/dashboard';
            this.router.navigate([destination]).then((success) => {
              if (!success) {
                console.warn('Navigation vers ' + destination + ' bloquée, redirection vers /dashboard');
                this.router.navigate(['/dashboard']);
              }
            }).catch((navErr) => {
              console.error('Erreur lors de la redirection :', navErr);
              this.router.navigate(['/dashboard']);
            });
          } else {
            this.errorMessage = res.message || 'Authentification échouée.';
          }
        },
        error: (err) => {
          if (err.status === 401) {
            this.errorMessage = err.error?.message || 'Identifiants invalides ou compte suspendu.';
          } else if (err.status === 0) {
            this.errorMessage = 'Impossible de joindre le serveur. Vérifiez que les conteneurs sont actifs.';
          } else if (err.status >= 500) {
            this.errorMessage = 'Erreur interne du serveur lors de la connexion. Veuillez réessayer.';
          } else {
            this.errorMessage = err.error?.message || 'Une erreur est survenue lors de la connexion.';
          }
        }
      });
  }
}
