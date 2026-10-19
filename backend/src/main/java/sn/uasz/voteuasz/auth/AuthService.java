package sn.uasz.voteuasz.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;
import sn.uasz.voteuasz.security.JwtTokenProvider;
import sn.uasz.voteuasz.security.UserPrincipal;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserCategory;
import sn.uasz.voteuasz.users.UserDto;
import sn.uasz.voteuasz.users.UserRepository;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Pattern INSTITUTIONAL_EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@zig\\.univ\\.sn$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Identifiants incorrects"));

        if (!user.isActif() || user.isBloque()) {
            throw new BadCredentialsException("Compte suspendu ou inactif. Veuillez contacter la commission électorale.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants incorrects");
        }

        user.setDerniereConnexion(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getCategorie() != null ? user.getCategorie().name() : null,
                roles
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!tokenProvider.validateToken(token)) {
            throw new BusinessException("Jeton de rafraîchissement invalide ou expiré");
        }

        String email = tokenProvider.getUsernameFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!user.isActif() || user.isBloque()) {
            throw new BusinessException("Compte suspendu ou inactif");
        }

        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getCategorie() != null ? user.getCategorie().name() : null,
                roles
        );
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException("Aucun utilisateur authentifié");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessException("L'adresse email institutionnelle est obligatoire.");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Validation stricte du domaine institutionnel @zig.univ.sn
        if (!INSTITUTIONAL_EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BusinessException("Seules les adresses institutionnelles officielles se terminant par @zig.univ.sn sont autorisées pour l'inscription étudiante.");
        }

        String normalizedNumId = request.getNumeroIdentification() != null ? request.getNumeroIdentification().trim() : "";
        if (normalizedNumId.isEmpty()) {
            throw new BusinessException("Le numéro d'étudiant ou d'identification est obligatoire.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Un compte existe déjà avec cette adresse email institutionnelle : " + normalizedEmail);
        }

        if (userRepository.existsByNumeroIdentification(normalizedNumId)) {
            throw new BusinessException("Un compte est déjà enregistré avec ce numéro d'identification : " + normalizedNumId);
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNom(request.getNom().trim());
        user.setPrenom(request.getPrenom().trim());
        user.setNumeroIdentification(normalizedNumId);
        user.setTelephone(request.getTelephone() != null && !request.getTelephone().isBlank() ? request.getTelephone().trim() : null);
        user.setCategorie(UserCategory.ETUDIANT);
        user.setActif(true);
        user.setBloque(false);

        // Attribution stricte et exclusive du rôle Électeur (JAMAIS un rôle administrateur)
        Role roleElecteur = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur universitaire")));
        user.setRoles(Set.of(roleElecteur));

        User saved = userRepository.save(user);
        return UserDto.fromEntity(saved);
    }
}
