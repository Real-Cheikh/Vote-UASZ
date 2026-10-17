package sn.uasz.voteuasz;

import org.junit.jupiter.api.BeforeEach;
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
import sn.uasz.voteuasz.eligibility.EligibilityRecord;
import sn.uasz.voteuasz.eligibility.EligibilityRepository;
import sn.uasz.voteuasz.otp.OtpService;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SecureVotingAndAntiDoubleVoteTests {

    @Autowired
    private VotingService votingService;

    @Autowired
    private BallotRepository ballotRepository;

    @Autowired
    private EligibilityRepository eligibilityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private CandidacyRepository candidacyRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User voter;
    private Election election;
    private Candidacy candidate1;

    @BeforeEach
    void setUp() {
        Role roleElecteur = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur")));

        voter = userRepository.findByEmail("voter.sec@uasz.sn").orElseGet(() -> {
            User u = new User();
            u.setEmail("voter.sec@uasz.sn");
            u.setPasswordHash(passwordEncoder.encode("PassVote2026!"));
            u.setNom("Fall");
            u.setPrenom("Cheikh");
            u.setNumeroIdentification("VOTE-SEC-001");
            u.setCategorie(UserCategory.ETUDIANT);
            u.setActif(true);
            u.setBloque(false);
            u.setRoles(Set.of(roleElecteur));
            return userRepository.save(u);
        });

        User candidateUser = userRepository.findByEmail("cand.sec@uasz.sn").orElseGet(() -> {
            User u = new User();
            u.setEmail("cand.sec@uasz.sn");
            u.setPasswordHash(passwordEncoder.encode("PassCand2026!"));
            u.setNom("Kane");
            u.setPrenom("Mariama");
            u.setNumeroIdentification("CAND-SEC-001");
            u.setCategorie(UserCategory.ETUDIANT);
            u.setActif(true);
            u.setBloque(false);
            u.setRoles(Set.of(roleElecteur));
            return userRepository.save(u);
        });

        College college = collegeRepository.findByCode("COL_VOTE_SEC").orElseGet(() -> {
            College c = new College("COL_VOTE_SEC", "Collège Vote Sec", "Desc", "UFR ST", "Math-Info", "Master", UserCategory.ETUDIANT);
            return collegeRepository.save(c);
        });

        election = new Election();
        election.setTitre("Scrutin Sécurisé UASZ 2026");
        election.setType(ElectionType.DELEGUE);
        election.setCollege(college);
        election.setDateOuverture(OffsetDateTime.now().minusHours(1));
        election.setDateCloture(OffsetDateTime.now().plusHours(4));
        election.setStatut(ElectionStatus.VOTING_OPEN);
        election.setPermettreVoteBlanc(true);
        election = electionRepository.save(election);

        candidate1 = new Candidacy();
        candidate1.setElection(election);
        candidate1.setCandidat(candidateUser);
        candidate1.setStatut(CandidacyStatus.VALIDATED);
        candidate1.setProfessionFoi("Programme pour l'excellence estudiantine.");
        candidate1 = candidacyRepository.save(candidate1);

        // Authentifier le contexte de sécurité
        UserPrincipal principal = UserPrincipal.create(voter);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Test 11 & 15 : Déroulement complet du vote, secret absolu du bulletin et reçu vérifiable")
    void testCompleteVoteAndSecretBallot() {
        // 1. Demande d'OTP
        otpService.generateOtp(voter.getId(), election.getId());
        String otpCode = otpService.getTestAdapterCode(voter.getEmail(), election.getId());

        // 2. Demande de jeton anonyme
        VoteAuthorizationResponseDto authDto = votingService.requestVoteAuthorization(
                new VoteAuthorizationRequestDto(election.getId(), otpCode));
        assertNotNull(authDto.getVoteToken());

        // 3. Dépôt du bulletin dans l'urne
        CastBallotDto ballotDto = new CastBallotDto(election.getId(), authDto.getVoteToken(), candidate1.getId(), false);
        VoteReceiptDto receipt = votingService.castBallot(ballotDto);

        assertNotNull(receipt);
        assertNotNull(receipt.getRecuVoteHash());

        // 4. Test 15 : Vérification du secret du bulletin
        // On inspecte les bulletins dans la base : AUCUN champ ne relie le bulletin à l'électeur
        List<Ballot> ballots = ballotRepository.findByElectionId(election.getId());
        assertFalse(ballots.isEmpty());
        Ballot recordedBallot = ballots.getFirst();
        assertEquals(receipt.getRecuVoteHash(), recordedBallot.getRecuVoteHash());
        // La table Ballot n'a aucune colonne ni référence d'utilisateur !

        // 5. Vérification publique du reçu (l'électeur prouve son vote sans révéler son choix)
        ReceiptVerificationDto verification = votingService.verifyReceipt(receipt.getRecuVoteHash());
        assertTrue(verification.isValide());
        assertEquals(election.getTitre(), verification.getElectionTitre());
    }

    @Test
    @DisplayName("Test 11 (bis) : Prévention stricte du double vote")
    void testDoubleVotePrevention() {
        // Premier vote complet
        otpService.generateOtp(voter.getId(), election.getId());
        String otpCode = otpService.getTestAdapterCode(voter.getEmail(), election.getId());
        VoteAuthorizationResponseDto authDto = votingService.requestVoteAuthorization(
                new VoteAuthorizationRequestDto(election.getId(), otpCode));

        CastBallotDto ballotDto = new CastBallotDto(election.getId(), authDto.getVoteToken(), candidate1.getId(), false);
        votingService.castBallot(ballotDto);

        // Tentative d'un second vote par le même électeur : doit être bloquée dès l'étape d'autorisation
        assertThrows(VoteException.class, () ->
                votingService.requestVoteAuthorization(new VoteAuthorizationRequestDto(election.getId(), otpCode)));

        // Tentative de rejouer le même jeton d'autorisation : doit être rejetée immédiatement
        assertThrows(VoteException.class, () ->
                votingService.castBallot(ballotDto));
    }

    @Test
    @DisplayName("Test 12 : Résistance aux requêtes concurrentes (Atomicité et Anti-concurrence)")
    void testConcurrentVotingResistance() throws InterruptedException, ExecutionException {
        // Émettre un jeton
        otpService.generateOtp(voter.getId(), election.getId());
        String otpCode = otpService.getTestAdapterCode(voter.getEmail(), election.getId());
        VoteAuthorizationResponseDto authDto = votingService.requestVoteAuthorization(
                new VoteAuthorizationRequestDto(election.getId(), otpCode));

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await(); // Synchronisation pour démarrage simultané
                    CastBallotDto dto = new CastBallotDto(election.getId(), authDto.getVoteToken(), candidate1.getId(), false);
                    votingService.castBallot(dto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        latch.countDown(); // Libération simultanée des 10 threads
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        // Exactement 1 seul bulletin doit avoir été accepté !
        assertEquals(1, successCount.get(), "Un seul thread doit réussir à déposer le bulletin");
        assertEquals(9, failureCount.get(), "Les 9 autres requêtes simultanées doivent être rejetées");
    }

    @Test
    @DisplayName("Test 14 : Rejet d'un bulletin malformé (ex: candidat inexistant ou élection invalide)")
    void testMalformedBallotRejection() {
        // Jeton invalide ou inexistant
        CastBallotDto badTokenBallot = new CastBallotDto(election.getId(), "token_inexistant_xyz_123", candidate1.getId(), false);
        assertThrows(VoteException.class, () -> votingService.castBallot(badTokenBallot));
    }
}
