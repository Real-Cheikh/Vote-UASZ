package sn.uasz.voteuasz.users;

import java.util.Set;

public class CommissionMemberResponseDto {

    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private String numeroIdentification;
    private String telephone;
    private UserCategory categorie;
    private Set<String> roles;
    private String motDePasseTemporaire; // Délivré UNIQUEMENT lors de la création pour transmission sécurisée hors-bande
    private String message;

    public CommissionMemberResponseDto() {
    }

    public CommissionMemberResponseDto(Long id, String email, String nom, String prenom,
                                       String numeroIdentification, String telephone,
                                       UserCategory categorie, Set<String> roles,
                                       String motDePasseTemporaire, String message) {
        this.id = id;
        this.email = email;
        this.nom = nom;
        this.prenom = prenom;
        this.numeroIdentification = numeroIdentification;
        this.telephone = telephone;
        this.categorie = categorie;
        this.roles = roles;
        this.motDePasseTemporaire = motDePasseTemporaire;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getMotDePasseTemporaire() {
        return motDePasseTemporaire;
    }

    public void setMotDePasseTemporaire(String motDePasseTemporaire) {
        this.motDePasseTemporaire = motDePasseTemporaire;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
