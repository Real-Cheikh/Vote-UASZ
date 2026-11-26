package sn.uasz.voteuasz.campaigns;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.uasz.voteuasz.common.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/campaigns")
@Tag(name = "Contenus de Campagne", description = "Publication et consultation des programmes et médias électoraux")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping("/candidacy/{candidacyId}")
    @Operation(summary = "Consulter les contenus de campagne autorisés d'un candidat")
    public ResponseEntity<ApiResponse<List<CampaignContent>>> getCampaignContents(@PathVariable Long candidacyId) {
        return ResponseEntity.ok(ApiResponse.ok("Contenus de campagne", campaignService.findByCandidacy(candidacyId)));
    }

    @PostMapping("/my/{candidacyId}")
    @PreAuthorize("hasAnyRole('CANDIDAT', 'ELECTEUR', 'COMMISSION_ELECTORALE', 'SUPER_ADMIN_ELECTORAL')")
    @Operation(summary = "Publier un message ou programme de campagne pour sa propre candidature validée")
    public ResponseEntity<ApiResponse<CampaignContent>> publishCampaignContent(
            @PathVariable Long candidacyId,
            @RequestBody Map<String, String> payload) {
        String titre = payload.getOrDefault("titre", "Message de campagne");
        String texte = payload.getOrDefault("texte", "");
        String mediaUrl = payload.get("mediaUrl");
        String typeMedia = payload.getOrDefault("typeMedia", "TEXT");

        CampaignContent created = campaignService.publishContent(candidacyId, titre, texte, mediaUrl, typeMedia);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Contenu de campagne publié avec succès", created));
    }
}
