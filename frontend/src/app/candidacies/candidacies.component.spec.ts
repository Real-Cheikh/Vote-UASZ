import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CandidaciesComponent } from './candidacies.component';
import { ApiService } from '../core/services/api.service';
import { AuthService } from '../core/services/auth.service';
import { Candidacy, Election, User } from '../core/models/models';

describe('CandidaciesComponent', () => {
  let component: CandidaciesComponent;
  let fixture: ComponentFixture<CandidaciesComponent>;
  let mockApiService: any;
  let mockAuthService: any;

  const mockUser: User = {
    id: 10,
    email: 'candidat1@zig.univ.sn',
    prenom: 'Awa',
    nom: 'Sow',
    numeroIdentification: 'ETU-2026-001',
    categorie: 'ETUDIANT',
    actif: true,
    bloque: false,
    roles: ['ROLE_CANDIDAT', 'ROLE_ELECTEUR']
  };

  const mockElection: Election = {
    id: 1,
    titre: 'Élection des Délégués UFR Sciences et Technologies 2026',
    description: 'Scrutin officiel',
    statut: 'CANDIDATURES_OPEN',
    type: 'DELEGUE',
    college: { id: 1, code: 'ETUDIANT', nom: 'Collège Étudiants', categorie: 'ETUDIANT' },
    dateOuverture: '2026-11-20T08:00:00Z',
    dateCloture: '2026-12-15T18:00:00Z',
    permettreVoteBlanc: true,
    dateCreation: '2026-11-19T10:00:00Z'
  };

  const mockCandidacy: Candidacy = {
    id: 1,
    electionId: 1,
    electionTitre: 'Élection des Délégués UFR Sciences et Technologies 2026',
    collegeNom: 'Collège Étudiants',
    candidat: mockUser,
    professionFoi: 'Agir ensemble pour une université moderne et solidaire.',
    biographie: 'Étudiante en Master Informatique, déléguée de promotion.',
    statut: 'VALIDATED',
    dateSoumission: '2026-11-21T14:30:00Z',
    documents: []
  };

  beforeEach(async () => {
    mockApiService = {
      getPublicElections: vi.fn().mockReturnValue(of({ success: true, message: 'OK', data: [mockElection] })),
      getAllPublicCandidates: vi.fn().mockReturnValue(of({ success: true, message: 'OK', data: [mockCandidacy] })),
      getPublicCandidates: vi.fn().mockReturnValue(of({ success: true, message: 'OK', data: [mockCandidacy] })),
      getOpenElectionsForCandidacy: vi.fn().mockReturnValue(of({ success: true, message: 'OK', data: [mockElection] })),
      getMyCandidacies: vi.fn().mockReturnValue(of({ success: true, message: 'OK', data: [mockCandidacy] })),
      applyCandidacyMultipart: vi.fn().mockReturnValue(of({ success: true, message: 'Candidature enregistrée', data: mockCandidacy })),
      invalidateElectionsCache: vi.fn()
    };

    mockAuthService = {
      isAuthenticated: vi.fn().mockReturnValue(true),
      currentUser: vi.fn().mockReturnValue(mockUser),
      isSuperAdmin: vi.fn().mockReturnValue(false),
      isCommission: vi.fn().mockReturnValue(false)
    };

    await TestBed.configureTestingModule({
      imports: [CandidaciesComponent],
      providers: [
        provideRouter([]),
        { provide: ApiService, useValue: mockApiService },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CandidaciesComponent);
    component = fixture.componentInstance;
  });

  it('1. Onglet Liste - État initial & chargement réactif', () => {
    expect(component).toBeTruthy();
    expect(component.activeTab()).toBe('LIST');
    expect(component.loadingCandidates()).toBe(true);
  });

  it('2. Onglet Liste - Affichage avec succès des candidatures et métadonnées', () => {
    fixture.detectChanges(); // Déclenche ngOnInit

    expect(component.loadingCandidates()).toBe(false);
    expect(component.candidates().length).toBe(1);
    expect(component.candidates()[0].candidat.prenom).toBe('Awa');
    expect(component.candidates()[0].statut).toBe('VALIDATED');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Espace Candidats & Professions de Foi');
    expect(compiled.textContent).toContain('Awa Sow');
    expect(compiled.textContent).toContain('Agir ensemble pour une université moderne');
  });

  it('3. Onglet Liste - État vide si aucune candidature n’est enregistrée', () => {
    mockApiService.getAllPublicCandidates.mockReturnValue(of({ success: true, message: 'OK', data: [] }));

    fixture.detectChanges();

    expect(component.loadingCandidates()).toBe(false);
    expect(component.candidates().length).toBe(0);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Aucun candidat enregistré pour ce scrutin');
  });

  it('4. Onglet Liste - État Erreur API avec affichage de l’erreur et bouton Réessayer', () => {
    mockApiService.getAllPublicCandidates.mockReturnValue(
      throwError(() => ({ error: { message: 'Serveur de candidatures temporairement indisponible' } }))
    );

    fixture.detectChanges();

    expect(component.loadingCandidates()).toBe(false);
    expect(component.candidatesError()).toContain('Serveur de candidatures temporairement indisponible');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Réessayer');
  });

  it('5. Onglet Liste - Filtrage par scrutin électoral', () => {
    fixture.detectChanges();

    component.onElectionFilterChange(1);

    expect(component.selectedElectionId()).toBe(1);
    expect(mockApiService.getPublicCandidates).toHaveBeenCalledWith(1);
  });

  it('6. Onglet Postuler - Recherche et chargement des scrutins ouverts', () => {
    fixture.detectChanges();

    component.setTab('APPLY');
    fixture.detectChanges();

    expect(component.activeTab()).toBe('APPLY');
    expect(component.loadingOpenElections()).toBe(false);
    expect(component.candidaciesOpenElections().length).toBe(1);
    expect(component.newCandidacy.electionId).toBe(1);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Dépôt de Dossier de Candidature');
    expect(compiled.textContent).toContain('Élection des Délégués UFR Sciences et Technologies 2026');
  });

  it('7. Onglet Postuler - Prévention des doublons si candidature déjà déposée', () => {
    fixture.detectChanges();

    component.setTab('APPLY');
    fixture.detectChanges();

    // Awa Sow a déjà mockCandidacy sur electionId: 1
    expect(component.isAlreadyAppliedForSelected()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Candidature déjà enregistrée pour ce scrutin');
  });

  it('8. Onglet Mes Candidatures - Affichage de l’historique des dossiers et statut', () => {
    fixture.detectChanges();

    component.setTab('MY_DOSSIER');
    fixture.detectChanges();

    expect(component.activeTab()).toBe('MY_DOSSIER');
    expect(component.myCandidacies().length).toBe(1);
    expect(component.myCandidacies()[0].statut).toBe('VALIDATED');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Mes Dossiers de Candidature');
    expect(compiled.textContent).toContain('Candidature Validée');
  });
});
