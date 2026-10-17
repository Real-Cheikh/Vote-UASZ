package sn.uasz.voteuasz.voting;

import java.time.OffsetDateTime;

public class ReceiptVerificationDto {

    private boolean valide;
    private String recuVoteHash;
    private Long electionId;
    private String electionTitre;
    private OffsetDateTime dateEnregistrement;
    private String message;

    public ReceiptVerificationDto() {
    }

    public ReceiptVerificationDto(boolean valide, String recuVoteHash, Long electionId, String electionTitre, OffsetDateTime dateEnregistrement, String message) {
        this.valide = valide;
        this.recuVoteHash = recuVoteHash;
        this.electionId = electionId;
        this.electionTitre = electionTitre;
        this.dateEnregistrement = dateEnregistrement;
        this.message = message;
    }

    public boolean isValide() {
        return valide;
    }

    public void setValide(boolean valide) {
        this.valide = valide;
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
