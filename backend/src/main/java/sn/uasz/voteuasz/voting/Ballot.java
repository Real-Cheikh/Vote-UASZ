package sn.uasz.voteuasz.voting;

import jakarta.persistence.*;
import sn.uasz.voteuasz.candidacies.Candidacy;
import sn.uasz.voteuasz.elections.Election;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ballots")
public class Ballot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidacy_id")
    private Candidacy candidacy; // NULL en cas de vote blanc

    @Column(name = "est_blanc", nullable = false)
    private boolean estBlanc = false;

    @Column(name = "recu_vote_hash", nullable = false, unique = true, length = 64)
    private String recuVoteHash; // Empreinte de reçu SHA-256 permettant à l'électeur de vérifier son dépôt

    @Column(name = "date_enregistrement", nullable = false, updatable = false)
    private OffsetDateTime dateEnregistrement = OffsetDateTime.now();

    public Ballot() {
    }

    public Ballot(Election election, Candidacy candidacy, boolean estBlanc, String recuVoteHash) {
        this.election = election;
        this.candidacy = candidacy;
        this.estBlanc = estBlanc;
        this.recuVoteHash = recuVoteHash;
        this.dateEnregistrement = OffsetDateTime.now();
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

    public Candidacy getCandidacy() {
        return candidacy;
    }

    public void setCandidacy(Candidacy candidacy) {
        this.candidacy = candidacy;
    }

    public boolean isEstBlanc() {
        return estBlanc;
    }

    public void setEstBlanc(boolean estBlanc) {
        this.estBlanc = estBlanc;
    }

    public String getRecuVoteHash() {
        return recuVoteHash;
    }

    public void setRecuVoteHash(String recuVoteHash) {
        this.recuVoteHash = recuVoteHash;
    }

    public OffsetDateTime getDateEnregistrement() {
        return dateEnregistrement;
    }

    public void setDateEnregistrement(OffsetDateTime dateEnregistrement) {
        this.dateEnregistrement = dateEnregistrement;
    }
}
