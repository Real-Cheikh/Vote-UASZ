package sn.uasz.voteuasz.voting;

import jakarta.persistence.*;
import sn.uasz.voteuasz.elections.Election;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vote_credentials")
public class VoteCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 du jeton d'autorisation de vote anonymisé

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @Column(name = "date_expiration", nullable = false)
    private OffsetDateTime dateExpiration;

    @Column(nullable = false)
    private boolean utilise = false;

    @Column(name = "date_utilisation")
    private OffsetDateTime dateUtilisation;

    public VoteCredential() {
    }

    public VoteCredential(String tokenHash, Election election, OffsetDateTime dateExpiration) {
        this.tokenHash = tokenHash;
        this.election = election;
        this.dateExpiration = dateExpiration;
        this.utilise = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public OffsetDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(OffsetDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public boolean isUtilise() {
        return utilise;
    }

    public void setUtilise(boolean utilise) {
        this.utilise = utilise;
    }

    public OffsetDateTime getDateUtilisation() {
        return dateUtilisation;
    }

    public void setDateUtilisation(OffsetDateTime dateUtilisation) {
        this.dateUtilisation = dateUtilisation;
    }
}
