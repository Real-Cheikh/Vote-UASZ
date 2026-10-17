package sn.uasz.voteuasz.eligibility;

import jakarta.persistence.*;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.users.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "eligibility_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_eligibility_election_user", columnNames = {"election_id", "user_id"})
})
public class EligibilityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean eligible = true;

    @Column(name = "a_vote", nullable = false)
    private boolean aVote = false;

    @Column(name = "date_vote")
    private OffsetDateTime dateVote;

    public EligibilityRecord() {
    }

    public EligibilityRecord(Election election, User user, boolean eligible) {
        this.election = election;
        this.user = user;
        this.eligible = eligible;
        this.aVote = false;
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

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public boolean isaVote() {
        return aVote;
    }

    public void setaVote(boolean aVote) {
        this.aVote = aVote;
    }

    public OffsetDateTime getDateVote() {
        return dateVote;
    }

    public void setDateVote(OffsetDateTime dateVote) {
        this.dateVote = dateVote;
    }
}
