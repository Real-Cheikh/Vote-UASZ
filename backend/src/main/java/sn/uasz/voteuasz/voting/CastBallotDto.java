package sn.uasz.voteuasz.voting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CastBallotDto {

    @NotNull(message = "L'identifiant du scrutin est obligatoire")
    private Long electionId;

    @NotBlank(message = "Le jeton d'autorisation de vote est obligatoire")
    private String voteToken;

    private Long candidacyId; // Obligatoire si estBlanc est false

    private boolean estBlanc = false;

    public CastBallotDto() {
    }

    public CastBallotDto(Long electionId, String voteToken, Long candidacyId, boolean estBlanc) {
        this.electionId = electionId;
        this.voteToken = voteToken;
        this.candidacyId = candidacyId;
        this.estBlanc = estBlanc;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public String getVoteToken() {
        return voteToken;
    }

    public void setVoteToken(String voteToken) {
        this.voteToken = voteToken;
    }

    public Long getCandidacyId() {
        return candidacyId;
    }

    public void setCandidacyId(Long candidacyId) {
        this.candidacyId = candidacyId;
    }

    public boolean isEstBlanc() {
        return estBlanc;
    }

    public void setEstBlanc(boolean estBlanc) {
        this.estBlanc = estBlanc;
    }
}
