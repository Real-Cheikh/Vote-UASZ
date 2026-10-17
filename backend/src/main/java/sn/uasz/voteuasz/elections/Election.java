package sn.uasz.voteuasz.elections;

import jakarta.persistence.*;
import sn.uasz.voteuasz.colleges.College;

import java.time.OffsetDateTime;

@Entity
@Table(name = "elections")
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ElectionType type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @Column(name = "date_ouverture", nullable = false)
    private OffsetDateTime dateOuverture;

    @Column(name = "date_cloture", nullable = false)
    private OffsetDateTime dateCloture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ElectionStatus statut = ElectionStatus.DRAFT;

    @Column(name = "permettre_vote_blanc", nullable = false)
    private boolean permettreVoteBlanc = true;

    @Column(name = "description_regles", columnDefinition = "TEXT")
    private String descriptionRegles;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation = OffsetDateTime.now();

    @OneToOne(mappedBy = "election", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ElectionRule rules;

    public Election() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ElectionType getType() {
        return type;
    }

    public void setType(ElectionType type) {
        this.type = type;
    }

    public College getCollege() {
        return college;
    }

    public void setCollege(College college) {
        this.college = college;
    }

    public OffsetDateTime getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(OffsetDateTime dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public OffsetDateTime getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(OffsetDateTime dateCloture) {
        this.dateCloture = dateCloture;
    }

    public ElectionStatus getStatut() {
        return statut;
    }

    public void setStatut(ElectionStatus statut) {
        this.statut = statut;
    }

    public boolean isPermettreVoteBlanc() {
        return permettreVoteBlanc;
    }

    public void setPermettreVoteBlanc(boolean permettreVoteBlanc) {
        this.permettreVoteBlanc = permettreVoteBlanc;
    }

    public String getDescriptionRegles() {
        return descriptionRegles;
    }

    public void setDescriptionRegles(String descriptionRegles) {
        this.descriptionRegles = descriptionRegles;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public ElectionRule getRules() {
        return rules;
    }

    public void setRules(ElectionRule rules) {
        this.rules = rules;
        if (rules != null) {
            rules.setElection(this);
        }
    }
}
