package sn.uasz.voteuasz.candidacies;

import jakarta.validation.constraints.NotNull;

public class CandidacyDecisionDto {

    @NotNull(message = "La décision est obligatoire (VALIDATED, REJECTED)")
    private CandidacyStatus decision;

    private String motif;

    public CandidacyDecisionDto() {
    }

    public CandidacyDecisionDto(CandidacyStatus decision, String motif) {
        this.decision = decision;
        this.motif = motif;
    }

    public CandidacyStatus getDecision() {
        return decision;
    }

    public void setDecision(CandidacyStatus decision) {
        this.decision = decision;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}
