import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApiService } from '../core/services/api.service';
import { Election } from '../core/models/models';

@Component({
  selector: 'app-election-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container elections-container">
      <div class="elections-header flex-between">
        <div>
          <h2>Scrutins Universitaires UASZ</h2>
          <p class="subtitle">Consultez l'ensemble des élections ouvertes, à venir et passées de l'établissement.</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-outline btn-sm" (click)="loadElections(true)" [disabled]="loading" title="Actualiser la liste">
            <span [class.spin]="loading">🔄</span> Actualiser
          </button>
        </div>
      </div>

      <!-- Filtres et Recherche -->
      <div class="toolbar-bar">
        <div class="filters-bar">
          <button class="filter-btn" [class.active]="filter === 'ALL'" (click)="setFilter('ALL')">
            Tous ({{ elections.length }})
          </button>
          <button class="filter-btn" [class.active]="filter === 'OPEN'" (click)="setFilter('OPEN')">
            🟢 En cours de vote
          </button>
          <button class="filter-btn" [class.active]="filter === 'CANDIDACIES'" (click)="setFilter('CANDIDACIES')">
            📝 Dépôt candidatures
          </button>
          <button class="filter-btn" [class.active]="filter === 'CLOSED'" (click)="setFilter('CLOSED')">
            📊 Résultats publiés
          </button>
        </div>

        <div class="search-box">
          <input
            type="text"
            class="form-control form-control-sm search-input"
            placeholder="Rechercher par titre ou collège..."
            [(ngModel)]="searchQuery"
            (ngModelChange)="applyFilter()"
          />
          @if (searchQuery) {
            <button class="search-clear-btn" (click)="searchQuery = ''; applyFilter()">✕</button>
          }
        </div>
      </div>

      <!-- État d'erreur -->
      @if (errorMessage) {
        <div class="card error-card">
          <div class="error-content">
            <span class="error-icon">⚠️</span>
            <div>
              <h4>Erreur de chargement des scrutins</h4>
              <p>{{ errorMessage }}</p>
            </div>
          </div>
          <button class="btn btn-primary btn-sm" (click)="loadElections(true)">
            🔄 Réessayer
          </button>
        </div>
      }

      <!-- État de chargement -->
      @if (loading && elections.length === 0) {
        <div class="card text-center loading-card">
          <div class="spinner"></div>
          <p class="loading-text">Chargement instantané des scrutins universitaires...</p>
        </div>
      } @else if (!errorMessage && filteredElections.length === 0) {
        <div class="card text-center" style="padding: 3rem;">
          <p class="empty-text">Aucun scrutin ne correspond à votre recherche ou filtre.</p>
          @if (searchQuery || filter !== 'ALL') {
            <button class="btn btn-outline btn-sm" style="margin-top: 1rem;" (click)="resetFilters()">
              Réinitialiser les filtres
            </button>
          }
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
                <div><strong>Collège électoral :</strong> {{ e.college?.nom || 'Général' }} ({{ e.college?.ufr || 'Toutes UFR' }})</div>
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
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .subtitle {
      color: var(--text-muted);
      font-size: 0.95rem;
    }
    .toolbar-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
      margin-bottom: 2rem;
      flex-wrap: wrap;
    }
    .filters-bar {
      display: flex;
      gap: 0.5rem;
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
    .search-box {
      position: relative;
      min-width: 280px;
    }
    .search-input {
      padding-right: 2rem;
      border-radius: 9999px;
    }
    .search-clear-btn {
      position: absolute;
      right: 0.75rem;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      cursor: pointer;
      color: var(--text-muted);
      font-size: 0.85rem;
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
    .error-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #fef2f2;
      border: 1px solid #fecaca;
      padding: 1.25rem 1.5rem;
      margin-bottom: 2rem;
      gap: 1rem;
      flex-wrap: wrap;
    }
    .error-content {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .error-icon {
      font-size: 1.75rem;
    }
    .loading-card {
      padding: 4rem 2rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }
    .spinner {
      width: 40px;
      height: 40px;
      border: 4px solid #e2e8f0;
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    .loading-text {
      color: var(--text-muted);
      font-size: 0.95rem;
    }
    .spin {
      display: inline-block;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class ElectionListComponent implements OnInit {
  private apiService = inject(ApiService);

  elections: Election[] = [];
  filteredElections: Election[] = [];
  loading = true;
  filter: 'ALL' | 'OPEN' | 'CANDIDACIES' | 'CLOSED' = 'ALL';
  searchQuery = '';
  errorMessage = '';

  ngOnInit(): void {
    this.loadElections(false);
  }

  loadElections(forceReload: boolean = false): void {
    this.loading = true;
    this.errorMessage = '';

    this.apiService.getPublicElections(forceReload).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.elections = res.data;
          this.applyFilter();
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Impossible de joindre le serveur électoral. Veuillez vérifier votre connexion.';
      }
    });
  }

  setFilter(f: 'ALL' | 'OPEN' | 'CANDIDACIES' | 'CLOSED'): void {
    this.filter = f;
    this.applyFilter();
  }

  resetFilters(): void {
    this.filter = 'ALL';
    this.searchQuery = '';
    this.applyFilter();
  }

  applyFilter(): void {
    let list = this.elections;

    if (this.filter === 'OPEN') {
      list = list.filter(e => e.statut === 'VOTING_OPEN');
    } else if (this.filter === 'CANDIDACIES') {
      list = list.filter(e => e.statut === 'CANDIDATURES_OPEN');
    } else if (this.filter === 'CLOSED') {
      list = list.filter(e => e.statut === 'RESULTS_PUBLISHED' || e.statut === 'ARCHIVED');
    }

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase().trim();
      list = list.filter(e =>
        (e.titre && e.titre.toLowerCase().includes(q)) ||
        (e.description && e.description.toLowerCase().includes(q)) ||
        (e.college?.nom && e.college.nom.toLowerCase().includes(q)) ||
        (e.type && e.type.toLowerCase().includes(q))
      );
    }

    this.filteredElections = list;
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
