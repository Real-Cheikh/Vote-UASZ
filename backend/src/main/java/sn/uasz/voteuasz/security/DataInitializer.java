package sn.uasz.voteuasz.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserCategory;
import sn.uasz.voteuasz.users.UserRepository;
import sn.uasz.voteuasz.users.UserService;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Value("${app.security.initial-admin.email:admin.electoral@uasz.sn}")
    private String adminEmail;

    @Value("${app.security.initial-admin.password:AdminUasz2026!Securise}")
    private String adminPassword;

    @Value("${app.security.initial-admin.nom:Super Admin}")
    private String adminNom;

    @Value("${app.security.initial-admin.prenom:Commission UASZ}")
    private String adminPrenom;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserService userService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Initialisation des rôles fondamentaux
        Role roleSuperAdmin = getOrCreateRole(RoleName.ROLE_SUPER_ADMIN_ELECTORAL, "Super-administrateur de la plateforme");
        Role roleCommission = getOrCreateRole(RoleName.ROLE_COMMISSION_ELECTORALE, "Membre de la commission électorale");
        getOrCreateRole(RoleName.ROLE_CANDIDAT, "Candidat aux scrutins");
        getOrCreateRole(RoleName.ROLE_ELECTEUR, "Électeur universitaire");

        // 2. Initialisation du premier Super Administrateur si non existant
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setNom(adminNom);
            admin.setPrenom(adminPrenom);
            admin.setNumeroIdentification("ADMIN-UASZ-001");
            admin.setTelephone("+221339910000");
            admin.setCategorie(UserCategory.PAT);
            admin.setActif(true);
            admin.setBloque(false);
            admin.setRoles(Set.of(roleSuperAdmin, roleCommission));

            userRepository.save(admin);
            log.info("Compte initial Super-Administrateur créé avec succès : {}", adminEmail);
        }

        // 3. Purge et archivage sécurisé des comptes de démonstration résiduels
        userService.purgeOrArchiveDemoAccounts();
    }

    private Role getOrCreateRole(RoleName name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name, description)));
    }
}
