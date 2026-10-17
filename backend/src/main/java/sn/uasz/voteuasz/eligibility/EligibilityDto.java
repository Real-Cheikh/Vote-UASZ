package sn.uasz.voteuasz.eligibility;

import sn.uasz.voteuasz.elections.ElectionDto;

import java.time.OffsetDateTime;

public class EligibilityDto {

    private Long id;
    private Long electionId;
    private ElectionDto election;
    private boolean eligible;
    private boolean aVote;
    private OffsetDateTime dateVote;
    private String motifIneligibilite;

    public EligibilityDto() {
    }

    public static EligibilityDto fromEntity(EligibilityRecord record) {
        EligibilityDto dto = new EligibilityDto();
        dto.setId(record.getId());
        dto.setElectionId(record.getElection().getId());
        dto.setElection(ElectionDto.fromEntity(record.getElection()));
        dto.setEligible(record.isEligible());
        dto.setAVote(record.isaVote());
        dto.setDateVote(record.getDateVote());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public ElectionDto getElection() {
        return election;
    }

    public void setElection(ElectionDto election) {
        this.election = election;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public boolean isAVote() {
        return aVote;
    }

    public void setAVote(boolean aVote) {
        this.aVote = aVote;
    }

    public OffsetDateTime getDateVote() {
        return dateVote;
    }

    public void setDateVote(OffsetDateTime dateVote) {
        this.dateVote = dateVote;
    }

    public String getMotifIneligibilite() {
        return motifIneligibilite;
    }

    public void setMotifIneligibilite(String motifIneligibilite) {
        this.motifIneligibilite = motifIneligibilite;
    }
}
