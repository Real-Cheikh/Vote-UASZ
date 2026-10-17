package sn.uasz.voteuasz.elections;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class ElectionCreateDto {

    @NotBlank(message = "Le titre de l'élection est obligatoire")
    private String titre;

    private String description;

    @NotNull(message = "Le type de scrutin est obligatoire")
    private ElectionType type;

    @NotNull(message = "Le collège électoral associé est obligatoire")
    private Long collegeId;

    @NotNull(message = "La date d'ouverture du scrutin est obligatoire")
    private OffsetDateTime dateOuverture;

    @NotNull(message = "La date de clôture du scrutin est obligatoire")
    private OffsetDateTime dateCloture;

    private boolean permettreVoteBlanc = true;
    private String descriptionRegles;

    // Règles optionnelles
    private String ufrCible;
    private String departementCible;
    private String niveauRequis;
    private String categorieRequise;

    public ElectionCreateDto() {
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

    public Long getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(Long collegeId) {
        this.collegeId = collegeId;
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
