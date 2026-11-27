import { Component, OnInit, inject, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { timeout, catchError, of } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { ApiService } from '../core/services/api.service';
import { Eligibility } from '../core/models/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="container dashboard-container">
      <!-- En-tête profil électeur -->
      <div class="dashboard-header card">
        <div class="user-greeting">
          <h2>Bienvenue, {{ authService.currentUser()?.prenom }} {{ authService.currentUser()?.nom }}</h2>
          <div class="user-badges">
            <span class="badge badge-primary">Catégorie : {{ authService.currentUser()?.categorie }}</span>
            <span class="badge badge-secondary">Statut : Inscrit sur les listes électorales</span>
          </div>
        </div>
        <div class="header-action">
          <a routerLink="/complaints" class="btn btn-outline btn-sm">
            <span>Déposer une Réclamation</span>
          </a>
        </div>
      </div>

      <!-- Scrutins éligibles pour cet électeur -->
      <div class="section-title flex-between">
        <div>
          <h3>Mes Scrutins Éligibles</h3>
          <p class="text-muted">Consultez les élections auxquelles vous avez légalement le droit de participer.</p>
        </div>
        <button class="btn btn-outline btn-sm" (click)="loadElections()" [disabled]="loading()" title="Actualiser mes scrutins">
          <span [class.spin]="loading()">🔄</span> Actualiser
        </button>
      </div>

      @if (loading()) {
        <div class="card text-center loading-card" style="padding: 3rem;">
          <div class="spinner"></div>
          <p style="margin-top: 1rem; color: var(--text-muted);">Chargement de vos scrutins personnalisés...</p>
        </div>
      } @else if (errorMessage()) {
        <div class="card error-card" style="padding: 2rem; border-left: 4px solid var(--danger);">
          <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
            <div>
              <p class="text-danger" style="margin: 0; font-weight: 500;">{{ errorMessage() }}</p>
              <small class="text-muted">Vérifiez votre connexion au serveur électoral UASZ.</small>
            </div>
            <button class="btn btn-primary btn-sm" (click)="loadElections()">
              🔄 Réessayer
            </button>
          </div>
        </div>
      } @else if (elections().length === 0) {
        <div class="card text-center" style="padding: 3rem;">
          <p>Aucun scrutin électoral actif ne correspond à votre collège pour le moment.</p>
          <a routerLink="/elections" class="btn btn-outline" style="margin-top: 1rem;">Voir tous les scrutins publics</a>
        </div>
      } @else {
        <div class="grid grid-cols-2">
          @for (item of elections(); track item.id) {
            <div class="card election-card" [class.voted]="isVoted(item)">
              <div class="card-header flex-between">
                <div>
                  <span class="badge badge-primary">{{ item.election.type || 'SCRUTIN' }}</span>
                  <h3 style="margin-top: 0.5rem;">{{ item.election.titre }}</h3>
                </div>
                <div>
                  @if (isVoted(item)) {
                    <span class="badge badge-success">✓ Voté</span>
                  } @else if (item.election.statut === 'VOTING_OPEN') {
                    <span class="badge badge-warning">Scrutin Ouvert</span>
                  } @else {
                    <span class="badge badge-secondary">{{ item.election.statut }}</span>
                  }
                </div>
              </div>

              <p class="election-desc">{{ item.election.description || 'Scrutin universitaire officiel UASZ.' }}</p>

              <div class="election-meta">
                <div><strong>Collège :</strong> {{ item.election.college?.nom || 'Général' }}</div>
                <div><strong>Clôture :</strong> {{ item.election.dateCloture | date:'dd/MM/yyyy HH:mm' }}</div>
              </div>

              <div class="card-footer">
                @if (isVoted(item)) {
                  <button class="btn btn-outline btn-block" disabled>
                    ✓ Votre bulletin est scellé dans l'urne
                  </button>
                } @else if (item.election.statut === 'VOTING_OPEN') {
                  <a [routerLink]="['/voting', item.election.id || item.electionId]" class="btn btn-primary btn-block">
                    Accéder à l'Isoloir Électronique →
                  </a>
                } @else {
                  <button class="btn btn-outline btn-block" disabled>
                    En attente de l'ouverture officielle
                  </button>
                }
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 2rem 1.25rem;
    }
    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2.5rem;
      background: linear-gradient(135deg, #ffffff, var(--primary-light));
      border-left: 6px solid var(--primary);
    }
    .user-greeting {
      margin-right: 1rem;
    }
    .user-badges {
      display: flex;
      gap: 0.5rem;
      margin-top: 0.5rem;
      flex-wrap: wrap;
    }
    .section-title {
      margin-bottom: 1.5rem;
    }
    .text-muted {
      color: var(--text-muted);
      font-size: 0.9rem;
    }
    .election-card {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .election-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    }
    .election-card.voted {
      background-color: #f8fafc;
      border-color: #cbd5e1;
    }
    .flex-between {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .election-desc {
      font-size: 0.9rem;
      color: var(--text-main);
      margin-bottom: 1rem;
    }
    .election-meta {
      font-size: 0.825rem;
      color: var(--text-muted);
      margin-bottom: 1.5rem;
      padding: 0.75rem;
      background: #f8fafc;
      border-radius: var(--radius-sm);
    }
    .btn-block {
      width: 100%;
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
    .text-danger {
      color: #dc2626;
    }
    .error-card {
      background-color: #fef2f2;
      border-color: #fecaca;
    }
  `]
})
export class DashboardComponent implements OnInit {
  authService = inject(AuthService);
  private apiService = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);

  loading = signal<boolean>(true);
  elections = signal<Eligibility[]>([]);
  errorMessage = signal<string>('');

  ngOnInit(): void {
    this.loadElections();
  }

  loadElections(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.apiService.getMyEligibleElections().pipe(
      timeout(12000),
      catchError((err) => {
        return of({
          success: false,
          message: err?.name === 'TimeoutError'
            ? 'Le délai d’attente a expiré (serveur en réveil ou indisponible).'
            : (err?.error?.message || 'Impossible de charger vos scrutins personnalisés.'),
          data: []
        });
      })
    ).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.elections.set(res.data);
        } else if (!res.success) {
          this.errorMessage.set(res.message || 'Impossible de charger vos scrutins personnalisés.');
        } else {
          this.elections.set([]);
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message || 'Erreur de connexion au serveur électoral.');
        this.cdr.markForCheck();
      }
    });
  }

  isVoted(item: Eligibility): boolean {
    if (!item) return false;
    return Boolean(item.aVote || (item as any).avote);
  }
}
