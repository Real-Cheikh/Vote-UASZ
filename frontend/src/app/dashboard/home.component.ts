import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../core/services/api.service';
import { AuthService } from '../core/services/auth.service';
import { Election } from '../core/models/models';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <!-- Hero Banner -->
    <section class="hero">
      <div class="container hero-container">
        <div class="hero-content">
          <div class="hero-badge">Plateforme Électorale Officielle • UASZ Sénégal</div>
          <h1 class="hero-title">La Démocratie Universitaire Numérique et Sécurisée</h1>
          <p class="hero-desc">
            VoteUASZ modernise les élections de l'Université Assane Seck de Ziguinchor :
            unicité stricte, secret absolu du scrutin, double contrôle OTP et audit cryptographique transparent.
          </p>
          <div class="hero-buttons">
            @if (authService.isAuthenticated()) {
              <a routerLink="/dashboard" class="btn btn-primary btn-lg">Accéder à Mon Espace Électeur →</a>
            } @else {
              <a routerLink="/login" class="btn btn-primary btn-lg">Se Connecter & Voter →</a>
            }
            <a routerLink="/verify-receipt" class="btn btn-outline btn-lg">Vérifier un Reçu de Vote</a>
          </div>
        </div>
        <div class="hero-card card">
          <div class="uasz-crest">
            <span class="crest-title">UASZ</span>
            <span class="crest-subtitle">Ziguinchor</span>
          </div>
          <div class="feature-list">
            <div class="feature-item">
              <span class="feature-icon">🔒</span>
              <div>
                <strong>Secret du Bulletin</strong>
                <p>Découplage strict entre votre identité et votre choix de vote.</p>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-icon">🛡️</span>
              <div>
                <strong>Anti-Double Vote</strong>
                <p>Consommation atomique immédiate du droit de vote sous PostgreSQL.</p>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-icon">📱</span>
              <div>
                <strong>Code OTP 5 minutes</strong>
                <p>Authentification forte à deux facteurs par code unique temporaire.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Scrutins Ouverts en ce moment -->
    <section class="container section-elections">
      <div class="section-header">
        <div>
          <h2>Scrutins Électoraux en Cours</h2>
          <p class="text-muted">Participez à la désignation de vos représentants universitaires.</p>
        </div>
        <a routerLink="/elections" class="btn btn-outline btn-sm">Tous les scrutins</a>
      </div>

      @if (loading) {
        <div class="card text-center" style="padding: 2.5rem;">
          <p>Chargement des scrutins officiels...</p>
        </div>
      } @else if (elections.length === 0) {
        <div class="card text-center" style="padding: 2.5rem;">
          <p>Aucun scrutin public n'est actuellement programmé.</p>
        </div>
      } @else {
        <div class="grid grid-cols-3">
          @for (e of elections; track e.id) {
            <div class="card election-box">
              <div class="card-header flex-between">
                <span class="badge badge-primary">{{ e.type }}</span>
                @if (e.statut === 'VOTING_OPEN') {
                  <span class="badge badge-warning">En cours</span>
                } @else if (e.statut === 'RESULTS_PUBLISHED') {
                  <span class="badge badge-success">Résultats publiés</span>
                } @else {
                  <span class="badge badge-secondary">{{ e.statut }}</span>
                }
              </div>
              <h3>{{ e.titre }}</h3>
              <p class="election-desc">{{ e.description || 'Scrutin institutionnel Université Assane Seck.' }}</p>
              <div class="election-dates">
                <span>Fin : {{ e.dateCloture | date:'dd/MM/yyyy HH:mm' }}</span>
              </div>
              <div class="election-btn">
                @if (e.statut === 'VOTING_OPEN') {
                  <a [routerLink]="['/voting', e.id]" class="btn btn-primary btn-block">Voter Maintenant</a>
                } @else if (e.statut === 'RESULTS_PUBLISHED') {
                  <a [routerLink]="['/results', e.id]" class="btn btn-outline btn-block">Consulter Résultats</a>
                } @else {
                  <a [routerLink]="['/elections']" class="btn btn-outline btn-block">Voir Détails</a>
                }
              </div>
            </div>
          }
        </div>
      }
    </section>

    <!-- Les 4 Piliers VoteUASZ -->
    <section class="pillars-section">
      <div class="container">
        <h2 class="text-center" style="margin-bottom: 2.5rem;">Les Garanties Démocratiques de VoteUASZ</h2>
        <div class="grid grid-cols-4">
          <div class="card pillar-card">
            <div class="pillar-icon">⚖️</div>
            <h4>Unicité du Vote</h4>
            <p>Transactions PostgreSQL et contraintes d'unicité empêchant le double vote, même sous requêtes concurrentes.</p>
          </div>
          <div class="card pillar-card">
            <div class="pillar-icon">🗳️</div>
            <h4>Secret de l'Urne</h4>
            <p>L'urne ne conserve aucune trace d'identité. Votre choix n'est associé à aucun identifiant personnel.</p>
          </div>
          <div class="card pillar-card">
            <div class="pillar-icon">📑</div>
            <h4>Procès-Verbal PDF</h4>
            <p>Génération automatique d'un procès-verbal officiel scellé par une empreinte numérique SHA-256 infalsifiable.</p>
          </div>
          <div class="card pillar-card">
            <div class="pillar-icon">📊</div>
            <h4>Diffusion Temps Réel</h4>
            <p>Les résultats et statistiques de participation sont diffusés en direct via WebSocket STOMP.</p>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .hero {
      background: linear-gradient(135deg, #0b4528, #105e38);
      color: #ffffff;
      padding: 4.5rem 0 3.5rem;
      border-bottom: 5px solid var(--secondary);
    }
    .hero-container {
      display: grid;
      grid-template-columns: 1.4fr 1fr;
      gap: 3rem;
      align-items: center;
    }
    .hero-badge {
      display: inline-block;
      background: rgba(201, 162, 39, 0.2);
      color: var(--secondary);
      border: 1px solid var(--secondary);
      padding: 0.35rem 0.85rem;
      border-radius: 9999px;
      font-size: 0.8rem;
      font-weight: 700;
      letter-spacing: 0.05em;
      margin-bottom: 1.25rem;
    }
    .hero-title {
      color: #ffffff;
      font-size: 2.75rem;
      font-weight: 800;
      line-height: 1.15;
      margin-bottom: 1.25rem;
    }
    .hero-desc {
      font-size: 1.1rem;
      line-height: 1.6;
      color: #e2e8f0;
      margin-bottom: 2rem;
    }
    .hero-buttons {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
    }
    .btn-lg {
      padding: 0.85rem 1.65rem;
      font-size: 1.05rem;
    }
    .hero .btn-outline {
      border-color: rgba(255, 255, 255, 0.4);
      color: #ffffff;
    }
    .hero .btn-outline:hover {
      background: rgba(255, 255, 255, 0.15);
      border-color: #ffffff;
    }
    .hero-card {
      color: var(--dark);
      border: none;
      box-shadow: 0 15px 35px rgba(0,0,0,0.25);
    }
    .uasz-crest {
      text-align: center;
      padding: 1rem 0;
      border-bottom: 2px dashed var(--border-color);
      margin-bottom: 1.25rem;
    }
    .crest-title {
      display: block;
      font-size: 1.75rem;
      font-weight: 900;
      color: var(--primary);
      letter-spacing: 0.1em;
    }
    .crest-subtitle {
      font-size: 0.85rem;
      color: var(--secondary);
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }
    .feature-list {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }
    .feature-item {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
    }
    .feature-icon {
      font-size: 1.5rem;
      background: var(--primary-light);
      padding: 0.5rem;
      border-radius: 10px;
    }
    .feature-item strong {
      display: block;
      font-size: 0.95rem;
      color: var(--dark);
    }
    .feature-item p {
      font-size: 0.825rem;
      color: var(--text-muted);
      margin: 0;
    }
    .section-elections {
      padding: 3.5rem 1.25rem;
    }
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      margin-bottom: 2rem;
    }
    .election-box {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .flex-between {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .election-desc {
      font-size: 0.875rem;
      color: var(--text-muted);
      margin: 0.75rem 0 1rem;
      flex-grow: 1;
    }
    .election-dates {
      font-size: 0.8rem;
      color: var(--text-muted);
      background: #f8fafc;
      padding: 0.5rem;
      border-radius: var(--radius-sm);
      margin-bottom: 1rem;
    }
    .btn-block { width: 100%; }
    .pillars-section {
      background: #f1f5f9;
      padding: 4rem 0;
    }
    .pillar-card {
      text-align: center;
      padding: 1.75rem 1.25rem;
    }
    .pillar-icon {
      font-size: 2.25rem;
      margin-bottom: 1rem;
    }
    .pillar-card h4 {
      font-size: 1.05rem;
      margin-bottom: 0.5rem;
    }
    .pillar-card p {
      font-size: 0.825rem;
      color: var(--text-muted);
      line-height: 1.5;
    }
    .text-center { text-align: center; }
    @media (max-width: 900px) {
      .hero-container { grid-template-columns: 1fr; }
      .hero-title { font-size: 2rem; }
    }
  `]
})
export class HomeComponent implements OnInit {
  authService = inject(AuthService);
  private apiService = inject(ApiService);

  elections: Election[] = [];
  loading = true;

  ngOnInit(): void {
    this.apiService.getPublicElections().subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.elections = res.data;
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
