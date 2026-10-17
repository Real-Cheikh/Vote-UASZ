package sn.uasz.voteuasz.campaigns;

import jakarta.persistence.*;
import sn.uasz.voteuasz.candidacies.Candidacy;

import java.time.OffsetDateTime;

@Entity
@Table(name = "campaign_contents")
public class CampaignContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidacy_id", nullable = false)
    private Candidacy candidacy;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String texte;

    @Column(name = "media_url", length = 255)
    private String mediaUrl;

    @Column(name = "type_media", length = 30)
    private String typeMedia; // IMAGE, VIDEO_LINK, TEXTE

    @Column(name = "statut_moderation", nullable = false, length = 30)
    private String statutModeration = "APPROVED";

    @Column(name = "date_publication", nullable = false, updatable = false)
    private OffsetDateTime datePublication = OffsetDateTime.now();

    public CampaignContent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Candidacy getCandidacy() {
        return candidacy;
    }

    public void setCandidacy(Candidacy candidacy) {
        this.candidacy = candidacy;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getTypeMedia() {
        return typeMedia;
    }

    public void setTypeMedia(String typeMedia) {
        this.typeMedia = typeMedia;
    }

    public String getStatutModeration() {
        return statutModeration;
    }

    public void setStatutModeration(String statutModeration) {
        this.statutModeration = statutModeration;
    }

    public OffsetDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(OffsetDateTime datePublication) {
        this.datePublication = datePublication;
    }
}
