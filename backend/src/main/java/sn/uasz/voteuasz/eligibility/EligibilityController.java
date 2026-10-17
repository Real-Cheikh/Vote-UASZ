package sn.uasz.voteuasz.eligibility;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import sn.uasz.voteuasz.common.ApiResponse;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.security.UserPrincipal;

import java.util.List;

@RestController
@RequestMapping("/eligibility")
@Tag(name = "Éligibilité Électorale", description = "Vérification des droits de vote et scrutins accessibles")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @GetMapping("/my-elections")
    @Operation(summary = "Obtenir la liste des scrutins auxquels l'électeur connecté est éligible")
    public ResponseEntity<ApiResponse<List<EligibilityDto>>> getMyEligibleElections() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Élections accessibles", eligibilityService.findEligibleElectionsForUser(userId)));
    }

    @GetMapping("/check/{electionId}")
    @Operation(summary = "Vérifier le statut d'éligibilité et l'état du droit de vote pour un scrutin")
    public ResponseEntity<ApiResponse<EligibilityDto>> checkEligibility(@PathVariable Long electionId) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Statut d'éligibilité", eligibilityService.checkUserEligibility(electionId, userId)));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new BusinessException("Session non authentifiée");
    }
}
