import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ApiService } from '../core/services/api.service';
import { ReceiptVerification } from '../core/models/models';

@Component({
  selector: 'app-receipt-check',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container verify-container">
      <div class="card verify-card">
        <div class="verify-header">
          <div class="badge badge-secondary">Audit Public & Transparence</div>
          <h2>Vérification Publique d'un Reçu de Vote</h2>
          <p class="subtitle">
            Attestez en toute indépendance de la présence de votre bulletin dans l'urne électronique scellée de l'UASZ.
          </p>
        </div>

        <form (ngSubmit)="checkReceipt()" class="verify-form">
          <div class="form-group">
            <label class="form-label">Empreinte Cryptographique du Reçu (SHA-256)</label>
            <input
              type="text"
              class="form-control code-input"
              [(ngModel)]="receiptHash"
              name="receiptHash"
              placeholder="Ex: 8f4b23... (chaîne hexadécimale de 64 caractères)"
              required
            />
          </div>

          <button type="submit" class="btn btn-primary btn-block btn-lg" [disabled]="!receiptHash || loading">
            @if (loading) {
              <span>Interrogation du registre de l'urne...</span>
            } @else {
              <span>🔍 Vérifier la Validité du Reçu</span>
            }
          </button>
        </form>

        @if (result) {
          <div class="result-box" [class.valid]="result.valide" [class.invalid]="!result.valide">
            @if (result.valide) {
              <div class="result-header">
                <span class="icon-valid">✓</span>
                <div>
                  <h3>Bulletin Officiellement Scellé dans l'Urne</h3>
                  <p class="result-status">Empreinte numérique certifiée conforme et authentifiée.</p>
                </div>
              </div>

              <div class="result-details">
                <div><strong>Scrutin :</strong> {{ result.electionTitre }}</div>
                <div><strong>Date & Heure de Dépôt :</strong> {{ result.dateEnregistrement | date:'dd/MM/yyyy HH:mm:ss' }}</div>
                <div><strong>Statut :</strong> Présent dans l'urne, inaltéré</div>
              </div>
            } @else {
              <div class="result-header">
                <span class="icon-invalid">✕</span>
                <div>
                  <h3>Empreinte Non Reconnue</h3>
                  <p class="result-status">Aucun bulletin correspondant à cette empreinte n'a été trouvé.</p>
                </div>
              </div>
            }
          </div>
        }

        <div class="alert alert-info" style="margin-top: 2rem;">
          <strong>Principe du Secret du Vote :</strong>
          La vérification atteste de la prise en compte effective du suffrage, sans jamais exposer le candidat ou le choix exprimé.
        </div>
      </div>
    </div>
  `,
  styles: [`
    .verify-container {
      max-width: 800px;
      padding: 3rem 1.25rem;
    }
    .verify-card {
      padding: 3rem;
    }
    .verify-header {
      text-align: center;
      margin-bottom: 2rem;
    }
    .subtitle {
      color: var(--text-muted);
      font-size: 0.95rem;
      margin-top: 0.5rem;
    }
    .code-input {
      font-family: monospace;
      font-size: 0.95rem;
      padding: 0.85rem;
    }
    .btn-block {
      width: 100%;
    }
    .result-box {
      margin-top: 2rem;
      padding: 1.75rem;
      border-radius: var(--radius-md);
      border: 1px solid var(--border-color);
    }
    .result-box.valid {
      background: var(--success-light);
      border-color: #86efac;
    }
    .result-box.invalid {
      background: var(--danger-light);
      border-color: #fca5a5;
    }
    .result-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1rem;
    }
    .icon-valid {
      width: 44px;
      height: 44px;
      background: var(--success);
      color: #ffffff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.5rem;
      font-weight: 900;
    }
    .icon-invalid {
      width: 44px;
      height: 44px;
      background: var(--danger);
      color: #ffffff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.5rem;
      font-weight: 900;
    }
    .result-status {
      font-size: 0.85rem;
      color: var(--text-muted);
    }
    .result-details {
      background: rgba(255, 255, 255, 0.7);
      padding: 1rem;
      border-radius: var(--radius-sm);
      font-size: 0.9rem;
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }
  `]
})
export class ReceiptCheckComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private apiService = inject(ApiService);

  receiptHash = '';
  loading = false;
  result: ReceiptVerification | null = null;

  ngOnInit(): void {
    const hash = this.route.snapshot.queryParamMap.get('hash');
    if (hash) {
      this.receiptHash = hash;
      this.checkReceipt();
    }
  }

  checkReceipt(): void {
    if (!this.receiptHash) return;

    this.loading = true;
    this.result = null;

    this.apiService.verifyReceipt(this.receiptHash.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        this.result = res.data;
      },
      error: () => {
        this.loading = false;
        this.result = {
          valide: false,
          recuVoteHash: this.receiptHash,
          message: "Erreur lors de l'interrogation du registre de l'urne."
        };
      }
    });
  }
}
