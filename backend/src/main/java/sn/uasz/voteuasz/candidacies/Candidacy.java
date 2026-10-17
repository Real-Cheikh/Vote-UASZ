package sn.uasz.voteuasz.candidacies;

import jakarta.persistence.*;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.users.User;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidacies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_candidacies_election_user", columnNames = {"election_id", "candidat_id"})
})
public class Candidacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "candidat_id", nullable = false)
    private User candidat;

    @Column(name = "profession_foi", columnDefinition = "TEXT")
    private String professionFoi;

    @Column(columnDefinition = "TEXT")
    private String biographie;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CandidacyStatus statut = CandidacyStatus.SUBMITTED;

    @Column(name = "motif_decision", columnDefinition = "TEXT")
    private String motifDecision;

    @Column(name = "date_decision")
    private OffsetDateTime dateDecision;

    @Column(name = "date_soumission", nullable = false, updatable = false)
    private OffsetDateTime dateSoumission = OffsetDateTime.now();

    @OneToMany(mappedBy = "candidacy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateDocument> documents = new ArrayList<>();

    public Candidacy() {
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

    public User getCandidat() {
        return candidat;
    }

    public void setCandidat(User candidat) {
        this.candidat = candidat;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public CandidacyStatus getStatut() {
        return statut;
    }

    public void setStatut(CandidacyStatus statut) {
        this.statut = statut;
    }

    public String getMotifDecision() {
        return motifDecision;
    }

    public void setMotifDecision(String motifDecision) {
        this.motifDecision = motifDecision;
    }

    public OffsetDateTime getDateDecision() {
        return dateDecision;
    }

    public void setDateDecision(OffsetDateTime dateDecision) {
        this.dateDecision = dateDecision;
    }

    public OffsetDateTime getDateSoumission() {
        return dateSoumission;
    }

    public void setDateSoumission(OffsetDateTime dateSoumission) {
        this.dateSoumission = dateSoumission;
    }

    public List<CandidateDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<CandidateDocument> documents) {
        this.documents = documents;
    }

    public void addDocument(CandidateDocument doc) {
        this.documents.add(doc);
        doc.setCandidacy(this);
    }
}
