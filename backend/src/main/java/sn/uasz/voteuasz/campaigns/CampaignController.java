package sn.uasz.voteuasz.campaigns;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.uasz.voteuasz.common.ApiResponse;

import java.util.List;

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
}
