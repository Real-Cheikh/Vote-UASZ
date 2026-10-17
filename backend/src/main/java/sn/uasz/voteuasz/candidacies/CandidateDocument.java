package sn.uasz.voteuasz.candidacies;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "candidate_documents")
public class CandidateDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidacy_id", nullable = false)
    @JsonIgnore
    private Candidacy candidacy;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 50)
    private DocumentType typeDocument;

    @Column(name = "nom_fichier_original", nullable = false)
    private String nomFichierOriginal;

    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

    @Column(name = "type_mime", nullable = false, length = 100)
    private String typeMime;

    @Column(name = "taille_octets", nullable = false)
    private Long tailleOctets;

    @Column(name = "date_upload", nullable = false, updatable = false)
    private OffsetDateTime dateUpload = OffsetDateTime.now();

    public CandidateDocument() {
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

    public DocumentType getTypeDocument() {
        return typeDocument;
    }

    public void setTypeDocument(DocumentType typeDocument) {
        this.typeDocument = typeDocument;
    }

    public String getNomFichierOriginal() {
        return nomFichierOriginal;
    }

    public void setNomFichierOriginal(String nomFichierOriginal) {
        this.nomFichierOriginal = nomFichierOriginal;
    }

    public String getCheminFichier() {
        return cheminFichier;
    }

    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
    }

    public String getTypeMime() {
        return typeMime;
    }

    public void setTypeMime(String typeMime) {
        this.typeMime = typeMime;
    }

    public Long getTailleOctets() {
        return tailleOctets;
    }

    public void setTailleOctets(Long tailleOctets) {
        this.tailleOctets = tailleOctets;
    }

    public OffsetDateTime getDateUpload() {
        return dateUpload;
    }

    public void setDateUpload(OffsetDateTime dateUpload) {
        this.dateUpload = dateUpload;
    }
}
