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
}
