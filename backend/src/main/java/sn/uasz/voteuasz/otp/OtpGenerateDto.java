package sn.uasz.voteuasz.otp;

import jakarta.validation.constraints.NotNull;

public class OtpGenerateDto {

    @NotNull(message = "L'identifiant du scrutin est obligatoire")
    private Long electionId;

    public OtpGenerateDto() {
    }

    public OtpGenerateDto(Long electionId) {
        this.electionId = electionId;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }
}
