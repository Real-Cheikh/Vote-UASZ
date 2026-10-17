import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ApiService } from '../core/services/api.service';
import { Candidacy, Election, VoteReceipt } from '../core/models/models';

@Component({
  selector: 'app-voting',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container voting-container">
      @if (loading) {
        <div class="card text-center" style="padding: 4rem;">
          <p>Initialisation sécurisée de l'isoloir électronique...</p>
        </div>
      } @else if (electionError) {
        <div class="card text-center" style="padding: 3rem;">
          <div class="alert alert-danger">{{ electionError }}</div>
          <a routerLink="/dashboard" class="btn btn-outline" style="margin-top: 1rem;">← Retour au tableau de bord</a>
        </div>
      } @else if (step === 1) {
        <!-- ÉTAPE 1 : VÉRIFICATION D'ÉLIGIBILITÉ & DÉFI OTP -->
        <div class="card voting-card">
          <div class="voting-header">
            <span class="step-indicator">Étape 1 sur 3 • Sécurité & Double Authentification</span>
            <h2>Authentification Forte pour le Vote</h2>
            <p class="subtitle">{{ election?.titre }}</p>
          </div>

          <div class="alert alert-info">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
            </svg>
            <div>
              <strong>Protocole d'Intégrité Électorale UASZ :</strong>
              Pour garantir que vous êtes l'unique titulaire de votre droit de vote, un code d'authentification à usage unique valable 5 minutes est requis.
            </div>
          </div>

          @if (otpMessage) {
            <div class="alert alert-success">{{ otpMessage }}</div>
          }
          @if (errorMessage) {
            <div class="alert alert-danger">{{ errorMessage }}</div>
          }

          @if (!otpSent) {
            <div class="otp-request-box">
              <p>Cliquez ci-dessous pour déclencher l'émission de votre code OTP.</p>
              <button (click)="requestOtp()" class="btn btn-primary btn-block" [disabled]="submitting">
                @if (submitting) {
                  <span>Génération du code...</span>
                } @else {
                  <span>Recevoir mon Code OTP de Vote</span>
                }
              </button>
            </div>
          } @else {
            <div class="otp-entry-box">
              <div class="countdown-badge">
                <span>Temps restant : <strong>{{ formattedCountdown }}</strong></span>
              </div>

              <div class="form-group" style="margin-top: 1.5rem;">
                <label class="form-label" style="text-align: center;">Entrez le code OTP à 6 chiffres</label>
                <input
                  type="text"
                  maxlength="6"
                  class="form-control otp-input"
                  [(ngModel)]="otpCode"
                  placeholder="000000"
                  pattern="[0-9]{6}"
                />
              </div>

              <button
                (click)="validateOtpAndGetToken()"
                class="btn btn-primary btn-block btn-lg"
                [disabled]="otpCode.length !== 6 || submitting"
              >
                @if (submitting) {
                  <span>Vérification & Obtention du Jeton...</span>
                } @else {
                  <span>Valider & Accéder à l'Urne Secrète →</span>
                }
              </button>
            </div>
          }
        </div>
      } @else if (step === 2) {
        <!-- ÉTAPE 2 : ISOLOIR ÉLECTRONIQUE ANONYME -->
        <div class="voting-booth">
          <div class="card booth-header">
            <div class="flex-between">
              <div>
                <span class="step-indicator">Étape 2 sur 3 • Isoloir Anonyme Scellé</span>
                <h2>Exprimez Votre Choix Électoral</h2>
                <p class="subtitle">{{ election?.titre }}</p>
              </div>
              <div class="anonymity-badge">
                <span class="shield">🛡️</span>
                <span>Session Anonymisée</span>
              </div>
            </div>
            <div class="security-banner">
              <strong>Garantie constitutionnelle :</strong> Votre jeton d'autorisation ne contient aucun identifiant personnel. Votre bulletin est strictly anonyme dès qu'il est glissé dans l'urne.
            </div>
          </div>

          @if (errorMessage) {
            <div class="alert alert-danger" style="margin: 1.5rem 0;">{{ errorMessage }}</div>
          }

          <div class="candidates-grid grid grid-cols-2">
            @for (cand of candidates; track cand.id) {
              <div
                class="card candidate-choice-card"
                [class.selected]="selectedCandidacyId === cand.id && !voteBlancSelected"
                (click)="selectCandidate(cand.id)"
              >
                <div class="candidate-pic">
                  @if (cand.photoUrl) {
                    <img [src]="cand.photoUrl" alt="Photo" />
                  } @else {
                    <div class="avatar-placeholder">{{ cand.candidat.prenom.charAt(0) }}{{ cand.candidat.nom.charAt(0) }}</div>
                  }
                </div>
                <div class="candidate-details">
                  <h3>{{ cand.candidat.nom }} {{ cand.candidat.prenom }}</h3>
                  <p class="candidate-category">{{ cand.candidat.categorie }}</p>
                  @if (cand.professionFoi) {
                    <p class="profession-foi">« {{ cand.professionFoi }} »</p>
                  }
                </div>
                <div class="radio-indicator">
                  <span class="check-circle"></span>
                </div>
              </div>
            }

            @if (election?.permettreVoteBlanc) {
              <div
                class="card candidate-choice-card blank-vote-card"
                [class.selected]="voteBlancSelected"
                (click)="selectVoteBlanc()"
              >
                <div class="blank-icon">⚪</div>
                <div class="candidate-details">
                  <h3>Vote Blanc</h3>
                  <p class="candidate-category">Option officielle de réserve</p>
                  <p class="profession-foi">Je choisis de ne soutenir aucun des candidats déclarés.</p>
                </div>
                <div class="radio-indicator">
                  <span class="check-circle"></span>
                </div>
              </div>
            }
          </div>

          <!-- Bouton de scellement du vote -->
          <div class="booth-footer card">
            <div class="summary-choice">
              <span>Votre sélection :</span>
              <strong>{{ currentSelectionLabel }}</strong>
            </div>
            <button
              (click)="castBallot()"
              class="btn btn-primary btn-lg"
              [disabled]="(!selectedCandidacyId && !voteBlancSelected) || submitting"
            >
              @if (submitting) {
                <span>Scellement dans l'urne...</span>
              } @else {
                <span>🔒 Confirmer et Déposer mon Bulletin</span>
              }
            </button>
          </div>
        </div>
      } @else if (step === 3 && receipt) {
        <!-- ÉTAPE 3 : CONFIRMATION OFFICIELLE & REÇU CRYPTOGRAPHIQUE -->
        <div class="card receipt-card">
          <div class="receipt-icon">✓</div>
          <h2>Vote Enregistré avec Succès !</h2>
          <p class="receipt-subtitle">Votre bulletin anonyme a été déposé et scellé dans l'urne électronique de l'UASZ.</p>

          <div class="receipt-details">
            <div class="receipt-row">
              <span>Scrutin :</span>
              <strong>{{ receipt.electionTitre }}</strong>
            </div>
            <div class="receipt-row">
              <span>Horodatage officiel :</span>
              <strong>{{ receipt.dateEnregistrement | date:'dd/MM/yyyy HH:mm:ss' }}</strong>
            </div>
            <div class="receipt-row hash-row">
              <span>Empreinte Numérique du Reçu (SHA-256) :</span>
              <code class="hash-code">{{ receipt.recuVoteHash }}</code>
            </div>
          </div>

          <div class="alert alert-info" style="margin: 1.5rem 0;">
            Ce reçu cryptographique est votre preuve irréfutable de participation. Il ne révèle en aucun cas le choix de votre vote mais vous permet de vérifier publiquement sa présence dans l'urne.
          </div>

          <div class="receipt-actions">
            <button class="btn btn-primary" (click)="printReceipt()">
              🖨️ Imprimer mon Reçu
            </button>
            <a [routerLink]="['/verify-receipt']" [queryParams]="{hash: receipt.recuVoteHash}" class="btn btn-outline">
              🔍 Vérifier la présence dans l'urne
            </a>
            <a routerLink="/dashboard" class="btn btn-outline">
              Retour à mon espace
            </a>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .voting-container {
      max-width: 900px;
      padding: 2.5rem 1.25rem;
    }
    .voting-card {
      padding: 3rem;
    }
    .voting-header {
      text-align: center;
      margin-bottom: 2rem;
    }
    .step-indicator {
      display: inline-block;
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: var(--secondary);
      margin-bottom: 0.5rem;
    }
    .subtitle {
      color: var(--text-muted);
      font-size: 1.05rem;
    }
    .otp-request-box, .otp-entry-box {
      text-align: center;
      padding: 2rem 0;
    }
    .otp-input {
      font-size: 2rem;
      letter-spacing: 0.5em;
      text-align: center;
      max-width: 260px;
      margin: 0 auto;
      font-family: monospace;
      font-weight: 700;
    }
    .countdown-badge {
      display: inline-block;
      background: var(--secondary-light);
      color: #854d0e;
      padding: 0.4rem 1rem;
      border-radius: 9999px;
      font-size: 0.9rem;
      border: 1px solid #fef08a;
    }
    .anonymity-badge {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      background: var(--primary-light);
      color: var(--primary);
      padding: 0.4rem 0.85rem;
      border-radius: 9999px;
      font-size: 0.8rem;
      font-weight: 700;
    }
    .security-banner {
      background: #f1f5f9;
      padding: 0.75rem 1rem;
      border-radius: var(--radius-md);
      font-size: 0.85rem;
      margin-top: 1rem;
      color: var(--text-muted);
    }
    .candidate-choice-card {
      display: flex;
      align-items: center;
      gap: 1.25rem;
      cursor: pointer;
      border: 2px solid var(--border-color);
      transition: all 0.2s ease;
    }
    .candidate-choice-card:hover {
      border-color: var(--primary);
      transform: translateY(-2px);
    }
    .candidate-choice-card.selected {
      border-color: var(--primary);
      background-color: var(--primary-light);
      box-shadow: 0 0 0 3px rgba(16, 94, 56, 0.2);
    }
    .candidate-pic img, .avatar-placeholder {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      object-fit: cover;
    }
    .avatar-placeholder {
      background: var(--primary);
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      font-size: 1.2rem;
    }
    .blank-icon {
      font-size: 2.5rem;
      padding: 0.25rem;
    }
    .candidate-details {
      flex: 1;
    }
    .candidate-category {
      font-size: 0.8rem;
      color: var(--text-muted);
      margin-bottom: 0.35rem;
    }
    .profession-foi {
      font-size: 0.825rem;
      font-style: italic;
      color: var(--text-main);
    }
    .check-circle {
      display: block;
      width: 24px;
      height: 24px;
      border-radius: 50%;
      border: 2px solid var(--border-color);
    }
    .selected .check-circle {
      border-color: var(--primary);
      background: var(--primary);
      box-shadow: inset 0 0 0 4px #ffffff;
    }
    .booth-footer {
      margin-top: 2rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #ffffff;
    }
    .receipt-card {
      text-align: center;
      padding: 3rem;
      border-top: 6px solid var(--success);
    }
    .receipt-icon {
      width: 64px;
      height: 64px;
      background: var(--success);
      color: #ffffff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 2rem;
      font-weight: 900;
      margin: 0 auto 1.5rem;
    }
    .receipt-details {
      text-align: left;
      background: #f8fafc;
      padding: 1.5rem;
      border-radius: var(--radius-md);
      margin: 2rem 0;
      border: 1px solid var(--border-color);
    }
    .receipt-row {
      display: flex;
      justify-content: space-between;
      padding: 0.5rem 0;
      border-bottom: 1px dashed var(--border-color);
      font-size: 0.9rem;
    }
    .hash-row {
      flex-direction: column;
      gap: 0.35rem;
      border-bottom: none;
    }
    .hash-code {
      background: #1e293b;
      color: #38bdf8;
      padding: 0.65rem;
      border-radius: 6px;
      font-size: 0.85rem;
      word-break: break-all;
    }
    .receipt-actions {
      display: flex;
      justify-content: center;
      gap: 1rem;
      flex-wrap: wrap;
    }
  `]
})
export class VotingComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private apiService = inject(ApiService);

  electionId!: number;
  election: Election | null = null;
  candidates: Candidacy[] = [];
  loading = true;
  submitting = false;
  electionError = '';
  errorMessage = '';
  otpMessage = '';

  step = 1; // 1: OTP, 2: Isoloir, 3: Reçu
  otpSent = false;
  otpCode = '';
  voteToken = '';

  selectedCandidacyId: number | null = null;
  voteBlancSelected = false;
  receipt: VoteReceipt | null = null;

  countdownSeconds = 300;
  countdownTimer: any = null;

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      this.electionError = 'Identifiant de scrutin invalide.';
      this.loading = false;
      return;
    }
    this.electionId = Number(idParam);
    this.loadElectionDetails();
  }

  ngOnDestroy(): void {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
    }
  }

  loadElectionDetails(): void {
    this.apiService.getPublicElectionById(this.electionId).subscribe({
      next: (res) => {
        this.election = res.data;
        if (this.election.statut !== 'VOTING_OPEN') {
          this.electionError = "Ce scrutin n'est pas ouvert au vote actuellement.";
          this.loading = false;
          return;
        }
        this.loadCandidates();
      },
      error: () => {
        this.electionError = 'Scrutin introuvable ou inaccessible.';
        this.loading = false;
      }
    });
  }

  loadCandidates(): void {
    this.apiService.getPublicCandidates(this.electionId).subscribe({
      next: (res) => {
        this.candidates = res.data || [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  requestOtp(): void {
    this.submitting = true;
    this.errorMessage = '';
    this.apiService.generateOtp(this.electionId).subscribe({
      next: (res) => {
        this.submitting = false;
        this.otpSent = true;
        this.otpMessage = `Un code de sécurité a été envoyé à ${res.data.destinationMasquee}. Il expire dans 5 minutes.`;
        this.startCountdown(res.data.validiteSecondes || 300);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err.error?.message || 'Erreur lors de la génération du code OTP.';
      }
    });
  }

  validateOtpAndGetToken(): void {
    if (this.otpCode.length !== 6) return;

    this.submitting = true;
    this.errorMessage = '';

    this.apiService.requestVoteToken(this.electionId, this.otpCode).subscribe({
      next: (res) => {
        this.submitting = false;
        this.voteToken = res.data.voteToken;
        this.step = 2; // Passage à l'isoloir anonyme
        if (this.countdownTimer) clearInterval(this.countdownTimer);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err.error?.message || 'Code OTP invalide ou expiré.';
      }
    });
  }

  selectCandidate(id: number): void {
    this.selectedCandidacyId = id;
    this.voteBlancSelected = false;
  }

  selectVoteBlanc(): void {
    this.selectedCandidacyId = null;
    this.voteBlancSelected = true;
  }

  get currentSelectionLabel(): string {
    if (this.voteBlancSelected) return 'Vote Blanc';
    if (this.selectedCandidacyId) {
      const cand = this.candidates.find(c => c.id === this.selectedCandidacyId);
      return cand ? `${cand.candidat.nom} ${cand.candidat.prenom}` : 'Candidat sélectionné';
    }
    return 'Aucun choix sélectionné';
  }

  castBallot(): void {
    if (!this.selectedCandidacyId && !this.voteBlancSelected) return;

    this.submitting = true;
    this.errorMessage = '';

    const payload = {
      electionId: this.electionId,
      voteToken: this.voteToken,
      candidacyId: this.selectedCandidacyId || undefined,
      estBlanc: this.voteBlancSelected
    };

    this.apiService.castBallot(payload).subscribe({
      next: (res) => {
        this.submitting = false;
        this.receipt = res.data;
        this.step = 3; // Confirmation
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err.error?.message || "Échec de l'enregistrement du bulletin.";
      }
    });
  }

  startCountdown(seconds: number): void {
    this.countdownSeconds = seconds;
    if (this.countdownTimer) clearInterval(this.countdownTimer);

    this.countdownTimer = setInterval(() => {
      this.countdownSeconds--;
      if (this.countdownSeconds <= 0) {
        clearInterval(this.countdownTimer);
        this.errorMessage = 'Le code OTP a expiré. Veuillez en redemander un nouveau.';
        this.otpSent = false;
      }
    }, 1000);
  }

  get formattedCountdown(): string {
    const mins = Math.floor(this.countdownSeconds / 60);
    const secs = this.countdownSeconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  }

  printReceipt(): void {
    window.print();
  }
}
