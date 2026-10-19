import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ApiResponse, LoginResponse, User } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'voteuasz_token';
  private readonly USER_KEY = 'voteuasz_user';

  // Signals réactifs Angular avec validation de validité temporelle du JWT
  private tokenSignal = signal<string | null>(this.getInitialToken());
  private userSignal = signal<User | null>(this.getStoredUser());

  public token = this.tokenSignal.asReadonly();
  public currentUser = this.userSignal.asReadonly();
  public isAuthenticated = computed(() => {
    const t = this.tokenSignal();
    return !!t && !this.isTokenExpired(t);
  });

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

  register(data: {
    email: string;
    password: string;
    nom: string;
    prenom: string;
    numeroIdentification: string;
    telephone?: string;
  }): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(`${this.apiUrl}/register`, data);
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

  private getInitialToken(): string | null {
    if (typeof window === 'undefined' || !window.localStorage) return null;
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token || this.isTokenExpired(token)) {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.USER_KEY);
      return null;
    }
    return token;
  }

  public isTokenExpired(token: string | null): boolean {
    if (!token) return true;
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return true;
      const payload = JSON.parse(atob(parts[1]));
      if (!payload.exp) return false;
      return payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }

  private getStoredUser(): User | null {
    if (typeof window === 'undefined' || !window.localStorage) return null;
    const data = localStorage.getItem(this.USER_KEY);
    if (!data) return null;
    try {
      return JSON.parse(data);
    } catch {
      return null;
    }
  }
}
