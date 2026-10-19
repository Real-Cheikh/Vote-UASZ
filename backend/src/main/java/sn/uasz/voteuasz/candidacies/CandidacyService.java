package sn.uasz.voteuasz.candidacies;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sn.uasz.voteuasz.audit.AuditService;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionDto;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.elections.ElectionStatus;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;
import sn.uasz.voteuasz.security.UserPrincipal;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CandidacyService {

    private final CandidacyRepository candidacyRepository;
    private final CandidateDocumentRepository documentRepository;
    private final ElectionRepository electionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    public CandidacyService(CandidacyRepository candidacyRepository,
                            CandidateDocumentRepository documentRepository,
                            ElectionRepository electionRepository,
                            UserRepository userRepository,
                            RoleRepository roleRepository,
                            FileStorageService fileStorageService,
                            AuditService auditService) {
        this.candidacyRepository = candidacyRepository;
        this.documentRepository = documentRepository;
        this.electionRepository = electionRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CandidacyDto> findPublicCandidatesByElection(Long electionId) {
        return candidacyRepository.findByElectionId(electionId).stream()
                .filter(c -> c.getStatut() != CandidacyStatus.REJECTED && c.getStatut() != CandidacyStatus.WITHDRAWN)
                .map(CandidacyDto::fromEntityPublic)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CandidacyDto> findAllPublicCandidates() {
        return candidacyRepository.findAll().stream()
                .filter(c -> c.getStatut() != CandidacyStatus.REJECTED && c.getStatut() != CandidacyStatus.WITHDRAWN)
                .map(CandidacyDto::fromEntityPublic)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CandidacyDto> findAllByElection(Long electionId) {
        return candidacyRepository.findByElectionId(electionId).stream()
                .map(CandidacyDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CandidacyDto findById(Long id) {
        Candidacy candidacy = candidacyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable avec l'id : " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = false;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            boolean isOwner = candidacy.getCandidat().getId().equals(principal.getId());
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN_ELECTORAL") || a.getAuthority().equals("ROLE_COMMISSION_ELECTORALE"));
            isPrivileged = isOwner || isAdmin;
        }

        return isPrivileged ? CandidacyDto.fromEntity(candidacy) : CandidacyDto.fromEntityPublic(candidacy);
    }

    @Transactional(readOnly = true)
    public List<CandidacyDto> findMyCandidacies() {
        User currentUser = getCurrentAuthenticatedUser();
        return candidacyRepository.findByCandidatId(currentUser.getId()).stream()
                .map(CandidacyDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ElectionDto> findOpenElectionsForCurrentUser() {
        List<Election> openElections = electionRepository.findByStatutInWithDetails(List.of(ElectionStatus.CANDIDATURES_OPEN));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return openElections.stream().map(ElectionDto::fromEntity).collect(Collectors.toList());
        }

        User currentUser = userRepository.findById(principal.getId()).orElse(null);
        if (currentUser == null) {
            return openElections.stream().map(ElectionDto::fromEntity).collect(Collectors.toList());
        }

        boolean isPrivileged = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_SUPER_ADMIN_ELECTORAL || r.getName() == RoleName.ROLE_COMMISSION_ELECTORALE);

        if (isPrivileged) {
            return openElections.stream().map(ElectionDto::fromEntity).collect(Collectors.toList());
        }

        return openElections.stream()
                .filter(e -> e.getCollege() == null || e.getCollege().getCategorie() == null || e.getCollege().getCategorie() == currentUser.getCategorie())
                .map(ElectionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CandidacyDto findMyCandidacyByElection(Long electionId) {
        User currentUser = getCurrentAuthenticatedUser();
        return candidacyRepository.findByElectionIdAndCandidatId(electionId, currentUser.getId())
                .map(CandidacyDto::fromEntity)
                .orElse(null);
    }

    @Transactional
    public CandidacyDto apply(CandidacyApplyDto dto) {
        return applyWithDocuments(dto.getElectionId(), dto.getProfessionFoi(), dto.getBiographie(), null, null);
    }

    @Transactional
    public CandidacyDto applyWithDocuments(Long electionId, String professionFoi, String biographie, MultipartFile cvFile, MultipartFile photoFile) {
        User currentUser = getCurrentAuthenticatedUser();

        if (!currentUser.isActif() || currentUser.isBloque()) {
            throw new BusinessException("Votre compte est inactif ou verrouillé par l'administration électorale.");
        }

        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Élection introuvable avec l'id : " + electionId));

        if (election.getStatut() != ElectionStatus.CANDIDATURES_OPEN) {
            throw new BusinessException("Le dépôt de candidatures est actuellement fermé pour ce scrutin");
        }

        boolean isSuperAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_SUPER_ADMIN_ELECTORAL || r.getName() == RoleName.ROLE_COMMISSION_ELECTORALE);

        if (!isSuperAdmin && election.getCollege() != null && election.getCollege().getCategorie() != null) {
            if (election.getCollege().getCategorie() != currentUser.getCategorie()) {
                throw new BusinessException(String.format(
                        "Votre catégorie (%s) ne correspond pas au collège électoral de ce scrutin (%s)",
                        currentUser.getCategorie(), election.getCollege().getNom()
                ));
            }
        }

        if (candidacyRepository.existsByElectionIdAndCandidatId(election.getId(), currentUser.getId())) {
            throw new BusinessException("Vous avez déjà déposé une candidature pour cette élection");
        }

        Candidacy candidacy = new Candidacy();
        candidacy.setElection(election);
        candidacy.setCandidat(currentUser);
        candidacy.setProfessionFoi(professionFoi);
        candidacy.setBiographie(biographie);
        candidacy.setStatut(CandidacyStatus.SUBMITTED);
        candidacy.setDateSoumission(OffsetDateTime.now());

        // Attribuer le rôle CANDIDAT si non présent
        Role candidateRole = roleRepository.findByName(RoleName.ROLE_CANDIDAT)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CANDIDAT, "Candidat aux scrutins")));
        if (!currentUser.getRoles().contains(candidateRole)) {
            currentUser.addRole(candidateRole);
            userRepository.save(currentUser);
        }

        Candidacy saved = candidacyRepository.save(candidacy);

        // Traitement de la pièce jointe CV si fournie
        if (cvFile != null && !cvFile.isEmpty()) {
            String cvStoredFilename = fileStorageService.storeFile(cvFile);
            CandidateDocument cvDoc = new CandidateDocument();
            cvDoc.setCandidacy(saved);
            cvDoc.setTypeDocument(DocumentType.CV);
            cvDoc.setNomFichierOriginal(cvFile.getOriginalFilename());
            cvDoc.setCheminFichier(cvStoredFilename);
            cvDoc.setTypeMime(cvFile.getContentType());
            cvDoc.setTailleOctets(cvFile.getSize());
            cvDoc.setDateUpload(OffsetDateTime.now());
            documentRepository.save(cvDoc);
            saved.getDocuments().add(cvDoc);
        }

        // Traitement de la photo de campagne si fournie
        if (photoFile != null && !photoFile.isEmpty()) {
            String photoStoredFilename = fileStorageService.storeFile(photoFile);
            CandidateDocument photoDoc = new CandidateDocument();
            photoDoc.setCandidacy(saved);
            photoDoc.setTypeDocument(DocumentType.PHOTO);
            photoDoc.setNomFichierOriginal(photoFile.getOriginalFilename());
            photoDoc.setCheminFichier(photoStoredFilename);
            photoDoc.setTypeMime(photoFile.getContentType());
            photoDoc.setTailleOctets(photoFile.getSize());
            photoDoc.setDateUpload(OffsetDateTime.now());
            documentRepository.save(photoDoc);
            saved.getDocuments().add(photoDoc);
            saved.setPhotoUrl("/api/v1/candidacies/documents/file/" + photoStoredFilename);
            saved = candidacyRepository.save(saved);
        }

        auditService.logEvent("CANDIDACY_SUBMITTED", "Dépôt de candidature par " + currentUser.getEmail(), election.getId(), "SUCCESS");

        return CandidacyDto.fromEntity(saved);
    }

    @Transactional
    public CandidateDocumentDto uploadDocument(Long candidacyId, DocumentType type, MultipartFile file) {
        Candidacy candidacy = candidacyRepository.findById(candidacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : " + candidacyId));

        User currentUser = getCurrentAuthenticatedUser();
        // Vérification de propriété (sauf super admin)
        boolean isOwner = candidacy.getCandidat().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_SUPER_ADMIN_ELECTORAL || r.getName() == RoleName.ROLE_COMMISSION_ELECTORALE);

        if (!isOwner && !isAdmin) {
            throw new BusinessException("Vous n'êtes pas autorisé à téléverser des documents pour ce dossier");
        }

        String storedFilename = fileStorageService.storeFile(file);

        CandidateDocument document = new CandidateDocument();
        document.setCandidacy(candidacy);
        document.setTypeDocument(type);
        document.setNomFichierOriginal(file.getOriginalFilename());
        document.setCheminFichier(storedFilename);
        document.setTypeMime(file.getContentType());
        document.setTailleOctets(file.getSize());
        document.setDateUpload(OffsetDateTime.now());

        if (type == DocumentType.PHOTO) {
            candidacy.setPhotoUrl("/api/v1/candidacies/documents/file/" + storedFilename);
            candidacyRepository.save(candidacy);
        }

        CandidateDocument saved = documentRepository.save(document);
        return CandidateDocumentDto.fromEntity(saved);
    }

    @Transactional
    public CandidacyDto reviewCandidacy(Long candidacyId, CandidacyDecisionDto decisionDto) {
        Candidacy candidacy = candidacyRepository.findById(candidacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : " + candidacyId));

        candidacy.setStatut(decisionDto.getDecision());
        candidacy.setMotifDecision(decisionDto.getMotif());
        candidacy.setDateDecision(OffsetDateTime.now());

        // Si validé, s'assurer que le candidat possède bien le rôle CANDIDAT
        if (decisionDto.getDecision() == CandidacyStatus.VALIDATED) {
            User candidateUser = candidacy.getCandidat();
            Role candidateRole = roleRepository.findByName(RoleName.ROLE_CANDIDAT)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CANDIDAT, "Candidat officiel")));
            candidateUser.addRole(candidateRole);
            userRepository.save(candidateUser);
        }

        Candidacy updated = candidacyRepository.save(candidacy);
        auditService.logEvent(
                "CANDIDACY_REVIEWED",
                String.format("Décision %s sur candidature ID %d. Motif: %s", decisionDto.getDecision(), candidacyId, decisionDto.getMotif()),
                candidacy.getElection().getId(),
                "SUCCESS"
        );

        return CandidacyDto.fromEntity(updated);
    }

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException("Utilisateur non authentifié");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
