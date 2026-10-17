package sn.uasz.voteuasz.complaints;

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
@RequestMapping("/complaints")
@Tag(name = "Contentieux & Réclamations", description = "Dépôt des contestations électorales et instruction par la Commission")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping("/election/{electionId}")
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Consulter toutes les réclamations liées à un scrutin (Commission)")
    public ResponseEntity<ApiResponse<List<ComplaintDto>>> getComplaintsByElection(@PathVariable Long electionId) {
        return ResponseEntity.ok(ApiResponse.ok("Réclamations récupérées", complaintService.findByElection(electionId)));
    }

    @GetMapping("/my")
    @Operation(summary = "Consulter ses propres réclamations déposées")
    public ResponseEntity<ApiResponse<List<ComplaintDto>>> getMyComplaints() {
        return ResponseEntity.ok(ApiResponse.ok("Mes réclamations", complaintService.findMyComplaints()));
    }

    @PostMapping
    @Operation(summary = "Déposer une contestation ou réclamation sur un scrutin")
    public ResponseEntity<ApiResponse<ComplaintDto>> submitComplaint(@Valid @RequestBody ComplaintSubmitDto dto) {
        ComplaintDto created = complaintService.submitComplaint(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Réclamation enregistrée avec succès", created));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Rendre une décision motivée sur une réclamation (Commission)")
    public ResponseEntity<ApiResponse<ComplaintDto>> resolveComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintResolutionDto resolutionDto) {
        ComplaintDto updated = complaintService.resolveComplaint(id, resolutionDto);
        return ResponseEntity.ok(ApiResponse.ok("Décision de la commission enregistrée", updated));
    }
}
