package sn.uasz.voteuasz.voting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class VoteAuthorizationRequestDto {

    @NotNull(message = "L'identifiant du scrutin est obligatoire")
    private Long electionId;

    @NotBlank(message = "Le code OTP est obligatoire")
    @Pattern(regexp = "^[0-9]{6}$", message = "Le code OTP doit être composé de 6 chiffres")
    private String otpCode;

    public VoteAuthorizationRequestDto() {
    }

    public VoteAuthorizationRequestDto(Long electionId, String otpCode) {
        this.electionId = electionId;
        this.otpCode = otpCode;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
