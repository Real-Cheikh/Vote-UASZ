package sn.uasz.voteuasz;

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

    private User testUser;
    private Election testElection;

    @BeforeEach
    void setUp() {
        Role roleElecteur = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur")));

        testUser = userRepository.findByEmail("electeur.otp@uasz.sn").orElseGet(() -> {
            User u = new User();
            u.setEmail("electeur.otp@uasz.sn");
            u.setPasswordHash(passwordEncoder.encode("PassOtp123!"));
            u.setNom("Badji");
            u.setPrenom("Modou");
            u.setNumeroIdentification("OTP-USER-001");
            u.setCategorie(UserCategory.ETUDIANT);
            u.setActif(true);
            u.setBloque(false);
            u.setRoles(Set.of(roleElecteur));
            return userRepository.save(u);
        });

        College college = collegeRepository.findByCode("COL_TEST_OTP").orElseGet(() -> {
            College c = new College("COL_TEST_OTP", "Collège Test OTP", "Desc", "UFR ST", "Informatique", "L3", UserCategory.ETUDIANT);
            return collegeRepository.save(c);
        });

        testElection = new Election();
        testElection.setTitre("Élection Test OTP");
        testElection.setType(ElectionType.DELEGUE);
        testElection.setCollege(college);
        testElection.setDateOuverture(OffsetDateTime.now().minusHours(1));
        testElection.setDateCloture(OffsetDateTime.now().plusHours(5));
        testElection.setStatut(ElectionStatus.VOTING_OPEN);
        testElection.setPermettreVoteBlanc(true);
        testElection = electionRepository.save(testElection);
    }

    @Test
    @DisplayName("Test 7 : Génération et validation d'un OTP légitime")
    void testValidOtpVerification() {
        OtpChallengeResponseDto challenge = otpService.generateOtp(testUser.getId(), testElection.getId());
        assertNotNull(challenge);
        assertEquals(300, challenge.getValiditeSecondes());

        String code = otpService.getTestAdapterCode(testUser.getEmail(), testElection.getId());
        assertNotNull(code);
        assertEquals(6, code.length());

        boolean valid = otpService.verifyOtp(testUser.getId(), testElection.getId(), code);
        assertTrue(valid);
    }

    @Test
    @DisplayName("Test 8 : Rejet immédiat d'un OTP expiré après 5 minutes")
    void testExpiredOtpRejection() {
        otpService.generateOtp(testUser.getId(), testElection.getId());
        String code = otpService.getTestAdapterCode(testUser.getEmail(), testElection.getId());

        // Simulation de l'expiration en base (date d'expiration passée de plus de 5 minutes)
        OtpChallenge challenge = otpRepository.findLatestActiveChallenge(testUser.getId(), testElection.getId()).orElseThrow();
        challenge.setDateExpiration(OffsetDateTime.now().minusMinutes(6));
        otpRepository.save(challenge);

        VoteException ex = assertThrows(VoteException.class, () ->
                otpService.verifyOtp(testUser.getId(), testElection.getId(), code));

        assertTrue(ex.getMessage().contains("expiré"));
    }

    @Test
    @DisplayName("Test 9 : Rejet d'un code OTP déjà consommé (Usage unique strict)")
    void testReusedOtpRejection() {
        otpService.generateOtp(testUser.getId(), testElection.getId());
        String code = otpService.getTestAdapterCode(testUser.getEmail(), testElection.getId());

        // Première utilisation : valide
        assertTrue(otpService.verifyOtp(testUser.getId(), testElection.getId(), code));

        // Seconde utilisation du même code : doit être rejetée car consommé
        assertThrows(VoteException.class, () ->
                otpService.verifyOtp(testUser.getId(), testElection.getId(), code));
    }

    @Test
    @DisplayName("Test 10 : Blocage après dépassement du nombre maximal de tentatives (3 échecs)")
    void testAttemptThrottling() {
        otpService.generateOtp(testUser.getId(), testElection.getId());

        // 3 échecs successifs avec de faux codes
        assertThrows(VoteException.class, () -> otpService.verifyOtp(testUser.getId(), testElection.getId(), "000000"));
        assertThrows(VoteException.class, () -> otpService.verifyOtp(testUser.getId(), testElection.getId(), "111111"));
        assertThrows(VoteException.class, () -> otpService.verifyOtp(testUser.getId(), testElection.getId(), "222222"));

        // 4ème tentative même avec le bon code : doit être bloquée
        String rightCode = otpService.getTestAdapterCode(testUser.getEmail(), testElection.getId());
        VoteException ex = assertThrows(VoteException.class, () ->
                otpService.verifyOtp(testUser.getId(), testElection.getId(), rightCode));

        assertTrue(ex.getMessage().contains("maximal") || ex.getMessage().contains("Aucun défi") || ex.getMessage().contains("bloqu"));
    }
}
