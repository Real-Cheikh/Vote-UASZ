package sn.uasz.voteuasz.candidacies;

import java.time.OffsetDateTime;

public class CandidateDocumentDto {
    private Long id;
    private DocumentType typeDocument;
    private String nomFichierOriginal;
    private String cheminFichier;
    private String typeMime;
    private Long tailleOctets;
    private OffsetDateTime dateUpload;

    public CandidateDocumentDto() {
    }

    public static CandidateDocumentDto fromEntity(CandidateDocument doc) {
        CandidateDocumentDto dto = new CandidateDocumentDto();
        dto.setId(doc.getId());
        dto.setTypeDocument(doc.getTypeDocument());
        dto.setNomFichierOriginal(doc.getNomFichierOriginal());
        dto.setCheminFichier(doc.getCheminFichier());
        dto.setTypeMime(doc.getTypeMime());
        dto.setTailleOctets(doc.getTailleOctets());
        dto.setDateUpload(doc.getDateUpload());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
