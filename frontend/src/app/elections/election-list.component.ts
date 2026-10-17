import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../core/services/api.service';
import { Election } from '../core/models/models';

@Component({
  selector: 'app-election-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="container elections-container">
      <div class="elections-header">
        <div>
          <h2>Scrutins Universitaires UASZ</h2>
          <p class="subtitle">Consultez l'ensemble des élections ouvertes, à venir et passées de l'établissement.</p>
        </div>
      </div>

      <!-- Filtres -->
      <div class="filters-bar">
        <button class="filter-btn" [class.active]="filter === 'ALL'" (click)="setFilter('ALL')">Tous les scrutins</button>
        <button class="filter-btn" [class.active]="filter === 'OPEN'" (click)="setFilter('OPEN')">🟢 En cours de vote</button>
        <button class="filter-btn" [class.active]="filter === 'CANDIDACIES'" (click)="setFilter('CANDIDACIES')">📝 Dépôt candidatures</button>
        <button class="filter-btn" [class.active]="filter === 'CLOSED'" (click)="setFilter('CLOSED')">📊 Résultats publiés</button>
      </div>

      @if (loading) {
        <div class="card text-center" style="padding: 3rem;">
          <p>Chargement des scrutins...</p>
        </div>
      } @else if (filteredElections.length === 0) {
        <div class="card text-center" style="padding: 3rem;">
          <p>Aucun scrutin ne correspond au filtre sélectionné.</p>
        </div>
      } @else {
        <div class="grid grid-cols-2">
          @for (e of filteredElections; track e.id) {
            <div class="card election-card">
              <div class="card-header flex-between">
                <div>
                  <span class="badge badge-primary">{{ e.type }}</span>
                  <h3 style="margin-top: 0.5rem;">{{ e.titre }}</h3>
                </div>
                <div>
                  <span class="badge" [ngClass]="getStatusBadgeClass(e.statut)">
                    {{ getStatusLabel(e.statut) }}
                  </span>
                </div>
              </div>

              <p class="election-desc">{{ e.description || 'Scrutin universitaire officiel régi par la Commission électorale UASZ.' }}</p>

              <div class="election-meta">
                <div><strong>Collège électoral :</strong> {{ e.college?.nom }} ({{ e.college?.ufr || 'Toutes UFR' }})</div>
                <div><strong>Ouverture :</strong> {{ e.dateOuverture | date:'dd/MM/yyyy HH:mm' }}</div>
                <div><strong>Clôture :</strong> {{ e.dateCloture | date:'dd/MM/yyyy HH:mm' }}</div>
              </div>

              <div class="card-footer-buttons">
                @if (e.statut === 'VOTING_OPEN') {
                  <a [routerLink]="['/voting', e.id]" class="btn btn-primary btn-block">
                    Accéder au Vote →
                  </a>
                } @else if (e.statut === 'RESULTS_PUBLISHED') {
                  <a [routerLink]="['/results', e.id]" class="btn btn-secondary btn-block">
                    Voir les Résultats Officiels →
                  </a>
                } @else if (e.statut === 'CANDIDATURES_OPEN') {
                  <a routerLink="/candidacies" class="btn btn-outline btn-block">
                    Consulter les Candidatures →
                  </a>
                } @else {
                  <button class="btn btn-outline btn-block" disabled>
                    Scrutin clôturé
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
    .elections-container {
      padding: 2.5rem 1.25rem;
    }
    .elections-header {
      margin-bottom: 2rem;
    }
    .subtitle {
      color: var(--text-muted);
      font-size: 0.95rem;
    }
    .filters-bar {
      display: flex;
      gap: 0.75rem;
      margin-bottom: 2rem;
      flex-wrap: wrap;
    }
    .filter-btn {
      padding: 0.5rem 1rem;
      background: #ffffff;
      border: 1px solid var(--border-color);
      border-radius: 9999px;
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--text-main);
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .filter-btn:hover, .filter-btn.active {
      background: var(--primary);
      color: #ffffff;
      border-color: var(--primary);
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
      background: #f8fafc;
      padding: 0.85rem;
      border-radius: var(--radius-sm);
      margin-bottom: 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }
    .btn-block {
      width: 100%;
    }
  `]
})
export class ElectionListComponent implements OnInit {
  private apiService = inject(ApiService);

  elections: Election[] = [];
  filteredElections: Election[] = [];
  loading = true;
  filter: 'ALL' | 'OPEN' | 'CANDIDACIES' | 'CLOSED' = 'ALL';

  ngOnInit(): void {
    this.apiService.getPublicElections().subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.elections = res.data;
          this.applyFilter();
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  setFilter(f: 'ALL' | 'OPEN' | 'CANDIDACIES' | 'CLOSED'): void {
    this.filter = f;
    this.applyFilter();
  }

  applyFilter(): void {
    if (this.filter === 'ALL') {
      this.filteredElections = this.elections;
    } else if (this.filter === 'OPEN') {
      this.filteredElections = this.elections.filter(e => e.statut === 'VOTING_OPEN');
    } else if (this.filter === 'CANDIDACIES') {
      this.filteredElections = this.elections.filter(e => e.statut === 'CANDIDATURES_OPEN');
    } else if (this.filter === 'CLOSED') {
      this.filteredElections = this.elections.filter(e => e.statut === 'RESULTS_PUBLISHED' || e.statut === 'ARCHIVED');
    }
  }

  getStatusBadgeClass(statut: string): string {
    switch (statut) {
      case 'VOTING_OPEN': return 'badge-warning';
      case 'RESULTS_PUBLISHED': return 'badge-success';
      case 'CANDIDATURES_OPEN': return 'badge-primary';
      default: return 'badge-secondary';
    }
  }

  getStatusLabel(statut: string): string {
    switch (statut) {
      case 'VOTING_OPEN': return 'Vote en cours';
      case 'RESULTS_PUBLISHED': return 'Résultats';
      case 'CANDIDATURES_OPEN': return 'Dépôt ouvert';
      case 'COUNTING': return 'Dépouillement';
      case 'READY_TO_VOTE': return 'Prêt à voter';
      default: return statut;
    }
  }
}
