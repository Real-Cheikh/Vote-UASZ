package sn.uasz.voteuasz.results;

import java.time.OffsetDateTime;
import java.util.List;

public class ElectionResultDto {

    private Long id;
    private Long electionId;
    private String electionTitre;
    private int inscritsTotal;
    private int votantsTotal;
    private int bulletinsValides;
    private int bulletinsBlancs;
    private double tauxParticipation;
    private CountingStatus statutDepouillement;
    private OffsetDateTime dateDepouillement;
    private OffsetDateTime dateValidation;
    private List<CandidateResultDto> resultatsCandidats;

    public ElectionResultDto() {
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

    public String getElectionTitre() {
        return electionTitre;
    }

    public void setElectionTitre(String electionTitre) {
        this.electionTitre = electionTitre;
    }

    public int getInscritsTotal() {
        return inscritsTotal;
    }

    public void setInscritsTotal(int inscritsTotal) {
        this.inscritsTotal = inscritsTotal;
    }

    public int getVotantsTotal() {
        return votantsTotal;
    }

    public void setVotantsTotal(int votantsTotal) {
        this.votantsTotal = votantsTotal;
    }

    public int getBulletinsValides() {
        return bulletinsValides;
    }

    public void setBulletinsValides(int bulletinsValides) {
        this.bulletinsValides = bulletinsValides;
    }

    public int getBulletinsBlancs() {
        return bulletinsBlancs;
    }

    public void setBulletinsBlancs(int bulletinsBlancs) {
        this.bulletinsBlancs = bulletinsBlancs;
    }

    public double getTauxParticipation() {
        return tauxParticipation;
    }

    public void setTauxParticipation(double tauxParticipation) {
        this.tauxParticipation = tauxParticipation;
    }

    public CountingStatus getStatutDepouillement() {
        return statutDepouillement;
    }

    public void setStatutDepouillement(CountingStatus statutDepouillement) {
        this.statutDepouillement = statutDepouillement;
    }

    public OffsetDateTime getDateDepouillement() {
        return dateDepouillement;
    }

    public void setDateDepouillement(OffsetDateTime dateDepouillement) {
        this.dateDepouillement = dateDepouillement;
    }

    public OffsetDateTime getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(OffsetDateTime dateValidation) {
        this.dateValidation = dateValidation;
    }

    public List<CandidateResultDto> getResultatsCandidats() {
        return resultatsCandidats;
    }

    public void setResultatsCandidats(List<CandidateResultDto> resultatsCandidats) {
        this.resultatsCandidats = resultatsCandidats;
    }
}
