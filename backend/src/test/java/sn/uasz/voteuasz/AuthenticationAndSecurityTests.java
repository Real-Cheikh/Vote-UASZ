package sn.uasz.voteuasz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import sn.uasz.voteuasz.auth.AuthService;
import sn.uasz.voteuasz.auth.LoginRequest;
import sn.uasz.voteuasz.auth.LoginResponse;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserCategory;
import sn.uasz.voteuasz.users.UserRepository;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AuthenticationAndSecurityTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmail("test.etudiant@uasz.sn")) {
            Role roleElecteur = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur")));

            User user = new User();
            user.setEmail("test.etudiant@uasz.sn");
            user.setPasswordHash(passwordEncoder.encode("PassSecure2026!"));
            user.setNom("Sané");
            user.setPrenom("Ibrahima");
            user.setNumeroIdentification("ETUD-TEST-001");
            user.setCategorie(UserCategory.ETUDIANT);
            user.setActif(true);
            user.setBloque(false);
            user.setRoles(Set.of(roleElecteur));
            userRepository.save(user);
        }
    }

    @Test
    @DisplayName("Test 1 & 2 : Connexion valide et génération d'un JWT avec rôles conformes")
    void testValidLogin() {
        LoginRequest request = new LoginRequest("test.etudiant@uasz.sn", "PassSecure2026!");
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("test.etudiant@uasz.sn", response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_ELECTEUR"));
    }

    @Test
    @DisplayName("Test 1 (bis) : Rejet d'une tentative de connexion avec mot de passe invalide")
    void testInvalidLogin() {
        LoginRequest request = new LoginRequest("test.etudiant@uasz.sn", "MauvaisMotDePasse!");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Test Sécurité : Hachage des mots de passe avec Argon2id")
    void testArgon2idHashing() {
        String rawPassword = "MonMotDePasseSecret123!";
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotNull(encoded);
        // Les empreintes Argon2 commencent typiquement par "$argon2id$"
        assertTrue(encoded.startsWith("$argon2id$") || encoded.contains("argon2"),
                "Le mot de passe doit être chiffré selon la norme Argon2id");
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
        assertFalse(passwordEncoder.matches("AutreChose", encoded));
    }

    @Test
    @DisplayName("Test Inscription : Inscription réussie avec adresse institutionnelle @zig.univ.sn")
    void testRegisterWithInstitutionalEmailSuccess() {
        sn.uasz.voteuasz.auth.RegisterRequest request = new sn.uasz.voteuasz.auth.RegisterRequest(
                "cheikh.diop@zig.univ.sn",
                "PassSecure2026!",
                "Diop",
                "Cheikh",
                "ETUD-ZIG-2026-999",
                "+221770000000"
        );

        sn.uasz.voteuasz.users.UserDto dto = authService.register(request);
        assertNotNull(dto);
        assertEquals("cheikh.diop@zig.univ.sn", dto.getEmail());
        assertEquals("ETUDIANT", dto.getCategorie().name());
        assertTrue(dto.getRoles().contains("ROLE_ELECTEUR"));
        assertFalse(dto.getRoles().contains("ROLE_SUPER_ADMIN_ELECTORAL"));
        assertFalse(dto.getRoles().contains("ROLE_COMMISSION_ELECTORALE"));
        assertTrue(dto.isActif());
        assertFalse(dto.isBloque());
    }

    @Test
    @DisplayName("Test Inscription : Rejet strict des adresses email non institutionnelles ou frauduleuses")
    void testRegisterWithNonInstitutionalEmailRejected() {
        // Domaine externe (ex: @gmail.com)
        sn.uasz.voteuasz.auth.RegisterRequest gmailRequest = new sn.uasz.voteuasz.auth.RegisterRequest(
                "hacker@gmail.com",
                "PassSecure2026!",
                "Hacker",
                "Test",
                "ETUD-FAKE-001",
                null
        );
        assertThrows(sn.uasz.voteuasz.common.BusinessException.class, () -> authService.register(gmailRequest));

        // Domaine frauduleux avec suffixe d'usurpation (ex: @zig.univ.sn.fake.com)
        sn.uasz.voteuasz.auth.RegisterRequest fakeSubdomainRequest = new sn.uasz.voteuasz.auth.RegisterRequest(
                "fraud@zig.univ.sn.fake.com",
                "PassSecure2026!",
                "Fraud",
                "Test",
                "ETUD-FAKE-002",
                null
        );
        assertThrows(sn.uasz.voteuasz.common.BusinessException.class, () -> authService.register(fakeSubdomainRequest));
    }

    @Test
    @DisplayName("Test Inscription : Rejet des doublons d'email ou de numéro d'identification")
    void testRegisterDuplicateRejected() {
        sn.uasz.voteuasz.auth.RegisterRequest req1 = new sn.uasz.voteuasz.auth.RegisterRequest(
                "fatou.sow@zig.univ.sn",
                "PassSecure2026!",
                "Sow",
                "Fatou",
                "ETUD-ZIG-2026-888",
                null
        );
        authService.register(req1);

        // Doublon sur l'email
        sn.uasz.voteuasz.auth.RegisterRequest duplicateEmail = new sn.uasz.voteuasz.auth.RegisterRequest(
                "fatou.sow@zig.univ.sn",
                "AutrePass2026!",
                "AutreNom",
                "AutrePrenom",
                "ETUD-ZIG-2026-889",
                null
        );
        assertThrows(sn.uasz.voteuasz.common.BusinessException.class, () -> authService.register(duplicateEmail));

        // Doublon sur le numéro d'identification
        sn.uasz.voteuasz.auth.RegisterRequest duplicateId = new sn.uasz.voteuasz.auth.RegisterRequest(
                "autre.etudiant@zig.univ.sn",
                "AutrePass2026!",
                "AutreNom",
                "AutrePrenom",
                "ETUD-ZIG-2026-888",
                null
        );
        assertThrows(sn.uasz.voteuasz.common.BusinessException.class, () -> authService.register(duplicateId));
    }
}
