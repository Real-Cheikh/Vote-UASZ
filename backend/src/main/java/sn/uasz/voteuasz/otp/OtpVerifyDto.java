package sn.uasz.voteuasz.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class OtpVerifyDto {

    @NotNull(message = "L'identifiant du scrutin est obligatoire")
    private Long electionId;

    @NotBlank(message = "Le code OTP est obligatoire")
    @Pattern(regexp = "^[0-9]{6}$", message = "Le code OTP doit être composé exactement de 6 chiffres")
    private String code;

    public OtpVerifyDto() {
    }

    public OtpVerifyDto(Long electionId, String code) {
        this.electionId = electionId;
        this.code = code;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
