package sn.uasz.voteuasz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import sn.uasz.voteuasz.audit.AuditEvent;
import sn.uasz.voteuasz.audit.AuditService;
import sn.uasz.voteuasz.candidacies.Candidacy;
import sn.uasz.voteuasz.candidacies.CandidacyRepository;
import sn.uasz.voteuasz.candidacies.CandidacyStatus;
import sn.uasz.voteuasz.colleges.College;
import sn.uasz.voteuasz.colleges.CollegeRepository;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.elections.ElectionStatus;
import sn.uasz.voteuasz.elections.ElectionType;
import sn.uasz.voteuasz.reports.PdfReportGeneratorService;
import sn.uasz.voteuasz.results.ElectionResultDto;
import sn.uasz.voteuasz.results.ResultsService;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserCategory;
import sn.uasz.voteuasz.users.UserRepository;
import sn.uasz.voteuasz.voting.Ballot;
import sn.uasz.voteuasz.voting.BallotRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ResultsAndPdfReportTests {

    @Autowired
    private ResultsService resultsService;

    @Autowired
    private PdfReportGeneratorService pdfService;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private BallotRepository ballotRepository;

    @Autowired
    private CandidacyRepository candidacyRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Election election;
    private Candidacy cand1;
    private Candidacy cand2;

    @BeforeEach
    void setUp() {
        Role roleCandidat = roleRepository.findByName(RoleName.ROLE_CANDIDAT)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CANDIDAT, "Candidat")));

        User c1 = userRepository.findByEmail("cand1.res@uasz.sn").orElseGet(() -> {
            User u = new User("cand1.res@uasz.sn", passwordEncoder.encode("Pass1!"), "Diallo", "Mamadou", "CAND-01", "+221770001", UserCategory.ETUDIANT);
            u.setRoles(Set.of(roleCandidat));
            return userRepository.save(u);
        });

        User c2 = userRepository.findByEmail("cand2.res@uasz.sn").orElseGet(() -> {
            User u = new User("cand2.res@uasz.sn", passwordEncoder.encode("Pass2!"), "Ndiaye", "Aïssatou", "CAND-02", "+221770002", UserCategory.ETUDIANT);
            u.setRoles(Set.of(roleCandidat));
            return userRepository.save(u);
        });

        College college = collegeRepository.findByCode("COL_RESULTS_TEST").orElseGet(() ->
                collegeRepository.save(new College("COL_RESULTS_TEST", "Collège Résultats", "Desc", "UFR LSH", "Anglais", "L2", UserCategory.ETUDIANT)));

        election = new Election();
        election.setTitre("Élection Délégué LSH 2026");
        election.setType(ElectionType.DELEGUE);
        election.setCollege(college);
        election.setDateOuverture(OffsetDateTime.now().minusDays(2));
        election.setDateCloture(OffsetDateTime.now().minusHours(1)); // Clôturée
        election.setStatut(ElectionStatus.VOTING_CLOSED);
        election.setPermettreVoteBlanc(true);
        election = electionRepository.save(election);

        cand1 = new Candidacy();
        cand1.setElection(election);
        cand1.setCandidat(c1);
        cand1.setStatut(CandidacyStatus.VALIDATED);
        cand1 = candidacyRepository.save(cand1);

        cand2 = new Candidacy();
        cand2.setElection(election);
        cand2.setCandidat(c2);
        cand2.setStatut(CandidacyStatus.VALIDATED);
        cand2 = candidacyRepository.save(cand2);

        ballotRepository.deleteAll();

        // Simulation de dépôts de bulletins anonymes dans l'urne
        // 3 voix pour candidat 1, 2 voix pour candidat 2, 1 vote blanc = 6 bulletins au total
        ballotRepository.save(new Ballot(election, cand1, false, "recu_" + java.util.UUID.randomUUID()));
        ballotRepository.save(new Ballot(election, cand1, false, "recu_" + java.util.UUID.randomUUID()));
        ballotRepository.save(new Ballot(election, cand1, false, "recu_" + java.util.UUID.randomUUID()));
        ballotRepository.save(new Ballot(election, cand2, false, "recu_" + java.util.UUID.randomUUID()));
        ballotRepository.save(new Ballot(election, cand2, false, "recu_" + java.util.UUID.randomUUID()));
        ballotRepository.save(new Ballot(election, null, true, "recu_" + java.util.UUID.randomUUID())); // Vote blanc
    }

    @Test
    @DisplayName("Test 17 : Interdiction de publication prématurée des résultats")
    void testPrematureResultsForbidden() {
        // Avant dépouillement et publication, l'accès public doit être formellement rejeté
        assertThrows(BusinessException.class, () -> resultsService.getPublicResults(election.getId()));
    }

    @Test
    @DisplayName("Test 18 & 19 : Dépouillement exact, cohérence des totaux et génération du rapport PDF officiel")
    void testCountingAndPdfGeneration() {
        // 1. Dépouillement
        ElectionResultDto countResult = resultsService.countVotes(election.getId());
        assertNotNull(countResult);
        assertEquals(6, countResult.getVotantsTotal());
        assertEquals(5, countResult.getBulletinsValides());
        assertEquals(1, countResult.getBulletinsBlancs());

        // 2. Publication officielle
        ElectionResultDto published = resultsService.publishResults(election.getId());
        assertNotNull(published);

        // 3. Consultation publique désormais autorisée
        ElectionResultDto publicRes = resultsService.getPublicResults(election.getId());
        assertEquals(6, publicRes.getVotantsTotal());

        // 4. Génération du PDF certifié
        byte[] pdfBytes = pdfService.generateElectionReportPdf(election.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "Le fichier PDF doit avoir une taille valide");

        // Vérification de l'en-tête PDF standard (%PDF-)
        String pdfHeader = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", pdfHeader);
    }

    @Test
    @DisplayName("Test 20 : Traçabilité d'audit sans fuite de données sensibles")
    void testAuditLogDataProtection() {
        List<AuditEvent> events = auditService.getEventsByElection(election.getId());
        assertNotNull(events);

        for (AuditEvent event : events) {
            String details = event.getDetails();
            if (details != null) {
                // Aucun mot de passe en clair
                assertFalse(details.contains("PassSecure"), "Les logs ne doivent contenir aucun mot de passe");
                // Aucune clé secrète
                assertFalse(details.contains("404E6352"), "Les logs ne doivent contenir aucun secret JWT");
            }
        }
    }
}
