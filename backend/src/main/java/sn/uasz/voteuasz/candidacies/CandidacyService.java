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
        return candidacyRepository.findByElectionIdAndStatut(electionId, CandidacyStatus.VALIDATED).stream()
                .map(CandidacyDto::fromEntity)
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
        return CandidacyDto.fromEntity(candidacy);
    }

    @Transactional
    public CandidacyDto apply(CandidacyApplyDto dto) {
        User currentUser = getCurrentAuthenticatedUser();

        Election election = electionRepository.findById(dto.getElectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Élection introuvable avec l'id : " + dto.getElectionId()));

        if (election.getStatut() != ElectionStatus.CANDIDATURES_OPEN) {
            throw new BusinessException("Le dépôt de candidatures est actuellement fermé pour ce scrutin");
        }

        if (candidacyRepository.existsByElectionIdAndCandidatId(election.getId(), currentUser.getId())) {
            throw new BusinessException("Vous avez déjà déposé une candidature pour cette élection");
        }

        Candidacy candidacy = new Candidacy();
        candidacy.setElection(election);
        candidacy.setCandidat(currentUser);
        candidacy.setProfessionFoi(dto.getProfessionFoi());
        candidacy.setBiographie(dto.getBiographie());
        candidacy.setStatut(CandidacyStatus.SUBMITTED);
        candidacy.setDateSoumission(OffsetDateTime.now());

        Candidacy saved = candidacyRepository.save(candidacy);
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
