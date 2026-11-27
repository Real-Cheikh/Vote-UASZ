import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { LoginComponent } from './login.component';
import { AuthService } from '../core/services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let mockAuthService: any;

  beforeEach(async () => {
    mockAuthService = {
      login: vi.fn(),
      register: vi.fn(),
      isCommission: vi.fn().mockReturnValue(false)
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait initialiser les signaux par défaut (onglet login, loading false)', () => {
    expect(component.activeTab()).toBe('login');
    expect(component.loading()).toBe(false);
    expect(component.regLoading()).toBe(false);
    expect(component.errorMessage()).toBe('');
    expect(component.successMessage()).toBe('');
  });

  it('devrait basculer proprement entre les onglets', () => {
    component.switchTab('register');
    expect(component.activeTab()).toBe('register');
    component.switchTab('login');
    expect(component.activeTab()).toBe('login');
  });

  it('devrait rejeter une inscription avec un email hors domaine @zig.univ.sn', () => {
    component.regPrenom = 'Cheikh';
    component.regNom = 'Diop';
    component.regEmail = 'cheikh@gmail.com';
    component.regNumeroId = 'ETU-2026-999';
    component.regPassword = 'MotDePasse2026!';
    component.regPasswordConfirm = 'MotDePasse2026!';

    component.onSubmitRegister();

    expect(component.errorMessage()).toContain('@zig.univ.sn');
    expect(component.regLoading()).toBe(false);
    expect(mockAuthService.register).not.toHaveBeenCalled();
  });

  it('devrait rejeter une inscription si les mots de passe ne correspondent pas', () => {
    component.regPrenom = 'Cheikh';
    component.regNom = 'Diop';
    component.regEmail = 'c.diop@zig.univ.sn';
    component.regNumeroId = 'ETU-2026-999';
    component.regPassword = 'MotDePasse2026!';
    component.regPasswordConfirm = 'Different2026!';

    component.onSubmitRegister();

    expect(component.errorMessage()).toContain('pas identiques');
    expect(component.regLoading()).toBe(false);
    expect(mockAuthService.register).not.toHaveBeenCalled();
  });

  it('devrait désactiver le chargement et basculer sur login en cas de succès d\'inscription', () => {
    mockAuthService.register.mockReturnValue(of({
      success: true,
      message: 'Compte créé',
      data: { id: 42, email: 'c.diop@zig.univ.sn' }
    }));

    component.regPrenom = 'Cheikh';
    component.regNom = 'Diop';
    component.regEmail = 'c.diop@zig.univ.sn';
    component.regNumeroId = 'ETU-2026-999';
    component.regPassword = 'MotDePasse2026!';
    component.regPasswordConfirm = 'MotDePasse2026!';

    component.onSubmitRegister();

    expect(mockAuthService.register).toHaveBeenCalledTimes(1);
    expect(component.regLoading()).toBe(false);
    expect(component.activeTab()).toBe('login');
    expect(component.successMessage()).toContain('Compte');
    expect(component.email).toBe('c.diop@zig.univ.sn');
    expect(component.regNom).toBe('');
  });

  it('devrait désactiver le chargement et afficher le message d\'erreur en cas d\'échec 409', () => {
    mockAuthService.register.mockReturnValue(throwError(() => ({
      status: 409,
      error: { message: 'Un compte existe déjà avec cette adresse email.' }
    })));

    component.regPrenom = 'Cheikh';
    component.regNom = 'Diop';
    component.regEmail = 'c.diop@zig.univ.sn';
    component.regNumeroId = 'ETU-2026-999';
    component.regPassword = 'MotDePasse2026!';
    component.regPasswordConfirm = 'MotDePasse2026!';

    component.onSubmitRegister();

    expect(component.regLoading()).toBe(false);
    expect(component.errorMessage()).toContain('Un compte existe déjà');
  });

  it('devrait pré-remplir les identifiants avec la méthode fillDemo', () => {
    component.fillDemo('admin.electoral@uasz.sn', 'AdminPass123!');
    expect(component.email).toBe('admin.electoral@uasz.sn');
    expect(component.password).toBe('AdminPass123!');
    expect(component.activeTab()).toBe('login');
  });
});
