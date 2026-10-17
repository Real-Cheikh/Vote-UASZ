package sn.uasz.voteuasz.elections;

import sn.uasz.voteuasz.colleges.CollegeDto;

import java.time.OffsetDateTime;

public class ElectionDto {

    private Long id;
    private String titre;
    private String description;
    private ElectionType type;
    private CollegeDto college;
    private OffsetDateTime dateOuverture;
    private OffsetDateTime dateCloture;
    private ElectionStatus statut;
    private boolean permettreVoteBlanc;
    private String descriptionRegles;
    private OffsetDateTime dateCreation;

    // Règles
    private String ufrCible;
    private String departementCible;
    private String niveauRequis;
    private String categorieRequise;

    public ElectionDto() {
    }

    public static ElectionDto fromEntity(Election election) {
        ElectionDto dto = new ElectionDto();
        dto.setId(election.getId());
        dto.setTitre(election.getTitre());
        dto.setDescription(election.getDescription());
        dto.setType(election.getType());
        if (election.getCollege() != null) {
            dto.setCollege(CollegeDto.fromEntity(election.getCollege()));
        }
        dto.setDateOuverture(election.getDateOuverture());
        dto.setDateCloture(election.getDateCloture());
        dto.setStatut(election.getStatut());
        dto.setPermettreVoteBlanc(election.isPermettreVoteBlanc());
        dto.setDescriptionRegles(election.getDescriptionRegles());
        dto.setDateCreation(election.getDateCreation());

        if (election.getRules() != null) {
            dto.setUfrCible(election.getRules().getUfrCible());
            dto.setDepartementCible(election.getRules().getDepartementCible());
            dto.setNiveauRequis(election.getRules().getNiveauRequis());
            dto.setCategorieRequise(election.getRules().getCategorieRequise());
        }
        return dto;
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

    public CollegeDto getCollege() {
        return college;
    }

    public void setCollege(CollegeDto college) {
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

    public String getUfrCible() {
        return ufrCible;
    }

    public void setUfrCible(String ufrCible) {
        this.ufrCible = ufrCible;
    }

    public String getDepartementCible() {
        return departementCible;
    }

    public void setDepartementCible(String departementCible) {
        this.departementCible = departementCible;
    }

    public String getNiveauRequis() {
        return niveauRequis;
    }

    public void setNiveauRequis(String niveauRequis) {
        this.niveauRequis = niveauRequis;
    }

    public String getCategorieRequise() {
        return categorieRequise;
    }

    public void setCategorieRequise(String categorieRequise) {
        this.categorieRequise = categorieRequise;
    }
}
