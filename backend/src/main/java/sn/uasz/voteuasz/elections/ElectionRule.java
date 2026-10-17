package sn.uasz.voteuasz.elections;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "election_rules")
public class ElectionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false, unique = true)
    @JsonIgnore
    private Election election;

    @Column(name = "ufr_cible", length = 100)
    private String ufrCible;

    @Column(name = "departement_cible", length = 100)
    private String departementCible;

    @Column(name = "niveau_requis", length = 50)
    private String niveauRequis;

    @Column(name = "categorie_requise", length = 30)
    private String categorieRequise;

    @Column(name = "limite_age")
    private Integer limiteAge;

    @Column(name = "regle_personnalisee_json", columnDefinition = "TEXT")
    private String reglePersonnaliseeJson;

    public ElectionRule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
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

    public Integer getLimiteAge() {
        return limiteAge;
    }

    public void setLimiteAge(Integer limiteAge) {
        this.limiteAge = limiteAge;
    }

    public String getReglePersonnaliseeJson() {
        return reglePersonnaliseeJson;
    }

    public void setReglePersonnaliseeJson(String reglePersonnaliseeJson) {
        this.reglePersonnaliseeJson = reglePersonnaliseeJson;
    }
}
