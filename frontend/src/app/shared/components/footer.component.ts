import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterModule],
  template: `
    <footer class="footer">
      <div class="container footer-container">
        <div class="footer-grid">
          <div class="footer-col">
            <div class="footer-logo">
              <span class="logo-box">UASZ</span>
              <span class="logo-title">VoteUASZ</span>
            </div>
            <p class="footer-desc">
              Système officiel de vote électronique de l'Université Assane Seck de Ziguinchor (UASZ).
              Garant de la transparence, du secret de l'urne et de l'intégrité démocratique.
            </p>
            <div class="legal-badge">
              <span>Projet Académique & Institutionnel — République du Sénégal</span>
            </div>
          </div>

          <div class="footer-col">
            <h4>Accès Rapide</h4>
            <ul class="footer-links">
              <li><a routerLink="/elections">Scrutins Ouverts</a></li>
              <li><a routerLink="/candidacies">Candidats Officiels</a></li>
              <li><a routerLink="/results">Résultats en Direct</a></li>
              <li><a routerLink="/verify-receipt">Vérification de Reçu</a></li>
            </ul>
          </div>

          <div class="footer-col">
            <h4>Assistance Électorale</h4>
            <ul class="footer-contact">
              <li><strong>Localisation :</strong> Campus de Diabir, Ziguinchor, Sénégal</li>
              <li><strong>Commission :</strong> commission.electorale&#64;uasz.sn</li>
              <li><strong>DSI Support :</strong> dsi&#64;uasz.sn</li>
              <li><strong>Téléphone :</strong> +221 33 991 00 00</li>
            </ul>
          </div>
        </div>

        <div class="footer-bottom">
          <p>© {{ currentYear }} Université Assane Seck de Ziguinchor (UASZ). Tous droits réservés.</p>
          <div class="footer-bottom-links">
            <a routerLink="/verify-receipt">Vérifier mon vote</a>
            <span class="sep">•</span>
            <a routerLink="/complaints">Contentieux</a>
          </div>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    .footer {
      background-color: var(--dark);
      color: #cbd5e1;
      padding: 3.5rem 0 1.5rem;
      margin-top: 4rem;
      border-top: 4px solid var(--secondary);
    }
    .footer-grid {
      display: grid;
      grid-template-columns: 2fr 1fr 1.5fr;
      gap: 3rem;
      margin-bottom: 2.5rem;
    }
    .footer-logo {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .logo-box {
      background: var(--secondary);
      color: var(--dark);
      font-weight: 900;
      padding: 0.35rem 0.65rem;
      border-radius: 6px;
      font-size: 0.85rem;
    }
    .logo-title {
      font-size: 1.25rem;
      font-weight: 800;
      color: #ffffff;
    }
    .footer-desc {
      font-size: 0.875rem;
      line-height: 1.6;
      margin-bottom: 1rem;
      color: #94a3b8;
    }
    .legal-badge {
      display: inline-block;
      font-size: 0.75rem;
      background: rgba(201, 162, 39, 0.15);
      color: var(--secondary);
      padding: 0.35rem 0.75rem;
      border-radius: 9999px;
      border: 1px solid rgba(201, 162, 39, 0.3);
    }
    .footer-col h4 {
      color: #ffffff;
      font-size: 1.05rem;
      margin-bottom: 1.25rem;
      position: relative;
    }
    .footer-col h4::after {
      content: '';
      position: absolute;
      bottom: -6px;
      left: 0;
      width: 30px;
      height: 2px;
      background: var(--secondary);
    }
    .footer-links, .footer-contact {
      list-style: none;
      padding: 0;
      font-size: 0.875rem;
    }
    .footer-links li, .footer-contact li {
      margin-bottom: 0.65rem;
    }
    .footer-links a {
      color: #94a3b8;
      text-decoration: none;
      transition: color 0.2s ease;
    }
    .footer-links a:hover {
      color: var(--secondary);
    }
    .footer-contact li {
      color: #94a3b8;
      line-height: 1.5;
    }
    .footer-contact strong {
      color: #e2e8f0;
    }
    .footer-bottom {
      border-top: 1px solid #334155;
      padding-top: 1.5rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 0.8rem;
      color: #64748b;
    }
    .footer-bottom-links {
      display: flex;
      gap: 0.75rem;
      align-items: center;
    }
    .footer-bottom-links a {
      color: #94a3b8;
    }
    .sep { color: #475569; }
    @media (max-width: 768px) {
      .footer-grid { grid-template-columns: 1fr; gap: 2rem; }
      .footer-bottom { flex-direction: column; gap: 0.75rem; text-align: center; }
    }
  `]
})
export class FooterComponent {
  currentYear = new Date().getFullYear();
}
