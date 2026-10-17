package sn.uasz.voteuasz.audit;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_uuid", nullable = false, unique = true, length = 64)
    private String eventUuid;

    @Column(nullable = false)
    private OffsetDateTime horodatage = OffsetDateTime.now();

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(length = 120)
    private String acteur;

    @Column(name = "election_id")
    private Long electionId;

    @Column(nullable = false, length = 30)
    private String resultat;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_source", length = 60)
    private String ipSource;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "empreinte_chaine", length = 64)
    private String empreinteChaine;

    public AuditEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventUuid() {
        return eventUuid;
    }

    public void setEventUuid(String eventUuid) {
        this.eventUuid = eventUuid;
    }

    public OffsetDateTime getHorodatage() {
        return horodatage;
    }

    public void setHorodatage(OffsetDateTime horodatage) {
        this.horodatage = horodatage;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActeur() {
        return acteur;
    }

    public void setActeur(String acteur) {
        this.acteur = acteur;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public String getResultat() {
        return resultat;
    }

    public void setResultat(String resultat) {
        this.resultat = resultat;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpSource() {
        return ipSource;
    }

    public void setIpSource(String ipSource) {
        this.ipSource = ipSource;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getEmpreinteChaine() {
        return empreinteChaine;
    }

    public void setEmpreinteChaine(String empreinteChaine) {
        this.empreinteChaine = empreinteChaine;
    }
}
