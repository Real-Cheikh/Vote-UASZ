package sn.uasz.voteuasz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import sn.uasz.voteuasz.colleges.College;
import sn.uasz.voteuasz.colleges.CollegeRepository;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.VoteException;
import sn.uasz.voteuasz.elections.*;
import sn.uasz.voteuasz.eligibility.EligibilityDto;
import sn.uasz.voteuasz.eligibility.EligibilityService;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserCategory;
import sn.uasz.voteuasz.users.UserRepository;
import sn.uasz.voteuasz.voting.CastBallotDto;
import sn.uasz.voteuasz.voting.VotingService;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class EligibilityAndElectionWorkflowTests {

    @Autowired
    private ElectionService electionService;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EligibilityService eligibilityService;

    @Autowired
    private VotingService votingService;

    private College collegeEtudiant;
    private College collegeEnseignant;
    private User etudiant;

    @BeforeEach
    void setUp() {
        collegeEtudiant = collegeRepository.findByCode("COL_TEST_ETUD_ELIG").orElseGet(() ->
                collegeRepository.save(new College("COL_TEST_ETUD_ELIG", "Collège Étudiants Elig", "Desc", "UFR ST", "Informatique", "L3", UserCategory.ETUDIANT)));

        collegeEnseignant = collegeRepository.findByCode("COL_TEST_ENS_ELIG").orElseGet(() ->
                collegeRepository.save(new College("COL_TEST_ENS_ELIG", "Collège Enseignants Elig", "Desc", "UFR ST", "Informatique", "Permanent", UserCategory.ENSEIGNANT)));

        etudiant = userRepository.findByEmail("etud.elig@uasz.sn").orElseGet(() -> {
            User u = new User("etud.elig@uasz.sn", "pass", "Ndao", "Alioune", "ETUD-ELIG-01", "+221770003", UserCategory.ETUDIANT);
            return userRepository.save(u);
        });
    }

    @Test
    @DisplayName("Test 4 : Contrôle dynamique d'éligibilité (admis pour son collège, rejeté pour collège enseignant)")
    void testDynamicEligibilityControl() {
        // Scrutin étudiants
        Election electionEtud = new Election();
        electionEtud.setTitre("Scrutin Étudiants UFR ST");
        electionEtud.setType(ElectionType.DELEGUE);
        electionEtud.setCollege(collegeEtudiant);
        electionEtud.setDateOuverture(OffsetDateTime.now().minusHours(1));
        electionEtud.setDateCloture(OffsetDateTime.now().plusHours(2));
        electionEtud.setStatut(ElectionStatus.VOTING_OPEN);
        electionEtud = electionRepository.save(electionEtud);

        EligibilityDto eligEtud = eligibilityService.checkUserEligibility(electionEtud.getId(), etudiant.getId());
        assertTrue(eligEtud.isEligible(), "L'étudiant doit être éligible au scrutin de son collège");

        // Scrutin enseignants
        Election electionEns = new Election();
        electionEns.setTitre("Scrutin Enseignants");
        electionEns.setType(ElectionType.DIRECTEUR_UFR);
        electionEns.setCollege(collegeEnseignant);
        electionEns.setDateOuverture(OffsetDateTime.now().minusHours(1));
        electionEns.setDateCloture(OffsetDateTime.now().plusHours(2));
        electionEns.setStatut(ElectionStatus.VOTING_OPEN);
        electionEns = electionRepository.save(electionEns);

        EligibilityDto eligEns = eligibilityService.checkUserEligibility(electionEns.getId(), etudiant.getId());
        assertFalse(eligEns.isEligible(), "L'étudiant ne doit PAS être éligible au scrutin des enseignants");
    }

    @Test
    @DisplayName("Test 5 : Rejet immédiat de toute tentative de vote sur un scrutin non ouvert (DRAFT ou READY_TO_VOTE)")
    void testVoteOnNonOpenElectionRejected() {
        Election electionBrouillon = new Election();
        electionBrouillon.setTitre("Scrutin en Préparation");
        electionBrouillon.setType(ElectionType.DELEGUE);
        electionBrouillon.setCollege(collegeEtudiant);
        electionBrouillon.setDateOuverture(OffsetDateTime.now().plusDays(2));
        electionBrouillon.setDateCloture(OffsetDateTime.now().plusDays(3));
        electionBrouillon.setStatut(ElectionStatus.DRAFT);
        electionBrouillon = electionRepository.save(electionBrouillon);

        CastBallotDto ballotDto = new CastBallotDto(electionBrouillon.getId(), "fake_token", null, true);
        assertThrows(VoteException.class, () -> votingService.castBallot(ballotDto));
    }

    @Test
    @DisplayName("Test 6 : Rejet immédiat de toute tentative de vote sur un scrutin clôturé")
    void testVoteOnClosedElectionRejected() {
        Election electionClose = new Election();
        electionClose.setTitre("Scrutin Terminé");
        electionClose.setType(ElectionType.DELEGUE);
        electionClose.setCollege(collegeEtudiant);
        electionClose.setDateOuverture(OffsetDateTime.now().minusDays(2));
        electionClose.setDateCloture(OffsetDateTime.now().minusHours(1));
        electionClose.setStatut(ElectionStatus.VOTING_CLOSED);
        electionClose = electionRepository.save(electionClose);

        CastBallotDto ballotDto = new CastBallotDto(electionClose.getId(), "fake_token", null, true);
        assertThrows(VoteException.class, () -> votingService.castBallot(ballotDto));
    }

    @Test
    @DisplayName("Workflow des transitions : Contrôle strict de la machine à états côté serveur")
    void testWorkflowStateMachineTransitions() {
        Election election = new Election();
        election.setTitre("Scrutin Machine à États");
        election.setType(ElectionType.DELEGUE);
        election.setCollege(collegeEtudiant);
        election.setDateOuverture(OffsetDateTime.now().minusHours(1));
        election.setDateCloture(OffsetDateTime.now().plusHours(4));
        election.setStatut(ElectionStatus.DRAFT);
        election = electionRepository.save(election);

        final Long testId = election.getId();

        // Transition légale : DRAFT -> CANDIDATURES_OPEN
        ElectionDto updated1 = electionService.updateStatus(testId,
                new ElectionStatusUpdateDto(ElectionStatus.CANDIDATURES_OPEN, "Ouverture officielle dépôts"));
        assertEquals(ElectionStatus.CANDIDATURES_OPEN, updated1.getStatut());

        // Transition illégale directe : CANDIDATURES_OPEN -> COUNTING (Interdite !)
        assertThrows(BusinessException.class, () ->
                electionService.updateStatus(testId,
                        new ElectionStatusUpdateDto(ElectionStatus.COUNTING, "Tentative de saut illégal")));

        // Transition légale : CANDIDATURES_OPEN -> CANDIDATURES_CLOSED -> VALIDATION -> READY_TO_VOTE
        electionService.updateStatus(testId, new ElectionStatusUpdateDto(ElectionStatus.CANDIDATURES_CLOSED, "Clôture"));
        electionService.updateStatus(testId, new ElectionStatusUpdateDto(ElectionStatus.VALIDATION, "Examen"));
        ElectionDto ready = electionService.updateStatus(testId, new ElectionStatusUpdateDto(ElectionStatus.READY_TO_VOTE, "Prêt"));
        assertEquals(ElectionStatus.READY_TO_VOTE, ready.getStatut());
    }
}
