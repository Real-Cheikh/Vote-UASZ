package sn.uasz.voteuasz.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.security.JwtTokenProvider;
import sn.uasz.voteuasz.security.UserPrincipal;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserDto;
import sn.uasz.voteuasz.users.UserRepository;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
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
}
