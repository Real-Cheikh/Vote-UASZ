package sn.uasz.voteuasz.colleges;

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
@RequestMapping("/colleges")
@Tag(name = "Collèges Électoraux", description = "Gestion des corps électoraux de l'Université Assane Seck")
public class CollegeController {

    private final CollegeService collegeService;

    public CollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @GetMapping
    @Operation(summary = "Lister tous les collèges électoraux configurés")
    public ResponseEntity<ApiResponse<List<CollegeDto>>> getAllColleges() {
        return ResponseEntity.ok(ApiResponse.ok("Collèges récupérés", collegeService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir le détail d'un collège électoral par identifiant")
    public ResponseEntity<ApiResponse<CollegeDto>> getCollegeById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Collège trouvé", collegeService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Créer un nouveau collège électoral")
    public ResponseEntity<ApiResponse<CollegeDto>> createCollege(@Valid @RequestBody CollegeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Collège électoral créé avec succès", collegeService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Mettre à jour un collège électoral existant")
    public ResponseEntity<ApiResponse<CollegeDto>> updateCollege(@PathVariable Long id, @Valid @RequestBody CollegeDto dto) {
        return ResponseEntity.ok(ApiResponse.ok("Collège électoral mis à jour", collegeService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Supprimer un collège électoral")
    public ResponseEntity<ApiResponse<Void>> deleteCollege(@PathVariable Long id) {
        collegeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Collège électoral supprimé", null));
    }
}
