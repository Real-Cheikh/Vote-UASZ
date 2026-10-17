package sn.uasz.voteuasz.otp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.voteuasz.common.ApiResponse;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.security.UserPrincipal;

@RestController
@RequestMapping("/otp")
@Tag(name = "Authentification OTP", description = "Vérification forte à deux facteurs par code temporaire à usage unique")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Demander la génération d'un code OTP à usage unique (valable 5 minutes)")
    public ResponseEntity<ApiResponse<OtpChallengeResponseDto>> generateOtp(@Valid @RequestBody OtpGenerateDto dto) {
        Long userId = getCurrentUserId();
        OtpChallengeResponseDto response = otpService.generateOtp(userId, dto.getElectionId());
        return ResponseEntity.ok(ApiResponse.ok("Code OTP transmis avec succès", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Valider un code OTP pour débloquer l'autorisation de vote")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody OtpVerifyDto dto) {
        Long userId = getCurrentUserId();
        otpService.verifyOtp(userId, dto.getElectionId(), dto.getCode());
        return ResponseEntity.ok(ApiResponse.ok("Code OTP validé avec succès", "Code vérifié"));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new BusinessException("Session non authentifiée");
    }
}
