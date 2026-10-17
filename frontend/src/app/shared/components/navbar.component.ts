import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <header class="navbar">
      <div class="container navbar-container">
        <!-- Logo UASZ -->
        <a routerLink="/" class="navbar-brand">
          <div class="brand-logo">
            <span class="logo-text">UASZ</span>
          </div>
          <div class="brand-title">
            <span class="app-name">Vote<span class="highlight">UASZ</span></span>
            <span class="sub-name">Élections Universitaires</span>
          </div>
        </a>

        <!-- Liens de navigation Desktop -->
        <nav class="nav-links" [class.mobile-open]="mobileMenuOpen">
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" (click)="mobileMenuOpen = false">Accueil</a>
          <a routerLink="/elections" routerLinkActive="active" (click)="mobileMenuOpen = false">Scrutins</a>
          <a routerLink="/candidacies" routerLinkActive="active" (click)="mobileMenuOpen = false">Candidats</a>
          <a routerLink="/results" routerLinkActive="active" (click)="mobileMenuOpen = false">Résultats Direct</a>
          <a routerLink="/verify-receipt" routerLinkActive="active" (click)="mobileMenuOpen = false">Vérifier Reçu</a>

          @if (authService.isAuthenticated()) {
            <a routerLink="/dashboard" routerLinkActive="active" class="link-auth" (click)="mobileMenuOpen = false">Mon Espace</a>
            @if (authService.isCommission()) {
              <a routerLink="/admin" routerLinkActive="active" class="link-admin" (click)="mobileMenuOpen = false">
                <span class="admin-badge">Admin</span>
              </a>
            }
          }
        </nav>

        <!-- Espace Utilisateur / Connexion -->
        <div class="navbar-actions">
          @if (authService.isAuthenticated()) {
            <div class="user-pill">
              <div class="user-avatar">{{ userInitials }}</div>
              <div class="user-info">
                <span class="user-name">{{ authService.currentUser()?.prenom }} {{ authService.currentUser()?.nom }}</span>
                <span class="user-role">{{ primaryRoleLabel }}</span>
              </div>
              <button (click)="logout()" class="btn-logout" title="Déconnexion">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                  <polyline points="16 17 21 12 16 7"></polyline>
                  <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
              </button>
            </div>
          } @else {
            <a routerLink="/login" class="btn btn-secondary btn-sm">Se Connecter</a>
          }

          <!-- Menu Hamburger Mobile -->
          <button class="hamburger" (click)="mobileMenuOpen = !mobileMenuOpen" aria-label="Menu">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="12" x2="21" y2="12"></line>
              <line x1="3" y1="6" x2="21" y2="6"></line>
              <line x1="3" y1="18" x2="21" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .navbar {
      background-color: #ffffff;
      border-bottom: 2px solid var(--border-color);
      position: sticky;
      top: 0;
      z-index: 1000;
      box-shadow: 0 2px 4px rgba(0,0,0,0.04);
    }
    .navbar-container {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 70px;
    }
    .navbar-brand {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      text-decoration: none;
    }
    .brand-logo {
      background: linear-gradient(135deg, var(--primary), var(--secondary));
      color: #ffffff;
      width: 42px;
      height: 42px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 900;
      font-size: 0.9rem;
      letter-spacing: 0.05em;
      box-shadow: 0 2px 8px rgba(16, 94, 56, 0.3);
    }
    .brand-title {
      display: flex;
      flex-direction: column;
    }
    .app-name {
      font-size: 1.25rem;
      font-weight: 800;
      color: var(--primary);
      line-height: 1.1;
    }
    .app-name .highlight {
      color: var(--secondary);
    }
    .sub-name {
      font-size: 0.7rem;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.08em;
      font-weight: 600;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 1.5rem;
    }
    .nav-links a {
      font-size: 0.925rem;
      font-weight: 600;
      color: var(--dark-surface);
      text-decoration: none;
      padding: 0.5rem 0.25rem;
      border-bottom: 2px solid transparent;
      transition: all 0.2s ease;
    }
    .nav-links a:hover, .nav-links a.active {
      color: var(--primary);
      border-bottom-color: var(--primary);
    }
    .admin-badge {
      background-color: var(--primary);
      color: #ffffff;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 700;
    }
    .navbar-actions {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .user-pill {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      background: #f1f5f9;
      padding: 0.35rem 0.75rem;
      border-radius: 9999px;
      border: 1px solid var(--border-color);
    }
    .user-avatar {
      width: 32px;
      height: 32px;
      background: var(--primary);
      color: #ffffff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.8rem;
      font-weight: 700;
    }
    .user-info {
      display: flex;
      flex-direction: column;
      line-height: 1.1;
    }
    .user-name {
      font-size: 0.825rem;
      font-weight: 700;
      color: var(--dark);
    }
    .user-role {
      font-size: 0.68rem;
      color: var(--text-muted);
    }
    .btn-logout {
      background: none;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      display: flex;
      align-items: center;
      padding: 0.25rem;
      border-radius: 4px;
      transition: color 0.2s ease;
    }
    .btn-logout:hover {
      color: var(--danger);
    }
    .hamburger {
      display: none;
      background: none;
      border: none;
      color: var(--dark);
      cursor: pointer;
    }
    @media (max-width: 900px) {
      .hamburger { display: block; }
      .nav-links {
        display: none;
        position: absolute;
        top: 70px;
        left: 0;
        right: 0;
        background: #ffffff;
        flex-direction: column;
        padding: 1.5rem;
        border-bottom: 2px solid var(--border-color);
        box-shadow: var(--shadow-md);
        gap: 1rem;
      }
      .nav-links.mobile-open { display: flex; }
      .user-info { display: none; }
    }
  `]
})
export class NavbarComponent {
  authService = inject(AuthService);
  mobileMenuOpen = false;

  get userInitials(): string {
    const u = this.authService.currentUser();
    if (!u) return 'U';
    return (u.prenom.charAt(0) + u.nom.charAt(0)).toUpperCase();
  }

  get primaryRoleLabel(): string {
    if (this.authService.isSuperAdmin()) return 'Super Admin';
    if (this.authService.isCommission()) return 'Commission';
    if (this.authService.isCandidat()) return 'Candidat';
    return 'Électeur';
  }

  logout(): void {
    this.authService.logout();
    window.location.href = '/';
  }
}
