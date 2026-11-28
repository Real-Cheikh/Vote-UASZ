package sn.uasz.voteuasz.users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.audit.AuditService;
import sn.uasz.voteuasz.auth.ChangePasswordDto;
import sn.uasz.voteuasz.candidacies.CandidacyRepository;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.complaints.ComplaintRepository;
import sn.uasz.voteuasz.eligibility.EligibilityRecord;
import sn.uasz.voteuasz.eligibility.EligibilityRepository;
import sn.uasz.voteuasz.otp.OtpRepository;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;
import sn.uasz.voteuasz.security.UserPrincipal;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "@#$%!&*";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%!&*]).{8,}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CandidacyRepository candidacyRepository;
    private final EligibilityRepository eligibilityRepository;
    private final ComplaintRepository complaintRepository;
    private final OtpRepository otpRepository;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       CandidacyRepository candidacyRepository,
                       EligibilityRepository eligibilityRepository,
                       ComplaintRepository complaintRepository,
                       OtpRepository otpRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.candidacyRepository = candidacyRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.complaintRepository = complaintRepository;
        this.otpRepository = otpRepository;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> findAllPaged(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto createUser(UserRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Un utilisateur existe déjà avec cet email : " + dto.getEmail());
        }
        if (userRepository.existsByNumeroIdentification(dto.getNumeroIdentification())) {
            throw new BusinessException("Ce numéro d'identification est déjà attribué : " + dto.getNumeroIdentification());
        }

        User user = new User();
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setNom(dto.getNom().trim());
        user.setPrenom(dto.getPrenom().trim());
        user.setNumeroIdentification(dto.getNumeroIdentification().trim());
        user.setTelephone(dto.getTelephone());
        user.setCategorie(dto.getCategorie());
        user.setActif(true);
        user.setBloque(false);
        user.setDoitChangerMotDePasse(false);

        Set<Role> roles = new HashSet<>();
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            for (String roleNameStr : dto.getRoles()) {
                RoleName roleName = RoleName.valueOf(roleNameStr);
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + roleNameStr));
                roles.add(role);
            }
            // Règle métier : tout candidat conserve impérativement son droit de vote électoral (ROLE_ELECTEUR)
            if (roles.stream().anyMatch(r -> r.getName() == RoleName.ROLE_CANDIDAT) &&
                roles.stream().noneMatch(r -> r.getName() == RoleName.ROLE_ELECTEUR)) {
                Role electeurRole = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                        .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur universitaire")));
                roles.add(electeurRole);
            }
        } else {
            // Rôle par défaut : Électeur
            Role defaultRole = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur universitaire")));
            roles.add(defaultRole);
        }
        user.setRoles(roles);

        User saved = userRepository.save(user);
        auditService.logEvent("USER_CREATED", "Création de compte utilisateur ID: " + saved.getId() + " (" + saved.getEmail() + ")", null, "SUCCESS");
        return UserDto.fromEntity(saved);
    }

    /**
     * Création administrative d'un membre de la commission électorale avec mot de passe aléatoire robuste
     * et obligation de renouvellement dès la première connexion.
     */
    @Transactional
    public CommissionMemberResponseDto createCommissionMember(CommissionMemberCreateDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Un compte existe déjà avec cette adresse email : " + email);
        }
        if (userRepository.existsByNumeroIdentification(dto.getNumeroIdentification())) {
            throw new BusinessException("Ce numéro d'identification est déjà attribué : " + dto.getNumeroIdentification());
        }

        String rawTempPassword = generateStrongPassword();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawTempPassword));
        user.setNom(dto.getNom().trim());
        user.setPrenom(dto.getPrenom().trim());
        user.setNumeroIdentification(dto.getNumeroIdentification().trim());
        user.setTelephone(dto.getTelephone());
        user.setCategorie(dto.getCategorie());
        user.setActif(true);
        user.setBloque(false);
        user.setDoitChangerMotDePasse(true); // Obligation stricte de changement au premier login

        // Attribution stricte du rôle Commission Électorale (jamais de rôle Super Admin hérité)
        Role commissionRole = roleRepository.findByName(RoleName.ROLE_COMMISSION_ELECTORALE)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_COMMISSION_ELECTORALE, "Membre de la commission électorale")));
        user.setRoles(Set.of(commissionRole));

        User saved = userRepository.save(user);
        auditService.logEvent("COMMISSION_MEMBER_CREATED", "Création du membre de commission électorale ID: " + saved.getId() + " (" + saved.getEmail() + ")", null, "SUCCESS");

        log.info("Membre de commission électorale créé avec succès : ID={}, Email={}", saved.getId(), saved.getEmail());

        return new CommissionMemberResponseDto(
                saved.getId(),
                saved.getEmail(),
                saved.getNom(),
                saved.getPrenom(),
                saved.getNumeroIdentification(),
                saved.getTelephone(),
                saved.getCategorie(),
                Set.of(RoleName.ROLE_COMMISSION_ELECTORALE.name()),
                rawTempPassword,
                "Compte de la commission électorale créé avec succès. Le mot de passe temporaire doit être transmis hors-bande de manière confidentielle. Le renouvellement est obligatoire dès la première connexion."
        );
    }

    /**
     * Procédure administrative sécurisée de réinitialisation de mot de passe par le Super Administrateur.
     */
    @Transactional
    public CommissionMemberResponseDto resetUserPasswordAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        String rawTempPassword = generateStrongPassword();
        user.setPasswordHash(passwordEncoder.encode(rawTempPassword));
        user.setDoitChangerMotDePasse(true);
        User saved = userRepository.save(user);

        auditService.logEvent("ADMIN_PASSWORD_RESET", "Réinitialisation administrative du mot de passe pour l'utilisateur ID: " + userId + " (" + user.getEmail() + ")", null, "SUCCESS");

        Set<String> roleNames = saved.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet());
        return new CommissionMemberResponseDto(
                saved.getId(),
                saved.getEmail(),
                saved.getNom(),
                saved.getPrenom(),
                saved.getNumeroIdentification(),
                saved.getTelephone(),
                saved.getCategorie(),
                roleNames,
                rawTempPassword,
                "Mot de passe temporaire régénéré avec succès. L'utilisateur devra obligatoirement le renouveler lors de sa prochaine connexion."
        );
    }

    /**
     * Renouvellement de mot de passe par l'utilisateur connecté (premier login ou changement volontaire).
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            auditService.logEvent("PASSWORD_CHANGE_FAILED", "Échec changement mot de passe : ancien mot de passe incorrect", null, "FAILURE");
            throw new BusinessException("L'ancien mot de passe (ou mot de passe temporaire) est incorrect.");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("Le nouveau mot de passe et sa confirmation ne correspondent pas.");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("Le nouveau mot de passe doit être différent de l'ancien mot de passe.");
        }

        if (!PASSWORD_PATTERN.matcher(dto.getNewPassword()).matches()) {
            throw new BusinessException("Le mot de passe doit comporter au moins 8 caractères, dont au moins une majuscule, une minuscule, un chiffre et un caractère spécial (@#$%!&*).");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setDoitChangerMotDePasse(false);
        userRepository.save(user);

        auditService.logEvent("PASSWORD_CHANGED", "Renouvellement réussi du mot de passe pour l'utilisateur ID: " + user.getId(), null, "SUCCESS");
    }

    /**
     * Modification des rôles avec protection anti-escalade de privilèges.
     */
    @Transactional
    public UserDto updateUserRoles(Long userId, Set<String> roleNames) {
        Long currentUserId = getCurrentUserId();
        if (userId.equals(currentUserId)) {
            auditService.logEvent("PRIVILEGE_ESCALATION_BLOCKED", "Tentative d'auto-modification de rôles bloquée pour l'utilisateur ID: " + userId, null, "DENIED");
            throw new BusinessException("Opération interdite : vous ne pouvez pas modifier vos propres rôles.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Set<Role> updatedRoles = new HashSet<>();
        for (String roleStr : roleNames) {
            RoleName roleName = RoleName.valueOf(roleStr);
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + roleStr));
            updatedRoles.add(role);
        }

        // Règle métier : tout candidat conserve impérativement son droit de vote électoral (ROLE_ELECTEUR)
        if (updatedRoles.stream().anyMatch(r -> r.getName() == RoleName.ROLE_CANDIDAT) &&
            updatedRoles.stream().noneMatch(r -> r.getName() == RoleName.ROLE_ELECTEUR)) {
            Role electeurRole = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur universitaire")));
            updatedRoles.add(electeurRole);
        }

        user.setRoles(updatedRoles);
        User saved = userRepository.save(user);
        auditService.logEvent("USER_ROLES_UPDATED", "Mise à jour des rôles pour l'utilisateur ID: " + userId + " -> " + roleNames, null, "SUCCESS");
        return UserDto.fromEntity(saved);
    }

    @Transactional
    public UserDto toggleUserStatus(Long userId, boolean actif, boolean bloque) {
        Long currentUserId = getCurrentUserId();
        if (userId.equals(currentUserId)) {
            throw new BusinessException("Opération interdite : vous ne pouvez pas désactiver ou bloquer votre propre compte administrateur.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setActif(actif);
        user.setBloque(bloque);
        User saved = userRepository.save(user);

        auditService.logEvent("USER_STATUS_TOGGLED", "Statut utilisateur ID: " + userId + " mis à jour (actif=" + actif + ", bloque=" + bloque + ")", null, "SUCCESS");
        return UserDto.fromEntity(saved);
    }

    /**
     * Désactivation sécurisée de tous les comptes de démonstration et synthétiques
     * sans rupture de l'intégrité référentielle ni altération des scrutins réels.
     */
    @Transactional
    public Map<String, Object> deactivateDemoAccounts() {
        List<User> allUsers = userRepository.findAll();
        List<String> deactivatedEmails = new ArrayList<>();

        for (User u : allUsers) {
            if (isDemoUserAccount(u)) {
                u.setActif(false);
                u.setBloque(true);
                userRepository.save(u);
                deactivatedEmails.add(u.getEmail());
            }
        }

        auditService.logEvent("DEMO_ACCOUNTS_DEACTIVATED", "Désactivation des comptes de démonstration : " + deactivatedEmails.size() + " comptes désactivés", null, "SUCCESS");

        Map<String, Object> result = new HashMap<>();
        result.put("deactivatedCount", deactivatedEmails.size());
        result.put("deactivatedAccounts", deactivatedEmails);
        result.put("message", "Tous les comptes de démonstration ont été désactivés et exclus de la connexion.");
        return result;
    }

    /**
     * Nettoyage définitif et audit approfondi des comptes de démonstration :
     * - Comptes avec dépendances historiques (candidatures, émargements aVote=true, réclamations) :
     *   Préservés pour garantir l'intégrité de l'urne et des résultats, mais désactivés définitivement,
     *   bloqués, mot de passe invalidé de façon irréversible et données directes anonymisées.
     * - Comptes sans aucune dépendance historique :
     *   Supprimés proprement et définitivement de la base de données.
     */
    @Transactional
    public Map<String, Object> purgeOrArchiveDemoAccounts() {
        List<User> allUsers = userRepository.findAll();
        List<String> retainedArchivedEmails = new ArrayList<>();
        List<String> deletedSafeEmails = new ArrayList<>();

        for (User u : allUsers) {
            if (isDemoUserAccount(u)) {
                Long uid = u.getId();
                boolean hasCandidacy = !candidacyRepository.findByCandidatId(uid).isEmpty();
                boolean hasVoted = eligibilityRepository.findByUserId(uid).stream()
                        .anyMatch(EligibilityRecord::isaVote);
                boolean hasComplaints = !complaintRepository.findByUserId(uid).isEmpty();

                if (hasCandidacy || hasVoted || hasComplaints) {
                    // DÉPENDANCE CRITIQUE -> Conservation en archive morte (neutralisation irréversible)
                    u.setActif(false);
                    u.setBloque(true);
                    u.setPasswordHash("REVOKED_HISTORICAL_RECORD_" + UUID.randomUUID());
                    u.setDoitChangerMotDePasse(false);
                    u.setTelephone(null); // Anonymisation des données de contact direct
                    userRepository.save(u);
                    retainedArchivedEmails.add(u.getEmail());
                    log.warn("Compte démonstration avec dépendance historique archivé mort et neutralisé : {} (candidature: {}, voté: {}, réclamations: {})",
                            u.getEmail(), hasCandidacy, hasVoted, hasComplaints);
                } else {
                    // AUCUNE DÉPENDANCE HISTORIQUE -> Suppression sûre et définitive
                    otpRepository.deleteByUserId(uid);
                    eligibilityRepository.deleteAll(eligibilityRepository.findByUserId(uid));
                    u.getRoles().clear();
                    userRepository.saveAndFlush(u);
                    userRepository.delete(u);
                    deletedSafeEmails.add(u.getEmail());
                    log.info("Compte démonstration sans dépendance historique supprimé définitivement : {}", u.getEmail());
                }
            }
        }

        auditService.logEvent("DEMO_ACCOUNTS_PURGED_AND_ARCHIVED",
                "Nettoyage des comptes de démo : " + deletedSafeEmails.size() + " supprimés définitivement, " + retainedArchivedEmails.size() + " archivés morts avec intégrité préservée",
                null, "SUCCESS");

        Map<String, Object> result = new HashMap<>();
        result.put("deletedSafeCount", deletedSafeEmails.size());
        result.put("deletedSafeAccounts", deletedSafeEmails);
        result.put("retainedArchivedCount", retainedArchivedEmails.size());
        result.put("retainedArchivedAccounts", retainedArchivedEmails);
        result.put("message", "Nettoyage définitif des comptes démo effectué : les comptes sans dépendance ont été supprimés, les comptes liés à l'historique électoral ont été archivés et définitivement verrouillés.");
        return result;
    }

    public boolean isDemoUserAccount(User user) {
        if (user == null || user.getEmail() == null) return false;
        // Protection absolue : ne JAMAIS désactiver un Super Administrateur
        if (user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_SUPER_ADMIN_ELECTORAL)) {
            return false;
        }
        String email = user.getEmail().toLowerCase();
        // Protection absolue des comptes institutionnels officiels et des étudiants réels inscrits
        if (email.equals("admin.electoral@uasz.sn") ||
            email.equals("cam.n@zig.univ.sn") ||
            email.equals("ad.77@zig.univ.sn") ||
            email.equals("k.d66@zig.univ.sn")) {
            return false;
        }
        String numId = user.getNumeroIdentification() != null ? user.getNumeroIdentification().toLowerCase() : "";
        return email.startsWith("etudiant1@") ||
               email.startsWith("candidat1@") ||
               email.startsWith("enseignant1@") ||
               email.startsWith("commission@") ||
               email.contains("synth_") ||
               email.contains("test") ||
               email.contains("audit") ||
               email.contains("demo") ||
               numId.contains("test") ||
               numId.contains("audit") ||
               numId.contains("demo");
    }

    public static String generateStrongPassword() {
        StringBuilder sb = new StringBuilder(14);
        sb.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        sb.append(SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length())));
        for (int i = 4; i < 14; i++) {
            sb.append(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }
        List<Character> chars = sb.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        Collections.shuffle(chars, RANDOM);
        StringBuilder res = new StringBuilder();
        chars.forEach(res::append);
        return res.toString();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return -1L;
    }
}
