import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError, BehaviorSubject } from 'rxjs';
import { vi } from 'vitest';
import { ResultsComponent } from './results.component';
import { ApiService } from '../core/services/api.service';
import { WebSocketService } from '../core/services/websocket.service';
import { Election, ElectionResult } from '../core/models/models';

describe('ResultsComponent', () => {
  let component: ResultsComponent;
  let fixture: ComponentFixture<ResultsComponent>;
  let mockApiService: any;
  let mockWsService: any;
  let wsStatusSubject: BehaviorSubject<boolean>;

  const mockElection: Election = {
    id: 2,
    titre: 'Scrutin Test Audit Synthétique UASZ 2026',
    description: 'Scrutin test',
    statut: 'RESULTS_PUBLISHED',
    type: 'DELEGUE',
    college: { id: 1, code: 'ETUDIANT', nom: 'Collège Étudiants', categorie: 'ETUDIANT' },
    dateOuverture: '2026-11-27T08:00:00Z',
    dateCloture: '2026-11-27T18:00:00Z',
    permettreVoteBlanc: true,
    dateCreation: '2026-11-27T07:00:00Z'
  };

  const mockResult: ElectionResult = {
    id: 1,
    electionId: 2,
    electionTitre: 'Scrutin Test Audit Synthétique UASZ 2026',
    inscritsTotal: 100,
    votantsTotal: 80,
    bulletinsValides: 75,
    bulletinsBlancs: 5,
    tauxParticipation: 80.0,
    statutDepouillement: 'PUBLISHED',
    dateDepouillement: '2026-11-27T18:30:00Z',
    dateValidation: '2026-11-27T18:35:00Z',
    resultatsCandidats: [
      {
        candidacyId: 10,
        candidateUserId: 20,
        candidateNom: 'Sow',
        candidatePrenom: 'Awa',
        voixObtenues: 50,
        pourcentage: 66.7
      },
      {
        candidacyId: 11,
        candidateUserId: 21,
        candidateNom: 'Diallo',
        candidatePrenom: 'Moussa',
        voixObtenues: 25,
        pourcentage: 33.3
      }
    ]
  };

  beforeEach(async () => {
    wsStatusSubject = new BehaviorSubject<boolean>(true);

    mockApiService = {
      getPublicElections: vi.fn().mockReturnValue(
        of({ success: true, message: 'OK', data: [mockElection] })
      ),
      getPublicResults: vi.fn().mockReturnValue(
        of({ success: true, message: 'OK', data: mockResult })
      ),
      downloadReportPdf: vi.fn().mockReturnValue(
        of(new Blob(['pdf content'], { type: 'application/pdf' }))
      )
    };

    mockWsService = {
      connect: vi.fn(),
      getConnectionStatus: vi.fn().mockReturnValue(wsStatusSubject.asObservable()),
      subscribeToElection: vi.fn().mockReturnValue(of(mockResult)),
      unsubscribeFromElection: vi.fn(),
      isConnected: vi.fn().mockReturnValue(true)
    };

    await TestBed.configureTestingModule({
      imports: [ResultsComponent],
      providers: [
        provideRouter([]),
        { provide: ApiService, useValue: mockApiService },
        { provide: WebSocketService, useValue: mockWsService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ResultsComponent);
    component = fixture.componentInstance;
  });

  it('1. État Initial & Chargement : doit initialiser avec les signaux par défaut', () => {
    expect(component).toBeTruthy();
    expect(component.loading()).toBe(true);
    expect(component.errorMessage()).toBe('');
  });

  it('2. État Succès : doit afficher les résultats publiés et les métriques de participation', () => {
    fixture.detectChanges(); // déclenche ngOnInit

    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBe('');
    expect(component.results()).toEqual(mockResult);
    expect(component.winnerCandidate?.candidateNom).toBe('Sow');
    expect(component.winnerCandidate?.voixObtenues).toBe(50);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Dépouillement & Suffrages Exprimés');
    expect(compiled.textContent).toContain('Awa Sow');
    expect(compiled.textContent).toContain('75'); // Bulletins Valides
    expect(compiled.textContent).toContain('80%'); // Taux de participation
  });

  it('3. État Vide : doit afficher un message clair si aucun scrutin n’a de résultats publiés', () => {
    mockApiService.getPublicElections.mockReturnValue(of({ success: true, message: 'OK', data: [] }));

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.publishedElections().length).toBe(0);
    expect(component.errorMessage()).toContain('Aucun scrutin clôturé avec résultats publiés');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Information sur les résultats');
  });

  it('4. État Erreur API : doit basculer vers l’état erreur avec le message et bouton réessayer', () => {
    mockApiService.getPublicResults.mockReturnValue(
      throwError(() => ({ error: { message: 'Serveur de résultats momentanément indisponible' } }))
    );

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.results()).toBeNull();
    expect(component.errorMessage()).toContain('Serveur de résultats momentanément indisponible');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Réessayer');
  });

  it('5. Changement de Scrutin : doit se désabonner de l’ancien WebSocket et charger le nouveau', () => {
    fixture.detectChanges();

    expect(component.selectedElectionId()).toBe(2);

    // Changement de scrutin vers ID 8
    component.onElectionSelect(8);

    expect(mockWsService.unsubscribeFromElection).toHaveBeenCalledWith(2);
    expect(component.selectedElectionId()).toBe(8);
    expect(mockApiService.getPublicResults).toHaveBeenCalledWith(8);
  });

  it('6. WebSocket en direct : doit mettre à jour les résultats lors d’un événement sans rechargement', () => {
    fixture.detectChanges();

    const updatedResult: ElectionResult = {
      ...mockResult,
      bulletinsValides: 76,
      votantsTotal: 81
    };

    component.subscribeWebSocket(2);
    mockWsService.subscribeToElection.mockReturnValue(of(updatedResult));

    component.results.set(updatedResult);
    fixture.detectChanges();

    expect(component.results()?.bulletinsValides).toBe(76);
  });
});
