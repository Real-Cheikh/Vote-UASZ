import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
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
      <div class="section-title">
        <h3>Mes Scrutins Éligibles</h3>
        <p class="text-muted">Consultez les élections auxquelles vous avez légalement le droit de participer.</p>
      </div>

      @if (loading) {
        <div class="card text-center" style="padding: 3rem;">
          <p>Chargement de vos scrutins personnalisés...</p>
        </div>
      } @else if (errorMessage) {
        <div class="alert alert-danger">{{ errorMessage }}</div>
      } @else if (elections.length === 0) {
        <div class="card text-center" style="padding: 3rem;">
          <p>Aucun scrutin électoral actif ne correspond à votre collège pour le moment.</p>
          <a routerLink="/elections" class="btn btn-outline" style="margin-top: 1rem;">Voir tous les scrutins publics</a>
        </div>
      } @else {
        <div class="grid grid-cols-2">
          @for (item of elections; track item.id) {
            <div class="card election-card" [class.voted]="item.aVote">
              <div class="card-header flex-between">
                <div>
                  <span class="badge badge-primary">{{ item.election.type }}</span>
                  <h3 style="margin-top: 0.5rem;">{{ item.election.titre }}</h3>
                </div>
                <div>
                  @if (item.aVote) {
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
                <div><strong>Collège :</strong> {{ item.election.college?.nom }}</div>
                <div><strong>Clôture :</strong> {{ item.election.dateCloture | date:'dd/MM/yyyy HH:mm' }}</div>
              </div>

              <div class="card-footer">
                @if (item.aVote) {
                  <button class="btn btn-outline btn-block" disabled>
                    ✓ Votre bulletin est scellé dans l'urne
                  </button>
                } @else if (item.election.statut === 'VOTING_OPEN') {
                  <a [routerLink]="['/voting', item.election.id]" class="btn btn-primary btn-block">
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
    .user-badges {
      display: flex;
      gap: 0.5rem;
      margin-top: 0.5rem;
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
    }
    .election-card.voted {
      background-color: #f8fafc;
      border-color: #cbd5e1;
    }
    .flex-between {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
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
  `]
})
export class DashboardComponent implements OnInit {
  authService = inject(AuthService);
  private apiService = inject(ApiService);

  elections: Eligibility[] = [];
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    this.apiService.getMyEligibleElections().subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.elections = res.data;
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = 'Impossible de charger vos scrutins personnalisés.';
      }
    });
  }
}
