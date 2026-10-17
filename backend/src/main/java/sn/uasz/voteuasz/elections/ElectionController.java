package sn.uasz.voteuasz.elections;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.uasz.voteuasz.common.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/elections")
@Tag(name = "Élections", description = "Gestion du cycle de vie des scrutins électoraux de l'UASZ")
public class ElectionController {

    private final ElectionService electionService;

    public ElectionController(ElectionService electionService) {
        this.electionService = electionService;
    }

    @GetMapping("/public")
    @Operation(summary = "Lister publiquement les scrutins officiels en cours ou passés")
    public ResponseEntity<ApiResponse<List<ElectionDto>>> getPublicElections() {
        return ResponseEntity.ok(ApiResponse.ok("Élections publiques récupérées", electionService.findPublicElections()));
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Consulter les détails publics d'un scrutin")
    public ResponseEntity<ApiResponse<ElectionDto>> getPublicElectionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Détails de l'élection", electionService.findById(id)));
    }

    @GetMapping
    @Operation(summary = "Lister tous les scrutins (pour les utilisateurs connectés)")
    public ResponseEntity<ApiResponse<List<ElectionDto>>> getAllElections() {
        return ResponseEntity.ok(ApiResponse.ok("Liste complète des élections", electionService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir les détails complets d'un scrutin par son identifiant")
    public ResponseEntity<ApiResponse<ElectionDto>> getElectionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Détails de l'élection", electionService.findById(id)));
    }

    @PostMapping("/manage")
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Créer une nouvelle élection (Commission électorale)")
    public ResponseEntity<ApiResponse<ElectionDto>> createElection(@Valid @RequestBody ElectionCreateDto dto) {
        ElectionDto created = electionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Élection configurée avec succès en statut DRAFT", created));
    }

    @PatchMapping("/manage/{id}/status")
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Faire avancer le statut de l'élection selon le workflow officiel")
    public ResponseEntity<ApiResponse<ElectionDto>> updateElectionStatus(
            @PathVariable Long id,
            @Valid @RequestBody ElectionStatusUpdateDto dto) {
        ElectionDto updated = electionService.updateStatus(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Statut de l'élection mis à jour avec succès", updated));
    }

    @DeleteMapping("/manage/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Supprimer un brouillon d'élection (Super Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteElection(@PathVariable Long id) {
        electionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Élection supprimée", null));
    }
}
