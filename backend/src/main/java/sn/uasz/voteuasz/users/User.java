package sn.uasz.voteuasz.users;

import jakarta.persistence.*;
import sn.uasz.voteuasz.roles.Role;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(nullable = false, length = 80)
    private String prenom;

    @Column(name = "numero_identification", unique = true, length = 50)
    private String numeroIdentification; // Numéro étudiant (ex: 20210045) ou matricule personnel UASZ

    @Column(length = 30)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserCategory categorie;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(nullable = false)
    private boolean bloque = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public User() {
    }

    public User(String email, String passwordHash, String nom, String prenom, String numeroIdentification,
                String telephone, UserCategory categorie) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nom = nom;
        this.prenom = prenom;
        this.numeroIdentification = numeroIdentification;
        this.telephone = telephone;
        this.categorie = categorie;
        this.actif = true;
        this.bloque = false;
        this.dateCreation = LocalDateTime.now();
    }

    // Getters et Setters
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }
}
