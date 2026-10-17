package sn.uasz.voteuasz.elections;

import jakarta.validation.constraints.NotNull;

public class ElectionStatusUpdateDto {

    @NotNull(message = "Le nouveau statut est requis")
    private ElectionStatus statut;

    private String motif;

    public ElectionStatusUpdateDto() {
    }

    public ElectionStatusUpdateDto(ElectionStatus statut, String motif) {
        this.statut = statut;
        this.motif = motif;
    }

    public ElectionStatus getStatut() {
        return statut;
    }

    public void setStatut(ElectionStatus statut) {
        this.statut = statut;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}
