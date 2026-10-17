import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ApiResponse, LoginResponse, User } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = '/api/v1/auth';
  private readonly TOKEN_KEY = 'voteuasz_token';
  private readonly USER_KEY = 'voteuasz_user';

  // Signals réactifs Angular
  private tokenSignal = signal<string | null>(localStorage.getItem(this.TOKEN_KEY));
  private userSignal = signal<User | null>(this.getStoredUser());

  public token = this.tokenSignal.asReadonly();
  public currentUser = this.userSignal.asReadonly();
  public isAuthenticated = computed(() => !!this.tokenSignal());

  public isSuperAdmin = computed(() => this.hasRole('ROLE_SUPER_ADMIN_ELECTORAL'));
  public isCommission = computed(() => this.hasRole('ROLE_COMMISSION_ELECTORALE') || this.isSuperAdmin());
  public isCandidat = computed(() => this.hasRole('ROLE_CANDIDAT'));
  public isElecteur = computed(() => this.hasRole('ROLE_ELECTEUR'));

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap((res) => {
        if (res.success && res.data) {
          this.setSession(res.data);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
  }

  public hasRole(role: string): boolean {
    const user = this.userSignal();
    return !!user && user.roles.includes(role);
  }

  public getToken(): string | null {
    return this.tokenSignal();
  }

  private setSession(authData: LoginResponse): void {
    localStorage.setItem(this.TOKEN_KEY, authData.token);
    const userObj: User = {
      id: authData.userId,
      email: authData.email,
      nom: authData.nom,
      prenom: authData.prenom,
      numeroIdentification: '',
      categorie: (authData.categorie as any) || 'ETUDIANT',
      actif: true,
      bloque: false,
      roles: authData.roles
    };
    localStorage.setItem(this.USER_KEY, JSON.stringify(userObj));
    this.tokenSignal.set(authData.token);
    this.userSignal.set(userObj);
  }

  private getStoredUser(): User | null {
    const data = localStorage.getItem(this.USER_KEY);
    if (!data) return null;
    try {
      return JSON.parse(data);
    } catch {
      return null;
    }
  }
}
