package sn.uasz.voteuasz.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import sn.uasz.voteuasz.common.ApiResponse;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.security.UserPrincipal;
import sn.uasz.voteuasz.users.UserDto;
import sn.uasz.voteuasz.users.UserService;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentification", description = "Endpoints de connexion, rafraîchissement de jetons et profil connecté")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un électeur avec adresse institutionnelle @zig.univ.sn")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        UserDto created = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Compte électeur créé avec succès. Vous pouvez maintenant vous connecter.", created));
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur avec email et mot de passe")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Authentification réussie", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement du jeton JWT d'accès avec le jeton de rafraîchissement")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Jeton renouvelé avec succès", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtention des informations du profil de l'utilisateur actuellement connecté")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        UserDto user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Profil récupéré", user));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion de la session")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Déconnexion effectuée avec succès", null));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Renouvellement du mot de passe (obligatoire au premier login ou sur demande)")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        Long userId = getCurrentUserId();
        userService.changePassword(userId, dto);
        return ResponseEntity.ok(ApiResponse.ok("Mot de passe mis à jour avec succès", "Mot de passe modifié"));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new BusinessException("Session non authentifiée");
    }
}
