package sn.uasz.voteuasz.voting;

import java.time.OffsetDateTime;

public class VoteReceiptDto {

    private String recuVoteHash;
    private Long electionId;
    private String electionTitre;
    private OffsetDateTime dateEnregistrement;
    private String message;

    public VoteReceiptDto() {
    }

    public VoteReceiptDto(String recuVoteHash, Long electionId, String electionTitre, OffsetDateTime dateEnregistrement, String message) {
        this.recuVoteHash = recuVoteHash;
        this.electionId = electionId;
        this.electionTitre = electionTitre;
        this.dateEnregistrement = dateEnregistrement;
        this.message = message;
    }

    public String getRecuVoteHash() {
        return recuVoteHash;
    }

    public void setRecuVoteHash(String recuVoteHash) {
        this.recuVoteHash = recuVoteHash;
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

    public OffsetDateTime getDateEnregistrement() {
        return dateEnregistrement;
    }

    public void setDateEnregistrement(OffsetDateTime dateEnregistrement) {
        this.dateEnregistrement = dateEnregistrement;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
