package sn.uasz.voteuasz.results;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.uasz.voteuasz.common.ApiResponse;

@RestController
@RequestMapping("/results")
@Tag(name = "Résultats & Dépouillement", description = "Procédures de dépouillement officiel, publication et diffusion temps réel")
public class ResultsController {

    private final ResultsService resultsService;

    public ResultsController(ResultsService resultsService) {
        this.resultsService = resultsService;
    }

    @GetMapping("/public/{electionId}")
    @Operation(summary = "Consulter publiquement les résultats officiels publiés d'un scrutin")
    public ResponseEntity<ApiResponse<ElectionResultDto>> getPublicResults(@PathVariable Long electionId) {
        return ResponseEntity.ok(ApiResponse.ok("Résultats officiels récupérés", resultsService.getPublicResults(electionId)));
    }

    @PostMapping("/count/{electionId}")
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Effectuer le dépouillement officiel d'un scrutin clôturé (Commission)")
    public ResponseEntity<ApiResponse<ElectionResultDto>> countVotes(@PathVariable Long electionId) {
        ElectionResultDto result = resultsService.countVotes(electionId);
        return ResponseEntity.ok(ApiResponse.ok("Dépouillement réalisé avec succès", result));
    }

    @PostMapping("/publish/{electionId}")
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Publier officiellement les résultats et diffuser sur WebSocket (Commission)")
    public ResponseEntity<ApiResponse<ElectionResultDto>> publishResults(@PathVariable Long electionId) {
        ElectionResultDto result = resultsService.publishResults(electionId);
        return ResponseEntity.ok(ApiResponse.ok("Résultats officiellement publiés et diffusés", result));
    }

    @GetMapping("/election/{electionId}")
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Consulter les résultats d'un scrutin pour la Commission")
    public ResponseEntity<ApiResponse<ElectionResultDto>> getCommissionResults(@PathVariable Long electionId) {
        return ResponseEntity.ok(ApiResponse.ok("Résultats de la commission", resultsService.getElectionResultsForCommission(electionId)));
    }
}
