package sn.uasz.voteuasz.colleges;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sn.uasz.voteuasz.users.UserCategory;

public class CollegeDto {

    private Long id;

    @NotBlank(message = "Le code du collège électoral est obligatoire")
    private String code;

    @NotBlank(message = "Le nom du collège électoral est obligatoire")
    private String nom;

    private String description;
    private String ufr;
    private String departement;
    private String niveau;

    @NotNull(message = "La catégorie universitaire est obligatoire")
    private UserCategory categorie;

    public CollegeDto() {
    }

    public static CollegeDto fromEntity(College college) {
        CollegeDto dto = new CollegeDto();
        dto.setId(college.getId());
        dto.setCode(college.getCode());
        dto.setNom(college.getNom());
        dto.setDescription(college.getDescription());
        dto.setUfr(college.getUfr());
        dto.setDepartement(college.getDepartement());
        dto.setNiveau(college.getNiveau());
        dto.setCategorie(college.getCategorie());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUfr() {
        return ufr;
    }

    public void setUfr(String ufr) {
        this.ufr = ufr;
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = departement;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public UserCategory getCategorie() {
        return categorie;
    }

    public void setCategorie(UserCategory categorie) {
        this.categorie = categorie;
    }
}
