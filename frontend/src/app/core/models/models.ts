export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface User {
  id: number;
  email: string;
  nom: string;
  prenom: string;
  numeroIdentification: string;
  telephone?: string;
  categorie: 'ETUDIANT' | 'PAT' | 'ENSEIGNANT';
  actif: boolean;
  bloque: boolean;
  roles: string[];
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  email: string;
  nom: string;
  prenom: string;
  categorie: string;
  roles: string[];
}

export interface College {
  id: number;
  code: string;
  nom: string;
  description?: string;
  ufr?: string;
  departement?: string;
  niveau?: string;
  categorie: 'ETUDIANT' | 'PAT' | 'ENSEIGNANT';
}

export type ElectionType = 'DELEGUE' | 'DIRECTEUR_UFR' | 'VICE_RECTEUR' | 'AUTRE';

export type ElectionStatus =
  | 'DRAFT'
  | 'CANDIDATURES_OPEN'
  | 'CANDIDATURES_CLOSED'
  | 'VALIDATION'
  | 'READY_TO_VOTE'
  | 'VOTING_OPEN'
  | 'VOTING_CLOSED'
  | 'COUNTING'
  | 'RESULTS_PUBLISHED'
  | 'ARCHIVED';

export interface Election {
  id: number;
  titre: string;
  description?: string;
  type: ElectionType;
  college: College;
  dateOuverture: string;
  dateCloture: string;
  statut: ElectionStatus;
  permettreVoteBlanc: boolean;
  descriptionRegles?: string;
  dateCreation: string;
  ufrCible?: string;
  departementCible?: string;
  niveauRequis?: string;
  categorieRequise?: string;
}

export interface Candidacy {
  id: number;
  electionId: number;
  electionTitre?: string;
  collegeNom?: string;
  candidat: User;
  professionFoi: string;
  biographie?: string;
  photoUrl?: string;
  statut: 'SUBMITTED' | 'VALIDATED' | 'REJECTED' | 'WITHDRAWN';
  motifDecision?: string;
  dateDecision?: string;
  dateSoumission: string;
  documents?: CandidateDocument[];
}

export interface CandidateDocument {
  id: number;
  typeDocument: 'CV' | 'ATTESTATION_INSCRIPTION' | 'PROGRAMME' | 'PHOTO' | 'CASIER_JUDICIAIRE';
  nomFichierOriginal: string;
  cheminFichier: string;
  typeMime: string;
  tailleOctets: number;
  dateUpload: string;
}

export interface Eligibility {
  id: number;
  electionId: number;
  election: Election;
  eligible: boolean;
  aVote: boolean;
  dateVote?: string;
  motifIneligibilite?: string;
}

export interface OtpChallengeResponse {
  message: string;
  dateExpiration: string;
  validiteSecondes: number;
  destinationMasquee: string;
}

export interface VoteAuthorizationResponse {
  voteToken: string;
  electionId: number;
  electionTitre: string;
  dateExpiration: string;
  message: string;
}

export interface VoteReceipt {
  recuVoteHash: string;
  electionId: number;
  electionTitre: string;
  dateEnregistrement: string;
  message: string;
}

export interface ReceiptVerification {
  valide: boolean;
  recuVoteHash: string;
  electionId?: number;
  electionTitre?: string;
  dateEnregistrement?: string;
  message: string;
}

export interface CandidateResult {
  candidacyId: number;
  candidateUserId: number;
  candidateNom: string;
  candidatePrenom: string;
  photoUrl?: string;
  voixObtenues: number;
  pourcentage: number;
}

export interface ElectionResult {
  id: number;
  electionId: number;
  electionTitre: string;
  inscritsTotal: number;
  votantsTotal: number;
  bulletinsValides: number;
  bulletinsBlancs: number;
  tauxParticipation: number;
  statutDepouillement: 'PROVISIONAL' | 'VALIDATED' | 'PUBLISHED';
  dateDepouillement: string;
  dateValidation?: string;
  resultatsCandidats: CandidateResult[];
}

export interface AuditEvent {
  id: number;
  eventUuid: string;
  horodatage: string;
  eventType: string;
  acteur: string;
  electionId?: number;
  resultat: string;
  details: string;
  ipSource?: string;
  correlationId?: string;
  empreinteChaine: string;
}

export interface Complaint {
  id: number;
  electionId: number;
  electionTitre?: string;
  user?: User;
  objet: string;
  description: string;
  statut: 'SUBMITTED' | 'UNDER_REVIEW' | 'RESOLVED' | 'REJECTED';
  reponseCommission?: string;
  piecesJointesUrls?: string;
  dateDepot: string;
  dateTraitement?: string;
}
