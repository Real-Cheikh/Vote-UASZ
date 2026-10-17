package sn.uasz.voteuasz.voting;

import java.time.OffsetDateTime;

public class VoteAuthorizationResponseDto {

    private String voteToken;
    private Long electionId;
    private String electionTitre;
    private OffsetDateTime dateExpiration;
    private String message;

    public VoteAuthorizationResponseDto() {
    }

    public VoteAuthorizationResponseDto(String voteToken, Long electionId, String electionTitre, OffsetDateTime dateExpiration, String message) {
        this.voteToken = voteToken;
        this.electionId = electionId;
        this.electionTitre = electionTitre;
        this.dateExpiration = dateExpiration;
        this.message = message;
    }

    public String getVoteToken() {
        return voteToken;
    }

    public void setVoteToken(String voteToken) {
        this.voteToken = voteToken;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public String getElectionTitre() {
        return electionTitre;
    }

    public void setElectionTitre(String electionTitre) {
        this.electionTitre = electionTitre;
    }

    public OffsetDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(OffsetDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
