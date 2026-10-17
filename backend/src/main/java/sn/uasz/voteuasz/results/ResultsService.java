package sn.uasz.voteuasz.results;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.audit.AuditService;
import sn.uasz.voteuasz.candidacies.Candidacy;
import sn.uasz.voteuasz.candidacies.CandidacyRepository;
import sn.uasz.voteuasz.candidacies.CandidacyStatus;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.elections.ElectionStatus;
import sn.uasz.voteuasz.eligibility.EligibilityRepository;
import sn.uasz.voteuasz.voting.BallotRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResultsService {

    private static final Logger log = LoggerFactory.getLogger(ResultsService.class);

    private final ElectionResultRepository resultRepository;
    private final ElectionRepository electionRepository;
    private final BallotRepository ballotRepository;
    private final CandidacyRepository candidacyRepository;
    private final EligibilityRepository eligibilityRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public ResultsService(ElectionResultRepository resultRepository,
                          ElectionRepository electionRepository,
                          BallotRepository ballotRepository,
                          CandidacyRepository candidacyRepository,
                          EligibilityRepository eligibilityRepository,
                          SimpMessagingTemplate messagingTemplate,
                          AuditService auditService,
                          ObjectMapper objectMapper) {
        this.resultRepository = resultRepository;
        this.electionRepository = electionRepository;
        this.ballotRepository = ballotRepository;
        this.candidacyRepository = candidacyRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.messagingTemplate = messagingTemplate;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Dépouillement officiel des votes : agrégation stricte et vérifiable des bulletins réels.
     */
    @Transactional
    public ElectionResultDto countVotes(Long electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Scrutin introuvable : " + electionId));

        if (election.getStatut() != ElectionStatus.VOTING_CLOSED && election.getStatut() != ElectionStatus.COUNTING) {
            throw new BusinessException("Le dépouillement ne peut s'effectuer que lorsque le scrutin est clôturé (VOTING_CLOSED)");
        }

        election.setStatut(ElectionStatus.COUNTING);
        electionRepository.save(election);

        // Récupération des chiffres réels du scrutin
        int inscrits = (int) eligibilityRepository.countByElectionIdAndEligibleTrue(electionId);
        if (inscrits == 0) {
            inscrits = 1; // Évite la division par zéro en cas d'effectif non pré-chargé
        }

        int votants = (int) ballotRepository.countByElectionId(electionId);
        int blancs = (int) ballotRepository.countByElectionIdAndEstBlancTrue(electionId);
        int valides = votants - blancs;

        double participation = Math.round(((double) votants / inscrits * 100.0) * 100.0) / 100.0;

        // Calcul des voix par candidat
        List<Candidacy> candidacies = candidacyRepository.findByElectionIdAndStatut(electionId, CandidacyStatus.VALIDATED);
        List<CandidateResultDto> candidateResults = new ArrayList<>();

        for (Candidacy cand : candidacies) {
            long voix = ballotRepository.countByElectionIdAndCandidacyId(electionId, cand.getId());
            double pct = (valides > 0) ? Math.round(((double) voix / valides * 100.0) * 100.0) / 100.0 : 0.0;

            CandidateResultDto cr = new CandidateResultDto(
                    cand.getId(),
                    cand.getCandidat().getId(),
                    cand.getCandidat().getNom(),
                    cand.getCandidat().getPrenom(),
                    cand.getPhotoUrl(),
                    voix,
                    pct
            );
            candidateResults.add(cr);
        }

        // Sérialisation JSON des résultats détaillés
        String jsonResultats;
        try {
            jsonResultats = objectMapper.writeValueAsString(candidateResults);
        } catch (Exception e) {
            throw new BusinessException("Erreur de sérialisation des résultats du dépouillement", e);
        }

        ElectionResult result = resultRepository.findByElectionId(electionId)
                .orElse(new ElectionResult());

        result.setElection(election);
        result.setInscritsTotal(inscrits);
        result.setVotantsTotal(votants);
        result.setBulletinsBlancs(blancs);
        result.setBulletinsValides(valides);
        result.setTauxParticipation(participation);
        result.setDonneesCandidatsJson(jsonResultats);
        result.setStatutDepouillement(CountingStatus.VALIDATED);
        result.setDateDepouillement(OffsetDateTime.now());
        result.setDateValidation(OffsetDateTime.now());

        ElectionResult saved = resultRepository.save(result);
        auditService.logEvent("VOTES_COUNTED", String.format("Dépouillement terminé : %d votants, %d valides", votants, valides), electionId, "SUCCESS");

        return mapToDto(saved, candidateResults);
    }

    /**
     * Publication officielle des résultats par la Commission Électorale.
     * Déclenche la diffusion en temps réel via WebSocket STOMP.
     */
    @Transactional
    public ElectionResultDto publishResults(Long electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Scrutin introuvable : " + electionId));

        ElectionResult result = resultRepository.findByElectionId(electionId)
                .orElseThrow(() -> new BusinessException("Le dépouillement doit impérativement être validé avant la publication officielle"));

        election.setStatut(ElectionStatus.RESULTS_PUBLISHED);
        electionRepository.save(election);

        result.setStatutDepouillement(CountingStatus.PUBLISHED);
        ElectionResult saved = resultRepository.save(result);

        List<CandidateResultDto> candidates = deserializeCandidateResults(saved.getDonneesCandidatsJson());
        ElectionResultDto dto = mapToDto(saved, candidates);

        // Diffusion temps réel sur le canal WebSocket public du scrutin
        try {
            messagingTemplate.convertAndSend("/topic/results/" + electionId, dto);
            messagingTemplate.convertAndSend("/topic/results/summary", dto);
            log.info("Résultats officiels diffusés via WebSocket STOMP pour l'élection ID : {}", electionId);
        } catch (Exception e) {
            log.warn("Impossible de diffuser les résultats sur WebSocket : {}", e.getMessage());
        }

        auditService.logEvent("RESULTS_PUBLISHED", "Publication officielle des résultats du scrutin", electionId, "SUCCESS");
        return dto;
    }

    /**
     * Consultation publique des résultats (interdite avant publication officielle).
     */
    @Transactional(readOnly = true)
    public ElectionResultDto getPublicResults(Long electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Scrutin introuvable : " + electionId));

        if (election.getStatut() != ElectionStatus.RESULTS_PUBLISHED && election.getStatut() != ElectionStatus.ARCHIVED) {
            throw new BusinessException("Les résultats de cette élection ne sont pas encore rendus publics.");
        }

        ElectionResult result = resultRepository.findByElectionId(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun résultat publié pour ce scrutin"));

        List<CandidateResultDto> candidates = deserializeCandidateResults(result.getDonneesCandidatsJson());
        return mapToDto(result, candidates);
    }

    @Transactional(readOnly = true)
    public ElectionResultDto getElectionResultsForCommission(Long electionId) {
        ElectionResult result = resultRepository.findByElectionId(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun dépouillement enregistré pour ce scrutin"));

        List<CandidateResultDto> candidates = deserializeCandidateResults(result.getDonneesCandidatsJson());
        return mapToDto(result, candidates);
    }

    private List<CandidateResultDto> deserializeCandidateResults(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<CandidateResultDto>>() {});
        } catch (Exception e) {
            log.error("Erreur de désérialisation des résultats des candidats", e);
            return new ArrayList<>();
        }
    }

    private ElectionResultDto mapToDto(ElectionResult entity, List<CandidateResultDto> candidates) {
        ElectionResultDto dto = new ElectionResultDto();
        dto.setId(entity.getId());
        dto.setElectionId(entity.getElection().getId());
        dto.setElectionTitre(entity.getElection().getTitre());
        dto.setInscritsTotal(entity.getInscritsTotal());
        dto.setVotantsTotal(entity.getVotantsTotal());
        dto.setBulletinsValides(entity.getBulletinsValides());
        dto.setBulletinsBlancs(entity.getBulletinsBlancs());
        dto.setTauxParticipation(entity.getTauxParticipation());
        dto.setStatutDepouillement(entity.getStatutDepouillement());
        dto.setDateDepouillement(entity.getDateDepouillement());
        dto.setDateValidation(entity.getDateValidation());
        dto.setResultatsCandidats(candidates);
        return dto;
    }
}
