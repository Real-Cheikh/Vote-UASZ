package sn.uasz.voteuasz.candidacies;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CandidacyApplyDto {

    @NotNull(message = "L'identifiant du scrutin est obligatoire")
    private Long electionId;

    @NotBlank(message = "La profession de foi est obligatoire")
    private String professionFoi;

    private String biographie;

    public CandidacyApplyDto() {
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public String getProfessionFoi() {
        return professionFoi;
    }

    public void setProfessionFoi(String professionFoi) {
        this.professionFoi = professionFoi;
    }

    public String getBiographie() {
        return biographie;
    }

    public void setBiographie(String biographie) {
        this.biographie = biographie;
    }
}
