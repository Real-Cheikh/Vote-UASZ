package sn.uasz.voteuasz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import sn.uasz.voteuasz.auth.AuthService;
import sn.uasz.voteuasz.auth.ChangePasswordDto;
import sn.uasz.voteuasz.auth.LoginRequest;
import sn.uasz.voteuasz.auth.LoginResponse;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;
import sn.uasz.voteuasz.security.UserPrincipal;
import sn.uasz.voteuasz.users.*;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class UserSecurityAndCommissionTests {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User superAdmin;
    private User realStudent;
    private User demoEtudiant;

    @BeforeEach
    void setUp() {
        Role roleSuperAdmin = roleRepository.findByName(RoleName.ROLE_SUPER_ADMIN_ELECTORAL)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_SUPER_ADMIN_ELECTORAL, "Super-admin")));
        Role roleCommission = roleRepository.findByName(RoleName.ROLE_COMMISSION_ELECTORALE)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_COMMISSION_ELECTORALE, "Commission")));
        Role roleElecteur = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur")));

        // 1. Super Admin
        superAdmin = userRepository.findByEmail("admin.test@uasz.sn").orElseGet(() -> {
            User u = new User("admin.test@uasz.sn", passwordEncoder.encode("TestPassword123!"), "Admin", "Super", "ADMIN-001", "+221770000001", UserCategory.PAT);
            u.setRoles(Set.of(roleSuperAdmin));
            return userRepository.save(u);
        });

        // 2. Compte réel inscrit
        realStudent = userRepository.findByEmail("real.student@zig.univ.sn").orElseGet(() -> {
            User u = new User("real.student@zig.univ.sn", passwordEncoder.encode("StudentPass123!"), "Ndiaye", "Moussa", "REAL-ETU-001", "+221770000002", UserCategory.ETUDIANT);
            u.setRoles(Set.of(roleElecteur));
            return userRepository.save(u);
        });

        // 3. Compte de démonstration
        demoEtudiant = userRepository.findByEmail("etudiant.demo.test@zig.univ.sn").orElseGet(() -> {
            User u = new User("etudiant.demo.test@zig.univ.sn", passwordEncoder.encode("DemoPass123!"), "Demo", "Etudiant", "DEMO-001", "+221770000003", UserCategory.ETUDIANT);
            u.setRoles(Set.of(roleElecteur));
            return userRepository.save(u);
        });

        // Authentifier le super-admin par défaut dans le contexte
        UserPrincipal principal = UserPrincipal.create(superAdmin);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Test 1 : Création sécurisée d'un membre de commission avec mot de passe temporaire robuste")
    void testCreateCommissionMember() {
        CommissionMemberCreateDto dto = new CommissionMemberCreateDto();
        dto.setEmail("membre.commission.audit@uasz.sn");
        dto.setNom("Ba");
        dto.setPrenom("Oumar");
        dto.setNumeroIdentification("COM-MEMBRE-001");
        dto.setTelephone("+221771234567");
        dto.setCategorie(UserCategory.ENSEIGNANT);

        CommissionMemberResponseDto created = userService.createCommissionMember(dto);

        assertNotNull(created.getId());
        assertEquals("membre.commission.audit@uasz.sn", created.getEmail());
        assertTrue(created.getRoles().contains(RoleName.ROLE_COMMISSION_ELECTORALE.name()));
        assertFalse(created.getRoles().contains(RoleName.ROLE_SUPER_ADMIN_ELECTORAL.name()), "La commission ne doit jamais hériter du rôle Super Admin");

        assertNotNull(created.getMotDePasseTemporaire());
        assertTrue(created.getMotDePasseTemporaire().length() >= 14, "Le mot de passe temporaire doit comporter au moins 14 caractères");

        User inDb = userRepository.findById(created.getId()).orElseThrow();
        assertTrue(inDb.isDoitChangerMotDePasse(), "L'obligation de changer le mot de passe doit être active");
        assertTrue(passwordEncoder.matches(created.getMotDePasseTemporaire(), inDb.getPasswordHash()));
        assertNotEquals(created.getMotDePasseTemporaire(), inDb.getPasswordHash(), "Le mot de passe en clair ne doit jamais être stocké");
    }

    @Test
    @DisplayName("Test 2 : Premier login avec mot de passe temporaire et obligation de renouvellement")
    void testLoginWithTemporaryPasswordRequiresChange() {
        CommissionMemberCreateDto dto = new CommissionMemberCreateDto();
        dto.setEmail("membre2.commission@uasz.sn");
        dto.setNom("Diallo");
        dto.setPrenom("Mariama");
        dto.setNumeroIdentification("COM-MEMBRE-002");
        dto.setCategorie(UserCategory.PAT);

        CommissionMemberResponseDto created = userService.createCommissionMember(dto);

        // Connexion avec le mot de passe temporaire
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("membre2.commission@uasz.sn");
        loginReq.setPassword(created.getMotDePasseTemporaire());

        LoginResponse loginResp = authService.login(loginReq);
        assertNotNull(loginResp.getToken());
        assertTrue(loginResp.isDoitChangerMotDePasse(), "Le login doit signaler que le mot de passe doit être renouvelé");

        // Renouvellement avec un mot de passe robuste conforme
        ChangePasswordDto changeDto = new ChangePasswordDto();
        changeDto.setOldPassword(created.getMotDePasseTemporaire());
        changeDto.setNewPassword("NouveauPassRobust2026@!");
        changeDto.setConfirmPassword("NouveauPassRobust2026@!");

        userService.changePassword(created.getId(), changeDto);

        User updatedUser = userRepository.findById(created.getId()).orElseThrow();
        assertFalse(updatedUser.isDoitChangerMotDePasse(), "Après renouvellement, l'obligation doit être levée");
        assertTrue(passwordEncoder.matches("NouveauPassRobust2026@!", updatedUser.getPasswordHash()));

        // Connexion avec le nouveau mot de passe
        loginReq.setPassword("NouveauPassRobust2026@!");
        LoginResponse loginResp2 = authService.login(loginReq);
        assertFalse(loginResp2.isDoitChangerMotDePasse());
    }

    @Test
    @DisplayName("Test 3 : Blocage strict contre l'escalade de privilèges et auto-attribution de rôles")
    void testPrivilegeEscalationPrevented() {
        // Le super admin ne peut pas auto-modifier ses propres rôles
        assertThrows(BusinessException.class, () ->
                userService.updateUserRoles(superAdmin.getId(), Set.of(RoleName.ROLE_ELECTEUR.name())));

        // Tentative d'auto-modification par un utilisateur lambda
        UserPrincipal studentPrincipal = UserPrincipal.create(realStudent);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(studentPrincipal, null, studentPrincipal.getAuthorities()));

        assertThrows(BusinessException.class, () ->
                userService.updateUserRoles(realStudent.getId(), Set.of(RoleName.ROLE_SUPER_ADMIN_ELECTORAL.name())));
    }

    @Test
    @DisplayName("Test 4 : Désactivation des comptes démo avec préservation des comptes réels et institutionnels")
    void testDeactivateDemoAccounts() {
        Map<String, Object> result = userService.deactivateDemoAccounts();
        assertNotNull(result);
        assertTrue((int) result.get("deactivatedCount") >= 1);

        // Le compte démo est maintenant inactif et bloqué
        User demoInDb = userRepository.findById(demoEtudiant.getId()).orElseThrow();
        assertFalse(demoInDb.isActif());
        assertTrue(demoInDb.isBloque());

        // Connexion bloquée pour le compte démo désactivé
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(demoEtudiant.getEmail());
        loginReq.setPassword("DemoPass123!");

        assertThrows(BadCredentialsException.class, () -> authService.login(loginReq));

        // Les comptes réels et super admin sont strictement préservés et actifs
        User adminInDb = userRepository.findById(superAdmin.getId()).orElseThrow();
        assertTrue(adminInDb.isActif());
        assertFalse(adminInDb.isBloque());

        User studentInDb = userRepository.findById(realStudent.getId()).orElseThrow();
        assertTrue(studentInDb.isActif());
        assertFalse(studentInDb.isBloque());
    }

    @Test
    @DisplayName("Test 5 : Purge et archivage sécurisé : suppression des comptes démo sans dépendance et archivage mort irréversible")
    void testPurgeOrArchiveDemoAccounts() {
        // 1. Créer un compte démo sans dépendance
        Role roleElecteur = roleRepository.findByName(RoleName.ROLE_ELECTEUR).orElseThrow();
        User orphanDemo = new User("orphan.demo@zig.univ.sn", passwordEncoder.encode("Pass123!"), "Orphan", "Demo", "ORPHAN-001", "+221770000099", UserCategory.ETUDIANT);
        orphanDemo.setRoles(Set.of(roleElecteur));
        orphanDemo = userRepository.save(orphanDemo);
        Long orphanId = orphanDemo.getId();

        // 2. Exécuter la purge et archivage
        Map<String, Object> result = userService.purgeOrArchiveDemoAccounts();
        assertNotNull(result);

        // 3. Vérifier que le compte sans dépendance a bien été SUPPRIMÉ
        assertTrue(userRepository.findById(orphanId).isEmpty(), "Le compte de démonstration orphelin doit être supprimé définitivement");

        // 4. Vérifier que les comptes institutionnels et réels restent intacts
        assertTrue(userRepository.findByEmail(superAdmin.getEmail()).isPresent(), "Le super admin officiel doit être préservé");
        assertTrue(userRepository.findByEmail(realStudent.getEmail()).isPresent(), "L'étudiant réel officiel doit être préservé");
    }
}
