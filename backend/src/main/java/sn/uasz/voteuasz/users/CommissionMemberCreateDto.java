package sn.uasz.voteuasz.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CommissionMemberCreateDto {

    @NotBlank(message = "L'adresse email institutionnelle est obligatoire")
    @Email(message = "Format d'adresse email invalide")
    private String email;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 80, message = "Le nom ne doit pas dépasser 80 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 80, message = "Le prénom ne doit pas dépasser 80 caractères")
    private String prenom;

    @NotBlank(message = "Le numéro d'identification institutionnel (matricule ou CIN) est obligatoire")
    @Size(max = 50, message = "Le numéro d'identification ne doit pas dépasser 50 caractères")
    private String numeroIdentification;

    private String telephone;

    @NotNull(message = "La catégorie universitaire est obligatoire (ex: ENSEIGNANT ou PAT)")
    private UserCategory categorie;

    public CommissionMemberCreateDto() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNumeroIdentification() {
        return numeroIdentification;
    }

    public void setNumeroIdentification(String numeroIdentification) {
        this.numeroIdentification = numeroIdentification;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public UserCategory getCategorie() {
        return categorie;
    }

    public void setCategorie(UserCategory categorie) {
        this.categorie = categorie;
    }
}
