package sn.uasz.voteuasz.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class UserDto {
    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private String numeroIdentification;
    private String telephone;
    private UserCategory categorie;
    private boolean actif;
    private boolean bloque;
    private LocalDateTime dateCreation;
    private LocalDateTime derniereConnexion;
    private Set<String> roles;

    public UserDto() {
    }

    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setNom(user.getNom());
        dto.setPrenom(user.getPrenom());
        dto.setNumeroIdentification(user.getNumeroIdentification());
        dto.setTelephone(user.getTelephone());
        dto.setCategorie(user.getCategorie());
        dto.setActif(user.isActif());
        dto.setBloque(user.isBloque());
        dto.setDateCreation(user.getDateCreation());
        dto.setDerniereConnexion(user.getDerniereConnexion());
        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(r -> r.getName().name())
                    .collect(Collectors.toSet()));
        }
        return dto;
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

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public boolean isBloque() {
        return bloque;
    }

    public void setBloque(boolean bloque) {
        this.bloque = bloque;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDerniereConnexion() {
        return derniereConnexion;
    }

    public void setDerniereConnexion(LocalDateTime derniereConnexion) {
        this.derniereConnexion = derniereConnexion;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
