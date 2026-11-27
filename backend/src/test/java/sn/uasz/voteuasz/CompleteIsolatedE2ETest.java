package sn.uasz.voteuasz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import sn.uasz.voteuasz.candidacies.Candidacy;
import sn.uasz.voteuasz.candidacies.CandidacyRepository;
import sn.uasz.voteuasz.candidacies.CandidacyStatus;
import sn.uasz.voteuasz.colleges.College;
import sn.uasz.voteuasz.colleges.CollegeRepository;
import sn.uasz.voteuasz.common.VoteException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.elections.ElectionStatus;
import sn.uasz.voteuasz.elections.ElectionType;
import sn.uasz.voteuasz.eligibility.EligibilityDto;
import sn.uasz.voteuasz.eligibility.EligibilityService;
import sn.uasz.voteuasz.otp.OtpService;
import sn.uasz.voteuasz.reports.PdfReportGeneratorService;
import sn.uasz.voteuasz.results.ElectionResultDto;
import sn.uasz.voteuasz.results.ResultsService;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;
import sn.uasz.voteuasz.security.UserPrincipal;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserCategory;
import sn.uasz.voteuasz.users.UserRepository;
import sn.uasz.voteuasz.voting.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CompleteIsolatedE2ETest {

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CandidacyRepository candidacyRepository;

    @Autowired
    private EligibilityService eligibilityService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private VotingService votingService;

    @Autowired
    private BallotRepository ballotRepository;

    @Autowired
    private ResultsService resultsService;

    @Autowired
    private PdfReportGeneratorService pdfService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Cycle complet isolé : Candidat-électeur, vote valide, rejet double vote, dépouillement exact et PV PDF")
    void testCompleteE2EVotingCycle() {
        // 1. Initialisation des rôles cumulatifs
        Role roleElecteur = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur")));
        Role roleCandidat = roleRepository.findByName(RoleName.ROLE_CANDIDAT)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CANDIDAT, "Candidat")));

        // 2. Création d'un collège électoral de test
        College college = collegeRepository.findByCode("COL_ISOLATED_CYCLE").orElseGet(() ->
                collegeRepository.save(new College("COL_ISOLATED_CYCLE", "Collège Test Isolé", "Desc", "UFR ST", "Informatique", "L3", UserCategory.ETUDIANT)));

        // 3. Création du candidat qui possède impérativement le rôle électeur (Point 7)
        User candidateUser = userRepository.findByEmail("candidate.voter@uasz.sn").orElseGet(() -> {
            User u = new User("candidate.voter@uasz.sn", passwordEncoder.encode("Pass123!"), "Sane", "Ousmane", "ETU-CAND-VOT-01", "+22177000999", UserCategory.ETUDIANT);
            u.setRoles(Set.of(roleCandidat, roleElecteur));
            return userRepository.save(u);
        });

        // Vérification de la règle de cumul (Point 7)
        assertTrue(candidateUser.getRoles().contains(roleCandidat), "L'utilisateur doit posséder le rôle CANDIDAT");
        assertTrue(candidateUser.getRoles().contains(roleElecteur), "Le candidat doit conserver le rôle ELECTEUR");

        // 4. Création d'un scrutin isolé
        Election election = new Election();
        election.setTitre("Scrutin Isolé de Validation Finale 2026");
        election.setType(ElectionType.DELEGUE);
        election.setCollege(college);
        election.setDateOuverture(OffsetDateTime.now().minusHours(1));
        election.setDateCloture(OffsetDateTime.now().plusHours(2));
        election.setStatut(ElectionStatus.VOTING_OPEN);
        election.setPermettreVoteBlanc(true);
        Election savedElection = electionRepository.save(election);
        assertNotNull(savedElection.getId());

        // 5. Création et validation de la candidature
        Candidacy cand = new Candidacy();
        cand.setElection(savedElection);
        cand.setCandidat(candidateUser);
        cand.setStatut(CandidacyStatus.VALIDATED);
        cand.setProfessionFoi("Pour une gestion transparente et participative.");
        Candidacy savedCand = candidacyRepository.save(cand);
        assertNotNull(savedCand.getId());

        // 6. Vérification de l'éligibilité du candidat connecté à son scrutin (Point 7)
        EligibilityDto elig = eligibilityService.checkUserEligibility(savedElection.getId(), candidateUser.getId());
        assertTrue(elig.isEligible(), "Le candidat inscrit dans son collège doit être éligible au vote");
        assertFalse(elig.isAVote(), "Le candidat n'a pas encore voté");

        // 7. Authentification dans le contexte de sécurité
        UserPrincipal principal = UserPrincipal.create(candidateUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 8. Étape 1 du vote : Défi OTP fort à 6 chiffres
        otpService.generateOtp(candidateUser.getId(), savedElection.getId());
        String otpCode = otpService.getTestAdapterCode(candidateUser.getEmail(), savedElection.getId());
        assertNotNull(otpCode, "Le code OTP doit être généré");
        assertEquals(6, otpCode.length(), "Le code OTP doit comporter 6 chiffres");

        // 9. Étape 2 du vote : Émission du jeton d'autorisation anonyme
        VoteAuthorizationResponseDto authDto = votingService.requestVoteAuthorization(
                new VoteAuthorizationRequestDto(savedElection.getId(), otpCode));
        assertNotNull(authDto.getVoteToken(), "Un jeton de vote anonyme doit être délivré");

        // 10. Étape 3 du vote : Dépôt du bulletin dans l'urne (Point 2)
        CastBallotDto ballotDto = new CastBallotDto(savedElection.getId(), authDto.getVoteToken(), savedCand.getId(), false);
        VoteReceiptDto receipt = votingService.castBallot(ballotDto);
        assertNotNull(receipt, "Le reçu de vote doit être émis");
        assertNotNull(receipt.getRecuVoteHash(), "L'empreinte cryptographique du reçu doit être présente");

        // 11. Vérification publique du reçu de vote (Point 3)
        ReceiptVerificationDto verification = votingService.verifyReceipt(receipt.getRecuVoteHash());
        assertTrue(verification.isValide(), "Le reçu de vote doit être vérifié avec succès");
        assertEquals(savedElection.getTitre(), verification.getElectionTitre());
        assertNotNull(verification.getDateEnregistrement());

        // Vérification de la présence effective dans l'urne
        List<Ballot> ballotsInUrn = ballotRepository.findByElectionId(savedElection.getId());
        assertEquals(1, ballotsInUrn.size(), "L'urne doit contenir exactement 1 bulletin");
        assertEquals(receipt.getRecuVoteHash(), ballotsInUrn.getFirst().getRecuVoteHash());

        // 12. Tentative d'un second vote par le même électeur -> doit être REJETÉE (Point 4)
        assertThrows(VoteException.class, () ->
                votingService.requestVoteAuthorization(new VoteAuthorizationRequestDto(savedElection.getId(), otpCode)),
                "La seconde demande d'autorisation doit être rejetée car le droit de vote est consommé");

        // Tentative de réutilisation du jeton de vote consommé -> doit être REJETÉE (Point 4)
        assertThrows(VoteException.class, () ->
                votingService.castBallot(ballotDto),
                "Le réemploi d'un jeton d'autorisation déjà consommé doit être rejeté");

        // 13. Clôture officielle et Dépouillement des votes (Point 5)
        savedElection.setStatut(ElectionStatus.VOTING_CLOSED);
        electionRepository.save(savedElection);

        ElectionResultDto resultDto = resultsService.countVotes(savedElection.getId());
        assertNotNull(resultDto);
        assertEquals(1, resultDto.getVotantsTotal(), "Le dépouillement doit compter exactement 1 votant");
        assertEquals(1, resultDto.getBulletinsValides(), "Le dépouillement doit compter exactement 1 bulletin valide");
        assertEquals(0, resultDto.getBulletinsBlancs(), "Il doit y avoir 0 vote blanc");
        assertEquals(1, resultDto.getResultatsCandidats().size());
        assertEquals(1, resultDto.getResultatsCandidats().getFirst().getVoixObtenues(), "Le candidat doit avoir obtenu exactement 1 voix");
        assertEquals(100.0, resultDto.getResultatsCandidats().getFirst().getPourcentage(), "Le candidat doit avoir 100% des suffrages");

        // 14. Publication officielle des résultats (Point 6)
        ElectionResultDto published = resultsService.publishResults(savedElection.getId());
        assertEquals(sn.uasz.voteuasz.results.CountingStatus.PUBLISHED, published.getStatutDepouillement());
        assertEquals(1, published.getVotantsTotal());

        // 15. Génération du Procès-Verbal Officiel PDF (Point 6)
        byte[] pdfBytes = pdfService.generateElectionReportPdf(savedElection.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "Le PV officiel en PDF doit être généré et non vide");
        // Vérification de la signature 'PDF'
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);

        System.out.println(">>> SUCCÈS TOTAL : CYCLE COMPLET DE VOTE VALIDÉ DANS L'ENVIRONNEMENT ISOLÉ <<<");
        System.out.println("    - Élection ID : " + savedElection.getId());
        System.out.println("    - Candidat-Électeur ID : " + candidateUser.getId());
        System.out.println("    - Empreinte Reçu : " + receipt.getRecuVoteHash());
        System.out.println("    - Taille PV PDF : " + pdfBytes.length + " octets");
    }
}
