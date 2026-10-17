package sn.uasz.voteuasz.complaints;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ComplaintSubmitDto {

    @NotNull(message = "L'identifiant du scrutin est obligatoire")
    private Long electionId;

    @NotBlank(message = "L'objet de la contestation est obligatoire")
    private String objet;

    @NotBlank(message = "La description de la réclamation est obligatoire")
    private String description;

    private String piecesJointesUrls;

    public ComplaintSubmitDto() {
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPiecesJointesUrls() {
        return piecesJointesUrls;
    }

    public void setPiecesJointesUrls(String piecesJointesUrls) {
        this.piecesJointesUrls = piecesJointesUrls;
    }
}
