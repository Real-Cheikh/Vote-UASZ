package sn.uasz.voteuasz.elections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.audit.AuditService;
import sn.uasz.voteuasz.colleges.College;
import sn.uasz.voteuasz.colleges.CollegeRepository;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ElectionService {

    private static final Logger log = LoggerFactory.getLogger(ElectionService.class);

    private final ElectionRepository electionRepository;
    private final CollegeRepository collegeRepository;
    private final AuditService auditService;

    public ElectionService(ElectionRepository electionRepository,
                           CollegeRepository collegeRepository,
                           AuditService auditService) {
        this.electionRepository = electionRepository;
        this.collegeRepository = collegeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ElectionDto> findAll() {
        return electionRepository.findAllWithDetails().stream()
                .map(ElectionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ElectionDto> findPublicElections() {
        // Pour les pages publiques, on affiche les élections qui ne sont pas en simple brouillon
        List<ElectionStatus> publicStatuses = List.of(
                ElectionStatus.CANDIDATURES_OPEN,
                ElectionStatus.CANDIDATURES_CLOSED,
                ElectionStatus.VALIDATION,
                ElectionStatus.READY_TO_VOTE,
                ElectionStatus.VOTING_OPEN,
                ElectionStatus.VOTING_CLOSED,
                ElectionStatus.COUNTING,
                ElectionStatus.RESULTS_PUBLISHED,
                ElectionStatus.ARCHIVED
        );
        return electionRepository.findByStatutInWithDetails(publicStatuses).stream()
                .map(ElectionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ElectionDto findById(Long id) {
        Election election = electionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élection introuvable avec l'id : " + id));
        return ElectionDto.fromEntity(election);
    }

    @Transactional
    public ElectionDto create(ElectionCreateDto dto) {
        if (dto.getDateCloture().isBefore(dto.getDateOuverture())) {
            throw new BusinessException("La date de clôture doit être postérieure à la date d'ouverture du scrutin");
        }

        College college = collegeRepository.findById(dto.getCollegeId())
                .orElseThrow(() -> new ResourceNotFoundException("Collège électoral introuvable : " + dto.getCollegeId()));

        Election election = new Election();
        election.setTitre(dto.getTitre().trim());
        election.setDescription(dto.getDescription());
        election.setType(dto.getType());
        election.setCollege(college);
        election.setDateOuverture(dto.getDateOuverture());
        election.setDateCloture(dto.getDateCloture());
        election.setStatut(dto.isOuvrirCandidatures() ? ElectionStatus.CANDIDATURES_OPEN : ElectionStatus.DRAFT);
        election.setPermettreVoteBlanc(dto.isPermettreVoteBlanc());
        election.setDescriptionRegles(dto.getDescriptionRegles());
        election.setDateCreation(OffsetDateTime.now());

        ElectionRule rules = new ElectionRule();
        rules.setUfrCible(dto.getUfrCible() != null ? dto.getUfrCible() : college.getUfr());
        rules.setDepartementCible(dto.getDepartementCible() != null ? dto.getDepartementCible() : college.getDepartement());
        rules.setNiveauRequis(dto.getNiveauRequis() != null ? dto.getNiveauRequis() : college.getNiveau());
        rules.setCategorieRequise(dto.getCategorieRequise() != null ? dto.getCategorieRequise() : college.getCategorie().name());
        election.setRules(rules);

        Election saved = electionRepository.save(election);
        auditService.logEvent("ELECTION_CREATED", "Création de l'élection: " + saved.getTitre(), saved.getId(), "SUCCESS");

        return ElectionDto.fromEntity(saved);
    }

    @Transactional
    public ElectionDto updateStatus(Long id, ElectionStatusUpdateDto dto) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élection introuvable avec l'id : " + id));

        ElectionStatus currentStatus = election.getStatut();
        ElectionStatus targetStatus = dto.getStatut();

        if (currentStatus == targetStatus) {
            return ElectionDto.fromEntity(election);
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessException(String.format(
                    "Transition d'état illégale de '%s' vers '%s'. Les règles du workflow électoral interdisent cette opération.",
                    currentStatus, targetStatus));
        }

        // Vérifications spécifiques de conformité temporelle et organisationnelle
        OffsetDateTime now = OffsetDateTime.now();
        if (targetStatus == ElectionStatus.VOTING_OPEN) {
            if (now.isAfter(election.getDateCloture())) {
                throw new BusinessException("Impossible d'ouvrir le scrutin : la date limite de clôture est déjà dépassée");
            }
        }

        if (targetStatus == ElectionStatus.COUNTING && currentStatus != ElectionStatus.VOTING_CLOSED) {
            throw new BusinessException("Le dépouillement ne peut être initié que lorsque le scrutin est formellement clôturé (VOTING_CLOSED)");
        }

        election.setStatut(targetStatus);
        Election updated = electionRepository.save(election);

        auditService.logEvent(
                "ELECTION_STATUS_CHANGED",
                String.format("Statut passé de %s à %s. Motif: %s", currentStatus, targetStatus, dto.getMotif()),
                updated.getId(),
                "SUCCESS"
        );

        return ElectionDto.fromEntity(updated);
    }

    @Transactional
    public void delete(Long id) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Élection introuvable : " + id));

        // Empêcher la suppression d'une élection contenant des votes ou des résultats
        if (election.getStatut() != ElectionStatus.DRAFT && election.getStatut() != ElectionStatus.ARCHIVED) {
            throw new BusinessException("Impossible de supprimer une élection en cours ou ayant fait l'objet d'un scrutin actif. Archivez-la plutôt.");
        }

        electionRepository.delete(election);
        auditService.logEvent("ELECTION_DELETED", "Suppression de l'élection ID " + id, id, "SUCCESS");
    }
}
