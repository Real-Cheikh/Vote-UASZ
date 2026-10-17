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

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.initial-admin.email:admin.electoral@uasz.sn}")
    private String adminEmail;

    @Value("${app.security.initial-admin.password:AdminUasz2026!Securise}")
    private String adminPassword;

    @Value("${app.security.initial-admin.nom:Super Admin}")
    private String adminNom;

    @Value("${app.security.initial-admin.prenom:Commission UASZ}")
    private String adminPrenom;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Initialisation des rôles fondamentaux
        Role roleSuperAdmin = getOrCreateRole(RoleName.ROLE_SUPER_ADMIN_ELECTORAL, "Super-administrateur de la plateforme");
        Role roleCommission = getOrCreateRole(RoleName.ROLE_COMMISSION_ELECTORALE, "Membre de la commission électorale");
        Role roleCandidat = getOrCreateRole(RoleName.ROLE_CANDIDAT, "Candidat aux scrutins");
        Role roleElecteur = getOrCreateRole(RoleName.ROLE_ELECTEUR, "Électeur universitaire");

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

        // 3. Comptes de démonstration académique si non présents
        initializeDemoUser("etudiant1@zig.univ.sn", "Etudiant2026!", "Diop", "Moussa", "20210045", UserCategory.ETUDIANT, Set.of(roleElecteur));
        initializeDemoUser("candidat1@zig.univ.sn", "Candidat2026!", "Sow", "Awa", "20200112", UserCategory.ETUDIANT, Set.of(roleElecteur, roleCandidat));
        initializeDemoUser("enseignant1@uasz.sn", "Enseignant2026!", "Ndiaye", "Pr. Ousmane", "ENS-9902", UserCategory.ENSEIGNANT, Set.of(roleElecteur));
        initializeDemoUser("commission@uasz.sn", "Commission2026!", "Sarr", "Fatou", "COM-2026", UserCategory.PAT, Set.of(roleCommission, roleElecteur));
    }

    private Role getOrCreateRole(RoleName name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name, description)));
    }

    private void initializeDemoUser(String email, String rawPassword, String nom, String prenom,
                                    String idNumber, UserCategory cat, Set<Role> roles) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setNom(nom);
            user.setPrenom(prenom);
            user.setNumeroIdentification(idNumber);
            user.setTelephone("+221770000000");
            user.setCategorie(cat);
            user.setActif(true);
            user.setBloque(false);
            user.setRoles(roles);
            userRepository.save(user);
            log.info("Compte de test initialisé : {} ({})", email, cat);
        }
    }
}
