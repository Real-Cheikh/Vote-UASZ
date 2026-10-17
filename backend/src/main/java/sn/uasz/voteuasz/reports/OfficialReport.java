package sn.uasz.voteuasz.reports;

import jakarta.persistence.*;
import sn.uasz.voteuasz.elections.Election;

import java.time.OffsetDateTime;

@Entity
@Table(name = "official_reports")
public class OfficialReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_rapport", nullable = false, unique = true, length = 80)
    private String numeroRapport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(name = "chemin_pdf", nullable = false, length = 255)
    private String cheminPdf;

    @Column(name = "empreinte_sha256", nullable = false, length = 64)
    private String empreinteSha256;

    @Column(name = "date_generation", nullable = false, updatable = false)
    private OffsetDateTime dateGeneration = OffsetDateTime.now();

    @Column(name = "signataire_nom", length = 120)
    private String signataireNom;

    @Column(name = "signataire_role", length = 80)
    private String signataireRole;

    public OfficialReport() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroRapport() {
        return numeroRapport;
    }

    public void setNumeroRapport(String numeroRapport) {
        this.numeroRapport = numeroRapport;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getCheminPdf() {
        return cheminPdf;
    }

    public void setCheminPdf(String cheminPdf) {
        this.cheminPdf = cheminPdf;
    }

    public String getEmpreinteSha256() {
        return empreinteSha256;
    }

    public void setEmpreinteSha256(String empreinteSha256) {
        this.empreinteSha256 = empreinteSha256;
    }

    public OffsetDateTime getDateGeneration() {
        return dateGeneration;
    }

    public void setDateGeneration(OffsetDateTime dateGeneration) {
        this.dateGeneration = dateGeneration;
    }

    public String getSignataireNom() {
        return signataireNom;
    }

    public void setSignataireNom(String signataireNom) {
        this.signataireNom = signataireNom;
    }

    public String getSignataireRole() {
        return signataireRole;
    }

    public void setSignataireRole(String signataireRole) {
        this.signataireRole = signataireRole;
    }
}
