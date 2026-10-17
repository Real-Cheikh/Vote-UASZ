package sn.uasz.voteuasz.candidacies;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sn.uasz.voteuasz.common.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/candidacies")
@Tag(name = "Candidatures & Campagnes", description = "Dépôt des dossiers, pièces justificatives et validation officielle")
public class CandidacyController {

    private final CandidacyService candidacyService;
    private final FileStorageService fileStorageService;

    public CandidacyController(CandidacyService candidacyService, FileStorageService fileStorageService) {
        this.candidacyService = candidacyService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/public/election/{electionId}")
    @Operation(summary = "Lister la liste officielle des candidats validés pour un scrutin")
    public ResponseEntity<ApiResponse<List<CandidacyDto>>> getPublicCandidates(@PathVariable Long electionId) {
        return ResponseEntity.ok(ApiResponse.ok("Candidats officiels validés", candidacyService.findPublicCandidatesByElection(electionId)));
    }

    @GetMapping("/election/{electionId}")
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Consulter tous les dossiers de candidature pour instruction par la Commission")
    public ResponseEntity<ApiResponse<List<CandidacyDto>>> getAllCandidatesByElection(@PathVariable Long electionId) {
        return ResponseEntity.ok(ApiResponse.ok("Tous les dossiers de candidature", candidacyService.findAllByElection(electionId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir les détails d'une candidature")
    public ResponseEntity<ApiResponse<CandidacyDto>> getCandidacyById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Détails de la candidature", candidacyService.findById(id)));
    }

    @PostMapping("/apply")
    @Operation(summary = "Déposer une candidature à un scrutin ouvert")
    public ResponseEntity<ApiResponse<CandidacyDto>> apply(@Valid @RequestBody CandidacyApplyDto dto) {
        CandidacyDto created = candidacyService.apply(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Candidature enregistrée avec succès", created));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Téléverser une pièce justificative pour son dossier (CV, Attestation, Photo)")
    public ResponseEntity<ApiResponse<CandidateDocumentDto>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("type") DocumentType type,
            @RequestParam("file") MultipartFile file) {
        CandidateDocumentDto doc = candidacyService.uploadDocument(id, type, file);
        return ResponseEntity.ok(ApiResponse.ok("Document téléversé avec succès", doc));
    }

    @GetMapping("/documents/file/{filename:.+}")
    @Operation(summary = "Télécharger ou visualiser un document justificatif sécurisé")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        Resource file = fileStorageService.loadFileAsResource(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @PatchMapping("/review/{id}")
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Valider ou rejeter un dossier de candidature avec motif officiel")
    public ResponseEntity<ApiResponse<CandidacyDto>> reviewCandidacy(
            @PathVariable Long id,
            @Valid @RequestBody CandidacyDecisionDto decisionDto) {
        CandidacyDto updated = candidacyService.reviewCandidacy(id, decisionDto);
        return ResponseEntity.ok(ApiResponse.ok("Décision enregistrée avec succès", updated));
    }
}
