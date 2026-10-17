package sn.uasz.voteuasz.complaints;

import jakarta.persistence.*;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.users.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String objet;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintStatus statut = ComplaintStatus.SUBMITTED;

    @Column(name = "reponse_commission", columnDefinition = "TEXT")
    private String reponseCommission;

    @Column(name = "pieces_jointes_urls", columnDefinition = "TEXT")
    private String piecesJointesUrls;

    @Column(name = "date_depot", nullable = false, updatable = false)
    private OffsetDateTime dateDepot = OffsetDateTime.now();

    @Column(name = "date_traitement")
    private OffsetDateTime dateTraitement;

    public Complaint() {
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getPiecesJointesUrls() {
        return piecesJointesUrls;
    }

    public void setPiecesJointesUrls(String piecesJointesUrls) {
        this.piecesJointesUrls = piecesJointesUrls;
    }

    public OffsetDateTime getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(OffsetDateTime dateDepot) {
        this.dateDepot = dateDepot;
    }

    public OffsetDateTime getDateTraitement() {
        return dateTraitement;
    }

    public void setDateTraitement(OffsetDateTime dateTraitement) {
        this.dateTraitement = dateTraitement;
    }
}
