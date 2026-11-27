package sn.uasz.voteuasz;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import sn.uasz.voteuasz.colleges.College;
import sn.uasz.voteuasz.colleges.CollegeRepository;
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

import java.time.OffsetDateTime;
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

    private User demoUser;
    private User officialUser;
    private Election demoElection;
    private Election demoElectionEnseignant;
    private Election officialElection;
    private College testCollege;
    private College enseignantCollege;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Sécurité 1 : Les scrutins officiels ne reçoivent JAMAIS de simulationCode en clair")
    void testOfficialElectionNeverReturnsSimulationCode() throws Exception {
        // Même pour un compte de test, un scrutin officiel ne divulgue JAMAIS le code OTP dans l'API
        OtpChallengeResponseDto challenge = otpService.generateOtp(demoUser.getId(), officialElection.getId());
        assertNotNull(challenge);
        assertEquals("EMAIL", challenge.getMode());
        assertNull(challenge.getSimulationCode(), "simulationCode doit impérativement être NULL pour un scrutin officiel");
        assertFalse(challenge.getMessage().contains("Mode Démonstration"));

        // Vérification de la sérialisation JSON : la clé simulationCode doit être totalement absente du JSON
        String json = objectMapper.writeValueAsString(challenge);
        assertFalse(json.contains("\"simulationCode\""), "Le champ simulationCode ne doit jamais être sérialisé en mode EMAIL");
    }

    @Test
    @DisplayName("Sécurité 2 : Les comptes officiels institutionnels ne reçoivent JAMAIS de simulationCode")
    void testOfficialUserNeverReturnsSimulationCode() throws Exception {
        // Même sur un scrutin de démonstration, un compte officiel ne reçoit jamais d'OTP en clair dans l'API
        OtpChallengeResponseDto challenge = otpService.generateOtp(officialUser.getId(), demoElectionEnseignant.getId());
        assertNotNull(challenge);
        assertEquals("EMAIL", challenge.getMode());
        assertNull(challenge.getSimulationCode(), "simulationCode doit être NULL pour un utilisateur institutionnel");

        String json = objectMapper.writeValueAsString(challenge);
        assertFalse(json.contains("\"simulationCode\""));
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
    @DisplayName("Sécurité 4 : Hachage cryptographique SHA-256 dans la base de données")
    void testOtpHashingSha256InDatabase() {
        OtpChallengeResponseDto challenge = otpService.generateOtp(demoUser.getId(), demoElection.getId());
        String code = otpService.getTestAdapterCode(demoUser.getEmail(), demoElection.getId());

        OtpChallenge entity = otpRepository.findLatestActiveChallenge(demoUser.getId(), demoElection.getId()).orElseThrow();
        assertNotNull(entity.getCodeHache());
        assertEquals(64, entity.getCodeHache().length(), "Le hash en base doit être une empreinte SHA-256 de 64 caractères hex");
        assertNotEquals(code, entity.getCodeHache(), "Le code OTP en clair ne doit JAMAIS être stocké en base de données");
        assertFalse(entity.isConsomme());
    }

    @Test
    @DisplayName("Sécurité 5 : Rejet immédiat d'un OTP expiré après 5 minutes")
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
    @DisplayName("Sécurité 6 : Rejet strict d'un code OTP déjà consommé (Usage unique)")
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
    @DisplayName("Sécurité 7 : Blocage strict après 3 tentatives erronées (Anti-bruteforce)")
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
}
