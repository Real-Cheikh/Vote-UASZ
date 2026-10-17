package sn.uasz.voteuasz.results;

import jakarta.persistence.*;
import sn.uasz.voteuasz.elections.Election;

import java.time.OffsetDateTime;

@Entity
@Table(name = "election_results")
public class ElectionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false, unique = true)
    private Election election;

    @Column(name = "inscrits_total", nullable = false)
    private int inscritsTotal;

    @Column(name = "votants_total", nullable = false)
    private int votantsTotal;

    @Column(name = "bulletins_valides", nullable = false)
    private int bulletinsValides;

    @Column(name = "bulletins_blancs", nullable = false)
    private int bulletinsBlancs;

    @Column(name = "taux_participation", nullable = false)
    private double tauxParticipation;

    @Column(name = "donnees_candidats_json", columnDefinition = "TEXT", nullable = false)
    private String donneesCandidatsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_depouillement", nullable = false, length = 40)
    private CountingStatus statutDepouillement = CountingStatus.PROVISIONAL;

    @Column(name = "date_depouillement", nullable = false)
    private OffsetDateTime dateDepouillement = OffsetDateTime.now();

    @Column(name = "date_validation")
    private OffsetDateTime dateValidation;

    public ElectionResult() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
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

    public String getDonneesCandidatsJson() {
        return donneesCandidatsJson;
    }

    public void setDonneesCandidatsJson(String donneesCandidatsJson) {
        this.donneesCandidatsJson = donneesCandidatsJson;
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
}
