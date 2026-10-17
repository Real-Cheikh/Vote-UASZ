package sn.uasz.voteuasz.voting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.uasz.voteuasz.common.ApiResponse;

@RestController
@RequestMapping("/voting")
@Tag(name = "Vote Électronique Sécurisé", description = "Procédures de vote anonyme, émission d'autorisations uniques et vérification de reçus")
public class VotingController {

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    @PostMapping("/request-token")
    @Operation(summary = "Étape 1 : Obtenir un jeton d'autorisation de vote anonyme après vérification OTP")
    public ResponseEntity<ApiResponse<VoteAuthorizationResponseDto>> requestVoteToken(
            @Valid @RequestBody VoteAuthorizationRequestDto requestDto) {
        VoteAuthorizationResponseDto response = votingService.requestVoteAuthorization(requestDto);
        return ResponseEntity.ok(ApiResponse.ok("Jeton de vote anonyme délivré", response));
    }

    @PostMapping("/cast-ballot")
    @Operation(summary = "Étape 2 : Déposer son bulletin dans l'urne électronique avec le jeton d'autorisation")
    public ResponseEntity<ApiResponse<VoteReceiptDto>> castBallot(
            @Valid @RequestBody CastBallotDto ballotDto) {
        VoteReceiptDto receipt = votingService.castBallot(ballotDto);
        return ResponseEntity.ok(ApiResponse.ok("Bulletin déposé et scellé avec succès", receipt));
    }

    @GetMapping("/verify-receipt/{receiptHash}")
    @Operation(summary = "Vérification publique de la présence d'un bulletin dans l'urne à partir de son reçu")
    public ResponseEntity<ApiResponse<ReceiptVerificationDto>> verifyReceipt(
            @PathVariable String receiptHash) {
        ReceiptVerificationDto result = votingService.verifyReceipt(receiptHash);
        return ResponseEntity.ok(ApiResponse.ok("Résultat de vérification du reçu", result));
    }
}
