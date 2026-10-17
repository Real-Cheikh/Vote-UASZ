package sn.uasz.voteuasz.otp;

import jakarta.persistence.*;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.users.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @Column(name = "code_hache", nullable = false, length = 255)
    private String codeHache; // Empreinte SHA-256 ou BCrypt du code OTP : jamais de code en clair dans la BDD !

    @Column(name = "date_expiration", nullable = false)
    private OffsetDateTime dateExpiration;

    @Column(name = "tentatives_restantes", nullable = false)
    private int tentativesRestantes = 3;

    @Column(nullable = false)
    private boolean consomme = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation = OffsetDateTime.now();

    public OtpChallenge() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public String getCodeHache() {
        return codeHache;
    }

    public void setCodeHache(String codeHache) {
        this.codeHache = codeHache;
    }

    public OffsetDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(OffsetDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public int getTentativesRestantes() {
        return tentativesRestantes;
    }

    public void setTentativesRestantes(int tentativesRestantes) {
        this.tentativesRestantes = tentativesRestantes;
    }

    public boolean isConsomme() {
        return consomme;
    }

    public void setConsomme(boolean consomme) {
        this.consomme = consomme;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
