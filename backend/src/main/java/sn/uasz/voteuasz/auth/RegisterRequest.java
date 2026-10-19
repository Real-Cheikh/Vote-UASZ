package sn.uasz.voteuasz.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "L'adresse email institutionnelle est obligatoire")
    @Email(message = "Format d'adresse email invalide")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@zig\\.univ\\.sn$",
            message = "Seules les adresses institutionnelles officielles se terminant par @zig.univ.sn sont autorisées"
    )
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit comporter au moins 8 caractères")
    private String password;

    @NotBlank(message = "Le nom de famille est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le numéro d'étudiant ou d'identification est obligatoire")
    private String numeroIdentification;

    private String telephone;

    public RegisterRequest() {
    }

    public RegisterRequest(String email, String password, String nom, String prenom, String numeroIdentification, String telephone) {
        this.email = email;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
        this.numeroIdentification = numeroIdentification;
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
