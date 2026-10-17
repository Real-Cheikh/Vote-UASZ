package sn.uasz.voteuasz.colleges;

import jakarta.persistence.*;
import sn.uasz.voteuasz.users.UserCategory;

@Entity
@Table(name = "colleges")
public class College {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String ufr;

    @Column(length = 100)
    private String departement;

    @Column(length = 50)
    private String niveau;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserCategory categorie;

    public College() {
    }

    public College(String code, String nom, String description, String ufr, String departement, String niveau, UserCategory categorie) {
        this.code = code;
        this.nom = nom;
        this.description = description;
        this.ufr = ufr;
        this.departement = departement;
        this.niveau = niveau;
        this.categorie = categorie;
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
