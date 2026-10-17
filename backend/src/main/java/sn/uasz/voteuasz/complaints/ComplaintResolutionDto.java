package sn.uasz.voteuasz.complaints;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ComplaintResolutionDto {

    @NotNull(message = "Le statut de résolution est obligatoire (RESOLVED, REJECTED, UNDER_REVIEW)")
    private ComplaintStatus statut;

    @NotBlank(message = "La réponse motivée de la commission est obligatoire")
    private String reponseCommission;

    public ComplaintResolutionDto() {
    }

    public ComplaintResolutionDto(ComplaintStatus statut, String reponseCommission) {
        this.statut = statut;
        this.reponseCommission = reponseCommission;
    }

    public ComplaintStatus getStatut() {
        return statut;
    }

    public void setStatut(ComplaintStatus statut) {
        this.statut = statut;
    }

    public String getReponseCommission() {
        return reponseCommission;
    }

    public void setReponseCommission(String reponseCommission) {
        this.reponseCommission = reponseCommission;
    }
}
