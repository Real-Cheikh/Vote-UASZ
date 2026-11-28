package sn.uasz.voteuasz.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.uasz.voteuasz.common.ApiResponse;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/users")
@Tag(name = "Gestion des Utilisateurs", description = "Administration des comptes, rôles et statuts d'accès")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN_ELECTORAL', 'COMMISSION_ELECTORALE')")
    @Operation(summary = "Lister tous les utilisateurs de la plateforme")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Liste des utilisateurs récupérée", userService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN_ELECTORAL', 'COMMISSION_ELECTORALE')")
    @Operation(summary = "Obtenir un utilisateur par son identifiant")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur trouvé", userService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Créer un nouvel utilisateur avec ses rôles initiaux")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserRegistrationDto dto) {
        UserDto created = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Utilisateur créé avec succès", created));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Modifier les rôles attribués à un utilisateur")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRoles(@PathVariable Long id, @RequestBody Set<String> roles) {
        return ResponseEntity.ok(ApiResponse.ok("Rôles mis à jour", userService.updateUserRoles(id, roles)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Activer, désactiver ou bloquer un compte utilisateur")
    public ResponseEntity<ApiResponse<UserDto>> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean actif,
            @RequestParam boolean bloque) {
        return ResponseEntity.ok(ApiResponse.ok("Statut du compte mis à jour", userService.toggleUserStatus(id, actif, bloque)));
    }

    @PostMapping("/commission")
    @PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Créer un compte individuel de membre de commission électorale avec mot de passe temporaire robuste")
    public ResponseEntity<ApiResponse<CommissionMemberResponseDto>> createCommissionMember(@Valid @RequestBody CommissionMemberCreateDto dto) {
        CommissionMemberResponseDto created = userService.createCommissionMember(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Membre de la commission créé avec succès", created));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Réinitialiser administrativement le mot de passe d'un compte avec génération d'un mot de passe temporaire")
    public ResponseEntity<ApiResponse<CommissionMemberResponseDto>> resetPassword(@PathVariable Long id) {
        CommissionMemberResponseDto response = userService.resetUserPasswordAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok("Mot de passe temporaire généré avec succès", response));
    }

    @PostMapping("/deactivate-demo-accounts")
    @PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Désactiver et exclure de la connexion tous les comptes de démonstration et de test")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> deactivateDemoAccounts() {
        return ResponseEntity.ok(ApiResponse.ok("Comptes de démonstration désactivés", userService.deactivateDemoAccounts()));
    }

    @PostMapping("/purge-demo-accounts")
    @PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Nettoyage définitif : suppression des comptes démo sans dépendance et archivage mort irréversible de ceux liés à l'historique électoral")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> purgeDemoAccounts() {
        return ResponseEntity.ok(ApiResponse.ok("Nettoyage définitif des comptes de démonstration terminé", userService.purgeOrArchiveDemoAccounts()));
    }
}
