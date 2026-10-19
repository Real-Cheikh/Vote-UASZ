import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ApiService } from '../core/services/api.service';
import { ReceiptVerification } from '../core/models/models';

type VerificationState = 'idle' | 'valid' | 'notFound' | 'networkError' | 'invalidFormat';

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
            Attestez en toute indépendance de la présence et de l'intégrité de votre bulletin dans l'urne électronique scellée de l'UASZ.
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
              (input)="onInputChange()"
            />
            <small class="field-hint">Le code doit comporter exactement 64 caractères hexadécimaux (0-9, a-f).</small>
          </div>

          <button type="submit" class="btn btn-primary btn-block btn-lg" [disabled]="!receiptHash || loading">
            @if (loading) {
              <span>Interrogation du registre de l'urne...</span>
            } @else {
              <span>🔍 Vérifier la Présence du Bulletin</span>
            }
          </button>
        </form>

        <!-- ÉTAT 1 : FORMAT NON CONFORME -->
        @if (status === 'invalidFormat') {
          <div class="result-box invalid-format">
            <div class="result-header">
              <span class="icon-warning">⚠️</span>
              <div>
                <h3>Format d'Empreinte Non Conforme</h3>
                <p class="result-status">
                  L'empreinte saisie comporte {{ receiptHash.trim().length }} caractères au lieu de 64 caractères hexadécimaux (SHA-256).
                </p>
              </div>
            </div>
          </div>
        }

        <!-- ÉTAT 2 : RÉCÉPISSÉ VALIDE ET INTÈGRE -->
        @if (status === 'valid' && result) {
          <div class="result-box valid">
            <div class="result-header">
              <span class="icon-valid">✓</span>
              <div>
                <h3>Bulletin Officiellement Scellé dans l'Urne</h3>
                <p class="result-status">Empreinte numérique certifiée conforme, authentifiée et inaltérée.</p>
              </div>
            </div>

            <div class="result-details">
              <div><strong>Scrutin Électoral :</strong> {{ result.electionTitre }}</div>
              <div><strong>Date & Heure d'Enregistrement :</strong> {{ result.dateEnregistrement | date:'dd/MM/yyyy HH:mm:ss' }}</div>
              <div><strong>Empreinte SHA-256 :</strong> <code class="hash-display">{{ result.recuVoteHash }}</code></div>
              <div><strong>Statut de l'Urne :</strong> <span class="badge badge-success">Présent & Verrouillé Cryptographiquement</span></div>
            </div>
          </div>
        }

        <!-- ÉTAT 3 : EMPREINTE NON TROUVÉE -->
        @if (status === 'notFound') {
          <div class="result-box not-found">
            <div class="result-header">
              <span class="icon-invalid">✕</span>
              <div>
                <h3>Empreinte Non Reconnue dans l'Urne</h3>
                <p class="result-status">
                  Aucun bulletin correspondant à cette empreinte de reçu n'a été enregistré pour les scrutins audités.
                </p>
              </div>
            </div>
            <p class="not-found-help">
              Vérifiez que vous avez copié l'intégralité du code de reçu délivré lors de la confirmation de votre vote.
            </p>
          </div>
        }

        <!-- ÉTAT 4 : ERREUR RÉSEAU / BACKEND INDISPONIBLE -->
        @if (status === 'networkError') {
          <div class="result-box network-error">
            <div class="result-header">
              <span class="icon-network">⚡</span>
              <div>
                <h3>Service de Vérification Temporairement Indisponible</h3>
                <p class="result-status">{{ networkErrorMessage }}</p>
              </div>
            </div>
            <div class="retry-box">
              <button type="button" class="btn btn-outline btn-sm" (click)="checkReceipt()">
                🔄 Réessayer la vérification
              </button>
            </div>
          </div>
        }

        <div class="alert alert-info" style="margin-top: 2rem;">
          <strong>Principe Constitutionnel du Secret du Vote :</strong>
          La vérification atteste publiquement de la prise en compte effective du suffrage dans l'urne électronique, sans jamais exposer l'identité de l'électeur ni le candidat choisi.
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
    .field-hint {
      display: block;
      color: var(--text-muted);
      font-size: 0.75rem;
      margin-top: 0.35rem;
    }
    .btn-block {
      width: 100%;
    }
    .result-box {
      margin-top: 2rem;
      padding: 1.75rem;
      border-radius: var(--radius-md);
      border: 1px solid var(--border-color);
      animation: fadeIn 0.3s ease;
    }
    .result-box.valid {
      background: var(--success-light);
      border-color: #86efac;
    }
    .result-box.not-found {
      background: #fef2f2;
      border-color: #fca5a5;
    }
    .result-box.invalid-format {
      background: #fffbeb;
      border-color: #fcd34d;
    }
    .result-box.network-error {
      background: #f8fafc;
      border-color: #cbd5e1;
    }
    .result-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1rem;
    }
    .icon-valid, .icon-invalid, .icon-warning, .icon-network {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.5rem;
      font-weight: 900;
      flex-shrink: 0;
    }
    .icon-valid {
      background: var(--success);
      color: #ffffff;
    }
    .icon-invalid {
      background: var(--danger);
      color: #ffffff;
    }
    .icon-warning {
      background: #d97706;
      color: #ffffff;
    }
    .icon-network {
      background: #475569;
      color: #ffffff;
    }
    .result-status {
      font-size: 0.85rem;
      color: var(--text-muted);
    }
    .result-details {
      background: rgba(255, 255, 255, 0.85);
      padding: 1rem 1.25rem;
      border-radius: var(--radius-sm);
      font-size: 0.9rem;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }
    .hash-display {
      background: #1e293b;
      color: #38bdf8;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-size: 0.8rem;
      word-break: break-all;
    }
    .not-found-help {
      font-size: 0.85rem;
      color: #991b1b;
      margin-top: 0.5rem;
    }
    .retry-box {
      margin-top: 0.75rem;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(6px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class ReceiptCheckComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private apiService = inject(ApiService);

  receiptHash = '';
  loading = false;
  status: VerificationState = 'idle';
  result: ReceiptVerification | null = null;
  networkErrorMessage = '';

  private readonly HASH_REGEX = /^[a-fA-F0-9]{64}$/;

  ngOnInit(): void {
    const hash = this.route.snapshot.queryParamMap.get('hash');
    if (hash) {
      this.receiptHash = hash;
      this.checkReceipt();
    }
  }

  onInputChange(): void {
    if (this.status !== 'idle') {
      this.status = 'idle';
      this.result = null;
    }
  }

  checkReceipt(): void {
    if (!this.receiptHash) return;

    const cleanHash = this.receiptHash.trim();

    // Validation préalable du format SHA-256 (64 hex)
    if (!this.HASH_REGEX.test(cleanHash)) {
      this.status = 'invalidFormat';
      this.result = null;
      return;
    }

    this.loading = true;
    this.status = 'idle';
    this.result = null;
    this.networkErrorMessage = '';

    this.apiService.verifyReceipt(cleanHash).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.data && res.data.valide) {
          this.status = 'valid';
          this.result = res.data;
        } else {
          this.status = 'notFound';
          this.result = res.data || null;
        }
      },
      error: (err) => {
        this.loading = false;
        this.status = 'networkError';
        this.networkErrorMessage = err.status === 0
          ? "Impossible de contacter le serveur de vote UASZ (connexion réseau ou serveur indisponible)."
          : (err.error?.message || "Erreur de communication lors de l'interrogation de l'urne.");
      }
    });
  }
}
