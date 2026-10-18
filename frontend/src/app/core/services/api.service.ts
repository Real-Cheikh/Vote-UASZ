import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, shareReplay, tap } from 'rxjs';
import {
  ApiResponse,
  AuditEvent,
  Candidacy,
  CandidateDocument,
  College,
  Complaint,
  Election,
  ElectionResult,
  Eligibility,
  OtpChallengeResponse,
  ReceiptVerification,
  User,
  VoteAuthorizationResponse,
  VoteReceipt
} from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = '/api/v1';

  private publicElectionsCache$: Observable<ApiResponse<Election[]>> | null = null;
  private publicElectionsCacheTime = 0;
  private readonly CACHE_TTL_MS = 30000; // 30 secondes de cache en mémoire

  constructor(private http: HttpClient) {}

  // --- Élections ---
  getPublicElections(forceReload: boolean = false): Observable<ApiResponse<Election[]>> {
    const now = Date.now();
    if (!forceReload && this.publicElectionsCache$ && (now - this.publicElectionsCacheTime < this.CACHE_TTL_MS)) {
      return this.publicElectionsCache$;
    }

    this.publicElectionsCacheTime = now;
    this.publicElectionsCache$ = this.http.get<ApiResponse<Election[]>>(`${this.baseUrl}/elections/public`).pipe(
      shareReplay({ bufferSize: 1, refCount: false })
    );

    return this.publicElectionsCache$;
  }

  invalidateElectionsCache(): void {
    this.publicElectionsCache$ = null;
    this.publicElectionsCacheTime = 0;
  }

  getPublicElectionById(id: number): Observable<ApiResponse<Election>> {
    return this.http.get<ApiResponse<Election>>(`${this.baseUrl}/elections/public/${id}`);
  }

  getAllElections(): Observable<ApiResponse<Election[]>> {
    return this.http.get<ApiResponse<Election[]>>(`${this.baseUrl}/elections`);
  }

  getElectionById(id: number): Observable<ApiResponse<Election>> {
    return this.http.get<ApiResponse<Election>>(`${this.baseUrl}/elections/${id}`);
  }

  createElection(election: any): Observable<ApiResponse<Election>> {
    return this.http.post<ApiResponse<Election>>(`${this.baseUrl}/elections/manage`, election).pipe(
      tap(() => this.invalidateElectionsCache())
    );
  }

  updateElectionStatus(id: number, statut: string, motif?: string): Observable<ApiResponse<Election>> {
    return this.http.patch<ApiResponse<Election>>(`${this.baseUrl}/elections/manage/${id}/status`, { statut, motif }).pipe(
      tap(() => this.invalidateElectionsCache())
    );
  }

  // --- Collèges Électoraux ---
  getColleges(): Observable<ApiResponse<College[]>> {
    return this.http.get<ApiResponse<College[]>>(`${this.baseUrl}/colleges`);
  }

  // --- Candidatures ---
  getAllPublicCandidates(): Observable<ApiResponse<Candidacy[]>> {
    return this.http.get<ApiResponse<Candidacy[]>>(`${this.baseUrl}/candidacies/public`);
  }

  getPublicCandidates(electionId: number): Observable<ApiResponse<Candidacy[]>> {
    return this.http.get<ApiResponse<Candidacy[]>>(`${this.baseUrl}/candidacies/public/election/${electionId}`);
  }

  getAllCandidatesByElection(electionId: number): Observable<ApiResponse<Candidacy[]>> {
    return this.http.get<ApiResponse<Candidacy[]>>(`${this.baseUrl}/candidacies/election/${electionId}`);
  }

  getMyCandidacies(): Observable<ApiResponse<Candidacy[]>> {
    return this.http.get<ApiResponse<Candidacy[]>>(`${this.baseUrl}/candidacies/my`);
  }

  getMyCandidacyForElection(electionId: number): Observable<ApiResponse<Candidacy>> {
    return this.http.get<ApiResponse<Candidacy>>(`${this.baseUrl}/candidacies/my/election/${electionId}`);
  }

  applyCandidacy(dto: { electionId: number; professionFoi: string; biographie?: string }): Observable<ApiResponse<Candidacy>> {
    return this.http.post<ApiResponse<Candidacy>>(`${this.baseUrl}/candidacies/apply`, dto);
  }

  applyCandidacyMultipart(formData: FormData): Observable<ApiResponse<Candidacy>> {
    return this.http.post<ApiResponse<Candidacy>>(`${this.baseUrl}/candidacies/apply`, formData);
  }

  uploadCandidacyDocument(candidacyId: number, type: string, file: File): Observable<ApiResponse<CandidateDocument>> {
    const formData = new FormData();
    formData.append('type', type);
    formData.append('file', file);
    return this.http.post<ApiResponse<CandidateDocument>>(`${this.baseUrl}/candidacies/${candidacyId}/documents`, formData);
  }

  reviewCandidacy(candidacyId: number, decision: 'VALIDATED' | 'REJECTED', motif?: string): Observable<ApiResponse<Candidacy>> {
    return this.http.patch<ApiResponse<Candidacy>>(`${this.baseUrl}/candidacies/review/${candidacyId}`, { decision, motif });
  }

  // --- Éligibilité ---
  getMyEligibleElections(): Observable<ApiResponse<Eligibility[]>> {
    return this.http.get<ApiResponse<Eligibility[]>>(`${this.baseUrl}/eligibility/my-elections`);
  }

  checkEligibility(electionId: number): Observable<ApiResponse<Eligibility>> {
    return this.http.get<ApiResponse<Eligibility>>(`${this.baseUrl}/eligibility/check/${electionId}`);
  }

  // --- OTP ---
  generateOtp(electionId: number): Observable<ApiResponse<OtpChallengeResponse>> {
    return this.http.post<ApiResponse<OtpChallengeResponse>>(`${this.baseUrl}/otp/generate`, { electionId });
  }

  verifyOtp(electionId: number, code: string): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.baseUrl}/otp/verify`, { electionId, code });
  }

  // --- Vote Électronique ---
  requestVoteToken(electionId: number, otpCode: string): Observable<ApiResponse<VoteAuthorizationResponse>> {
    return this.http.post<ApiResponse<VoteAuthorizationResponse>>(`${this.baseUrl}/voting/request-token`, { electionId, otpCode });
  }

  castBallot(dto: { electionId: number; voteToken: string; candidacyId?: number; estBlanc: boolean }): Observable<ApiResponse<VoteReceipt>> {
    return this.http.post<ApiResponse<VoteReceipt>>(`${this.baseUrl}/voting/cast-ballot`, dto);
  }

  verifyReceipt(receiptHash: string): Observable<ApiResponse<ReceiptVerification>> {
    return this.http.get<ApiResponse<ReceiptVerification>>(`${this.baseUrl}/voting/verify-receipt/${encodeURIComponent(receiptHash)}`);
  }

  // --- Résultats & Dépouillement ---
  getPublicResults(electionId: number): Observable<ApiResponse<ElectionResult>> {
    return this.http.get<ApiResponse<ElectionResult>>(`${this.baseUrl}/results/public/${electionId}`);
  }

  countVotes(electionId: number): Observable<ApiResponse<ElectionResult>> {
    return this.http.post<ApiResponse<ElectionResult>>(`${this.baseUrl}/results/count/${electionId}`, {}).pipe(
      tap(() => this.invalidateElectionsCache())
    );
  }

  publishResults(electionId: number): Observable<ApiResponse<ElectionResult>> {
    return this.http.post<ApiResponse<ElectionResult>>(`${this.baseUrl}/results/publish/${electionId}`, {}).pipe(
      tap(() => this.invalidateElectionsCache())
    );
  }

  getCommissionResults(electionId: number): Observable<ApiResponse<ElectionResult>> {
    return this.http.get<ApiResponse<ElectionResult>>(`${this.baseUrl}/results/election/${electionId}`);
  }

  // --- Procès-Verbaux PDF ---
  downloadReportPdf(electionId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/reports/election/${electionId}/pdf`, { responseType: 'blob' });
  }

  verifyReport(reportNumber: string): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/reports/verify/${reportNumber}`);
  }

  // --- Réclamations ---
  getComplaintsByElection(electionId: number): Observable<ApiResponse<Complaint[]>> {
    return this.http.get<ApiResponse<Complaint[]>>(`${this.baseUrl}/complaints/election/${electionId}`);
  }

  getMyComplaints(): Observable<ApiResponse<Complaint[]>> {
    return this.http.get<ApiResponse<Complaint[]>>(`${this.baseUrl}/complaints/my`);
  }

  submitComplaint(dto: { electionId: number; objet: string; description: string; piecesJointesUrls?: string }): Observable<ApiResponse<Complaint>> {
    return this.http.post<ApiResponse<Complaint>>(`${this.baseUrl}/complaints`, dto);
  }

  resolveComplaint(id: number, statut: string, reponseCommission: string): Observable<ApiResponse<Complaint>> {
    return this.http.patch<ApiResponse<Complaint>>(`${this.baseUrl}/complaints/${id}/resolve`, { statut, reponseCommission });
  }

  // --- Audit ---
  getAuditEvents(page = 0, size = 20): Observable<ApiResponse<any>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/audit/events`, { params });
  }

  // --- Utilisateurs ---
  getAllUsers(): Observable<ApiResponse<User[]>> {
    return this.http.get<ApiResponse<User[]>>(`${this.baseUrl}/users`);
  }

  createUser(user: any): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(`${this.baseUrl}/users`, user);
  }

  toggleUserStatus(id: number, actif: boolean, bloque: boolean): Observable<ApiResponse<User>> {
    const params = new HttpParams().set('actif', actif).set('bloque', bloque);
    return this.http.patch<ApiResponse<User>>(`${this.baseUrl}/users/${id}/status`, {}, { params });
  }
}
