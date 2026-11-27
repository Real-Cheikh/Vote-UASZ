package sn.uasz.voteuasz;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import sn.uasz.voteuasz.colleges.College;
import sn.uasz.voteuasz.colleges.CollegeRepository;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.RateLimitExceededException;
import sn.uasz.voteuasz.common.VoteException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.elections.ElectionStatus;
import sn.uasz.voteuasz.elections.ElectionType;
import sn.uasz.voteuasz.otp.OtpChallenge;
import sn.uasz.voteuasz.otp.OtpChallengeResponseDto;
import sn.uasz.voteuasz.otp.OtpRepository;
import sn.uasz.voteuasz.otp.OtpService;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserCategory;
import sn.uasz.voteuasz.users.UserRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class OtpSecurityTests {

    @Autowired
    private OtpService otpService;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User demoUser;
    private User officialUser;
    private Election demoElection;
    private Election demoElectionEnseignant;
    private Election officialElection;
    private College testCollege;
    private College enseignantCollege;

    @BeforeEach
    void setUp() {
        otpRepository.deleteAll();

        Role roleElecteur = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur")));

        testCollege = collegeRepository.findByCode("COL_TEST_OTP").orElseGet(() -> {
            College c = new College("COL_TEST_OTP", "Collège Test OTP", "Desc", "UFR ST", "Informatique", "L3", UserCategory.ETUDIANT);
            return collegeRepository.save(c);
        });

        enseignantCollege = collegeRepository.findByCode("COL_TEST_ENS").orElseGet(() -> {
            College c = new College("COL_TEST_ENS", "Collège Enseignants OTP", "Desc", "UFR ST", "Informatique", "Tous", UserCategory.ENSEIGNANT);
            return collegeRepository.save(c);
        });

        // 1. Compte électeur de test / démonstration
        demoUser = userRepository.findByEmail("electeur.test.otp@zig.univ.sn").orElseGet(() -> {
            User u = new User();
            u.setEmail("electeur.test.otp@zig.univ.sn");
            u.setPasswordHash(passwordEncoder.encode("PassOtp123!"));
            u.setNom("Badji");
            u.setPrenom("Modou");
            u.setNumeroIdentification("OTP-TEST-001");
            u.setCategorie(UserCategory.ETUDIANT);
            u.setActif(true);
            u.setBloque(false);
            u.setRoles(Set.of(roleElecteur));
            return userRepository.save(u);
        });

        // 2. Compte officiel institutionnel réel (non démonstration)
        officialUser = userRepository.findByEmail("professeur.diagne@uasz.sn").orElseGet(() -> {
            User u = new User();
            u.setEmail("professeur.diagne@uasz.sn");
            u.setPasswordHash(passwordEncoder.encode("PassOfficiel123!"));
            u.setNom("Diagne");
            u.setPrenom("Ousmane");
            u.setNumeroIdentification("ENS-2026-0042");
            u.setCategorie(UserCategory.ENSEIGNANT);
            u.setActif(true);
            u.setBloque(false);
            u.setRoles(Set.of(roleElecteur));
            return userRepository.save(u);
        });

        // 3. Scrutin de démonstration / test (étudiants)
        demoElection = new Election();
        demoElection.setTitre("Scrutin Test Synthétique Audit 2026");
        demoElection.setDescription("Scrutin de démonstration pour banc d'essai et soutenance");
        demoElection.setType(ElectionType.DELEGUE);
        demoElection.setCollege(testCollege);
        demoElection.setDateOuverture(OffsetDateTime.now().minusHours(1));
        demoElection.setDateCloture(OffsetDateTime.now().plusHours(5));
        demoElection.setStatut(ElectionStatus.VOTING_OPEN);
        demoElection.setPermettreVoteBlanc(true);
        demoElection = electionRepository.save(demoElection);

        // 4. Scrutin de démonstration pour enseignants
        demoElectionEnseignant = new Election();
        demoElectionEnseignant.setTitre("Scrutin Test Synthétique Enseignants 2026");
        demoElectionEnseignant.setDescription("Scrutin de démonstration pour banc d'essai et soutenance");
        demoElectionEnseignant.setType(ElectionType.DIRECTEUR_UFR);
        demoElectionEnseignant.setCollege(enseignantCollege);
        demoElectionEnseignant.setDateOuverture(OffsetDateTime.now().minusHours(1));
        demoElectionEnseignant.setDateCloture(OffsetDateTime.now().plusHours(5));
        demoElectionEnseignant.setStatut(ElectionStatus.VOTING_OPEN);
        demoElectionEnseignant.setPermettreVoteBlanc(true);
        demoElectionEnseignant = electionRepository.save(demoElectionEnseignant);

        // 5. Scrutin officiel institutionnel (aucun mot-clé de test/synthétique)
        officialElection = new Election();
        officialElection.setTitre("Élection Officielle des Représentants au Conseil d'Administration 2026");
        officialElection.setDescription("Scrutin universitaire officiel régalien de l'UASZ");
        officialElection.setType(ElectionType.DELEGUE);
        officialElection.setCollege(testCollege);
        officialElection.setDateOuverture(OffsetDateTime.now().minusHours(1));
        officialElection.setDateCloture(OffsetDateTime.now().plusHours(5));
        officialElection.setStatut(ElectionStatus.VOTING_OPEN);
        officialElection.setPermettreVoteBlanc(true);
        officialElection = electionRepository.save(officialElection);
    }

    @Test
    @DisplayName("Sécurité 1 : Les scrutins officiels refusent tout succès fictif sans SMTP configuré")
    void testOfficialElectionWithoutSmtpRefusesFictitiousSuccess() {
        // En l'absence de serveur SMTP réel configuré, le système refuse impérativement d'annoncer un succès
        BusinessException ex = assertThrows(BusinessException.class, () ->
                otpService.generateOtp(demoUser.getId(), officialElection.getId()));

        assertTrue(ex.getMessage().contains("SMTP"), "Le message d'erreur doit indiquer explicitement l'absence de SMTP");
        // Aucun défi ne doit être créé ni aucun code fuité
        assertTrue(otpRepository.findLatestActiveChallenge(demoUser.getId(), officialElection.getId()).isEmpty());
    }

    @Test
    @DisplayName("Sécurité 2 : Les comptes officiels institutionnels refusent tout succès fictif sans SMTP")
    void testOfficialUserWithoutSmtpRefusesFictitiousSuccess() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                otpService.generateOtp(officialUser.getId(), demoElectionEnseignant.getId()));

        assertTrue(ex.getMessage().contains("SMTP"));
        assertTrue(otpRepository.findLatestActiveChallenge(officialUser.getId(), demoElectionEnseignant.getId()).isEmpty());
    }

    @Test
    @DisplayName("Sécurité 3 : Le mode SIMULATION est strictement actif uniquement pour demoUser ET demoElection")
    void testDemonstrationScrutinAndDemoUserReturnsSimulationCode() {
        OtpChallengeResponseDto challenge = otpService.generateOtp(demoUser.getId(), demoElection.getId());
        assertNotNull(challenge);
        assertEquals(300, challenge.getValiditeSecondes());
        assertEquals("SIMULATION", challenge.getMode());
        assertNotNull(challenge.getSimulationCode());
        assertEquals(6, challenge.getSimulationCode().length());
        assertTrue(challenge.getMessage().contains("Mode Démonstration"));

        String adapterCode = otpService.getTestAdapterCode(demoUser.getEmail(), demoElection.getId());
        assertEquals(adapterCode, challenge.getSimulationCode());
    }

    @Test
    @DisplayName("Sécurité 4 : Hachage cryptographique HMAC-SHA-256 dans la base de données")
    void testOtpHashingHmacSha256InDatabase() throws Exception {
        OtpChallengeResponseDto challenge = otpService.generateOtp(demoUser.getId(), demoElection.getId());
        String code = otpService.getTestAdapterCode(demoUser.getEmail(), demoElection.getId());

        OtpChallenge entity = otpRepository.findLatestActiveChallenge(demoUser.getId(), demoElection.getId()).orElseThrow();
        assertNotNull(entity.getCodeHache());
        assertEquals(64, entity.getCodeHache().length(), "Le hash en base doit être une empreinte hex de 64 caractères");
        assertNotEquals(code, entity.getCodeHache(), "Le code OTP en clair ne doit JAMAIS être stocké en base");
        assertFalse(entity.isConsomme());

        // Vérifier que le hash correspond bien à HMAC-SHA256 avec la clé de test
        String secret = "9f82c4e3b1a7d6e5f8a2c1b4e7d9c2a1b4e7d9c2a1b4e7d9c2a1b4e7d9c2a1b4";
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(keySpec);
        byte[] expectedHash = hmac.doFinal(code.getBytes(StandardCharsets.UTF_8));
        String expectedHex = HexFormat.of().formatHex(expectedHash);

        assertEquals(expectedHex, entity.getCodeHache(), "Le hash doit correspondre à l'algorithme HMAC-SHA-256");
    }

    @Test
    @DisplayName("Sécurité 4-bis : Rétrocompatibilité avec les défis existants hachés en legacy SHA-256")
    void testLegacySha256BackwardCompatibility() throws Exception {
        String legacyCode = "456789";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(legacyCode.getBytes(StandardCharsets.UTF_8));
        String legacyHex = HexFormat.of().formatHex(hash);

        OtpChallenge legacyChallenge = new OtpChallenge();
        legacyChallenge.setUser(demoUser);
        legacyChallenge.setElection(demoElection);
        legacyChallenge.setCodeHache(legacyHex);
        legacyChallenge.setDateExpiration(OffsetDateTime.now().plusMinutes(5));
        legacyChallenge.setTentativesRestantes(3);
        legacyChallenge.setConsomme(false);
        legacyChallenge.setDateCreation(OffsetDateTime.now());
        otpRepository.save(legacyChallenge);

        // La vérification doit accepter le hash legacy SHA-256 pour assurer une transition sans rupture
        boolean verified = otpService.verifyOtp(demoUser.getId(), demoElection.getId(), legacyCode);
        assertTrue(verified, "L'algorithme de transition doit valider les anciens défis hachés en SHA-256 standard");
    }

    @Test
    @DisplayName("Sécurité 5 : Rate Limiting — Délai de recharge strict (Cooldown 10s) rejeté avec HTTP 429")
    void testRateLimitCooldown() {
        otpService.generateOtp(demoUser.getId(), demoElection.getId());

        // Deuxième appel immédiat (avant écoulement des 10s de cooldown configurées dans application-test.yml)
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class, () ->
                otpService.generateOtp(demoUser.getId(), demoElection.getId()));

        assertTrue(ex.getRetryAfterSeconds() > 0, "Retry-After doit être strictement positif");
        assertTrue(ex.getMessage().toLowerCase().contains("veuillez patienter"));
    }

    @Test
    @DisplayName("Sécurité 6 : Rate Limiting — Fenêtre glissante (Max 5 défis par fenêtre) rejetée avec HTTP 429")
    void testRateLimitSlidingWindow() {
        // Simuler 5 défis créés espacés de plus que le cooldown mais dans la fenêtre des 15 minutes
        for (int i = 0; i < 5; i++) {
            OtpChallenge c = new OtpChallenge();
            c.setUser(demoUser);
            c.setElection(demoElection);
            c.setCodeHache("hash_" + i);
            c.setDateExpiration(OffsetDateTime.now().plusMinutes(5));
            c.setTentativesRestantes(3);
            c.setConsomme(true);
            // dateCreation antérieure au cooldown (ex: il y a 30s) mais dans la fenêtre (15 min)
            c.setDateCreation(OffsetDateTime.now().minusSeconds(30 + i * 10));
            otpRepository.save(c);
        }

        // Le 6ème appel doit lever RateLimitExceededException
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class, () ->
                otpService.generateOtp(demoUser.getId(), demoElection.getId()));

        assertTrue(ex.getMessage().contains("Nombre maximal de demandes"));
    }

    @Test
    @DisplayName("Sécurité 7 : Rejet immédiat d'un OTP expiré après 5 minutes")
    void testExpiredOtpRejection() {
        otpService.generateOtp(demoUser.getId(), demoElection.getId());
        String code = otpService.getTestAdapterCode(demoUser.getEmail(), demoElection.getId());

        // Simulation de l'expiration en base (date d'expiration passée)
        OtpChallenge challenge = otpRepository.findLatestActiveChallenge(demoUser.getId(), demoElection.getId()).orElseThrow();
        challenge.setDateExpiration(OffsetDateTime.now().minusMinutes(6));
        otpRepository.save(challenge);

        VoteException ex = assertThrows(VoteException.class, () ->
                otpService.verifyOtp(demoUser.getId(), demoElection.getId(), code));

        assertTrue(ex.getMessage().contains("expiré"));
    }

    @Test
    @DisplayName("Sécurité 8 : Rejet strict d'un code OTP déjà consommé (Usage unique)")
    void testReusedOtpRejection() {
        otpService.generateOtp(demoUser.getId(), demoElection.getId());
        String code = otpService.getTestAdapterCode(demoUser.getEmail(), demoElection.getId());

        // Première utilisation : valide
        assertTrue(otpService.verifyOtp(demoUser.getId(), demoElection.getId(), code));

        // Seconde utilisation du même code : doit être rejetée car consommé
        assertThrows(VoteException.class, () ->
                otpService.verifyOtp(demoUser.getId(), demoElection.getId(), code));
    }

    @Test
    @DisplayName("Sécurité 9 : Blocage strict après 3 tentatives erronées (Anti-bruteforce)")
    void testAttemptThrottling() {
        otpService.generateOtp(demoUser.getId(), demoElection.getId());

        // 3 échecs successifs avec de faux codes
        assertThrows(VoteException.class, () -> otpService.verifyOtp(demoUser.getId(), demoElection.getId(), "000000"));
        assertThrows(VoteException.class, () -> otpService.verifyOtp(demoUser.getId(), demoElection.getId(), "111111"));
        assertThrows(VoteException.class, () -> otpService.verifyOtp(demoUser.getId(), demoElection.getId(), "222222"));

        // 4ème tentative même avec le bon code : doit être bloquée
        String rightCode = otpService.getTestAdapterCode(demoUser.getEmail(), demoElection.getId());
        VoteException ex = assertThrows(VoteException.class, () ->
                otpService.verifyOtp(demoUser.getId(), demoElection.getId(), rightCode));

        assertTrue(ex.getMessage().contains("maximal") || ex.getMessage().contains("Aucun défi") || ex.getMessage().contains("bloqu"));
    }

    @Test
    @DisplayName("Sécurité 10 : Isolation transactionnelle REQUIRES_NEW — Décrément persistant même si la transaction appelante roll back")
    void testFailedAttemptPersistedEvenIfOuterTransactionRollsBack() {
        otpService.generateOtp(demoUser.getId(), demoElection.getId());

        // Simuler une transaction appelante (comme VotingService.requestVoteAuthorization) qui échoue et rollback
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            // Tentative erronée dans l'OTP
            otpService.verifyOtp(demoUser.getId(), demoElection.getId(), "999999");
        } catch (VoteException ignored) {
            // attendu
        } finally {
            // Rollback explicite de la transaction appelante englobante
            transactionManager.rollback(status);
        }

        // Grâce à REQUIRES_NEW, la décrémentation des tentatives dans OtpService a été commitée dans sa propre transaction
        OtpChallenge challengeAfterRollback = otpRepository.findLatestActiveChallenge(demoUser.getId(), demoElection.getId()).orElseThrow();
        assertEquals(2, challengeAfterRollback.getTentativesRestantes(),
                "Le compteur de tentatives restantes DOIT avoir été décrémenté à 2 malgré le rollback de la transaction appelante");
    }
}
