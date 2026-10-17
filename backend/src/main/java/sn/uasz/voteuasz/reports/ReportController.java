package sn.uasz.voteuasz.reports;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.uasz.voteuasz.common.ApiResponse;
import sn.uasz.voteuasz.common.ResourceNotFoundException;

@RestController
@RequestMapping("/reports")
@Tag(name = "Procès-Verbaux & Rapports Officiels", description = "Génération de procès-verbaux PDF certifiés et vérification cryptographique")
public class ReportController {

    private final PdfReportGeneratorService pdfReportGeneratorService;
    private final OfficialReportRepository reportRepository;

    public ReportController(PdfReportGeneratorService pdfReportGeneratorService,
                            OfficialReportRepository reportRepository) {
        this.pdfReportGeneratorService = pdfReportGeneratorService;
        this.reportRepository = reportRepository;
    }

    @GetMapping("/election/{electionId}/pdf")
    @Operation(summary = "Télécharger le procès-verbal officiel au format PDF")
    public ResponseEntity<byte[]> downloadElectionReportPdf(@PathVariable Long electionId) {
        byte[] pdfBytes = pdfReportGeneratorService.generateElectionReportPdf(electionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"PV_Election_UASZ_" + electionId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/verify/{reportNumber}")
    @Operation(summary = "Vérifier publiquement l'authenticité d'un procès-verbal à partir de son numéro officiel")
    public ResponseEntity<ApiResponse<OfficialReport>> verifyReport(@PathVariable String reportNumber) {
        OfficialReport report = reportRepository.findByNumeroRapport(reportNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun procès-verbal officiel ne correspond au numéro : " + reportNumber));
        return ResponseEntity.ok(ApiResponse.ok("Procès-verbal officiel authentifié", report));
    }
}
