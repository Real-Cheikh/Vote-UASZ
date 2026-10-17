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
}
