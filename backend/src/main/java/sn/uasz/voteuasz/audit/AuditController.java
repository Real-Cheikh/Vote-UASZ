package sn.uasz.voteuasz.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.uasz.voteuasz.common.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/audit")
@Tag(name = "Audit & Traçabilité", description = "Journaux d'audit immuables avec chaînage cryptographique")
@PreAuthorize("hasRole('SUPER_ADMIN_ELECTORAL')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/events")
    @Operation(summary = "Consulter le registre d'audit général avec pagination")
    public ResponseEntity<ApiResponse<Page<AuditEvent>>> getAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditEvent> events = auditService.getEvents(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok("Événements d'audit récupérés", events));
    }

    @GetMapping("/election/{electionId}")
    @Operation(summary = "Consulter les événements d'audit d'une élection spécifique")
    public ResponseEntity<ApiResponse<List<AuditEvent>>> getElectionAudit(@PathVariable Long electionId) {
        return ResponseEntity.ok(ApiResponse.ok("Audit de l'élection récupéré", auditService.getEventsByElection(electionId)));
    }
}
