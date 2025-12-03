package com.example.Model;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "hospitals")
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_hopital", nullable = false)
    private String nomHopital;

    @Column(name = "type")
    private String type; // public, privé, etc.

    @Column(name = "ville")
    private String ville;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "lits_total")
    private Integer litsTotales;

    @Column(name = "lits_occupees")
    private Integer litsOccupees;

    @Column(name = "lits_disponibles")
    private Integer litsDisponibles;

    @ElementCollection
    @CollectionTable(name = "hospital_specialites", joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "specialite")
    private List<String> specialitesPrincipales;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "urgence_ouvert")
    private Boolean urgenceOuvert;

    @Column(name = "temps_attente_urgence")
    private Integer tempsAttenteUrgence; // en minutes

    @Column(name = "niveau_surcharge")
    private String niveauSurcharge; // "faible", "moyen", "élevé", "critique"

    @Column(name = "nb_medecins_disponibles")
    private Integer nbMedecinsDisponibles;

    @Column(name = "nb_infirmiers_disponibles")
    private Integer nbInfirmiersDisponibles;

    @Column(name = "nb_ambulances_disponibles")
    private Integer nbAmbulancesDisponibles;

    @Column(name = "respirateurs_disponibles")
    private Integer respirateursDisponibles;

    @Column(name = "bloc_operatoire_disponible")
    private Boolean blocOperatoireDisponible;

    // Calcul automatique des lits disponibles (si besoin)
    @PrePersist
    @PreUpdate
    private void calculateLitsDisponibles() {
        if (litsTotales != null && litsOccupees != null) {
            litsDisponibles = litsTotales - litsOccupees;
        }
    }


    // Constructeurs
    public Hospital() {}

    // Getters et Setters (tous)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNomHopital() { return nomHopital; }
    public void setNomHopital(String nomHopital) { this.nomHopital = nomHopital; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public Integer getLitsTotal() { return litsTotales; }
    public void setLitsTotal(Integer litsTotal) { this.litsTotales = litsTotal; }

    public Integer getLitsOccupees() { return litsOccupees; }
    public void setLitsOccupees(Integer litsOccupees) { this.litsOccupees = litsOccupees; }

    public Integer getLitsDisponibles() { return litsDisponibles; }
    public void setLitsDisponibles(Integer litsDisponibles) { this.litsDisponibles = litsDisponibles; }

    public List<String> getSpecialitesPrincipales() { return specialitesPrincipales; }
    public void setSpecialitesPrincipales(List<String> specialitesPrincipales) {
        this.specialitesPrincipales = specialitesPrincipales;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Boolean getUrgenceOuvert() { return urgenceOuvert; }
    public void setUrgenceOuvert(Boolean urgenceOuvert) { this.urgenceOuvert = urgenceOuvert; }

    public Integer getTempsAttenteUrgence() { return tempsAttenteUrgence; }
    public void setTempsAttenteUrgence(Integer tempsAttenteUrgence) { this.tempsAttenteUrgence = tempsAttenteUrgence; }

    public String getNiveauSurcharge() { return niveauSurcharge; }
    public void setNiveauSurcharge(String niveauSurcharge) { this.niveauSurcharge = niveauSurcharge; }

    public Integer getNbMedecinsDisponibles() { return nbMedecinsDisponibles; }
    public void setNbMedecinsDisponibles(Integer nbMedecinsDisponibles) { this.nbMedecinsDisponibles = nbMedecinsDisponibles; }

    public Integer getNbInfirmiersDisponibles() { return nbInfirmiersDisponibles; }
    public void setNbInfirmiersDisponibles(Integer nbInfirmiersDisponibles) { this.nbInfirmiersDisponibles = nbInfirmiersDisponibles; }

    public Integer getNbAmbulancesDisponibles() { return nbAmbulancesDisponibles; }
    public void setNbAmbulancesDisponibles(Integer nbAmbulancesDisponibles) { this.nbAmbulancesDisponibles = nbAmbulancesDisponibles; }

    public Integer getRespirateursDisponibles() { return respirateursDisponibles; }
    public void setRespirateursDisponibles(Integer respirateursDisponibles) { this.respirateursDisponibles = respirateursDisponibles; }

    public Boolean getBlocOperatoireDisponible() { return blocOperatoireDisponible; }
    public void setBlocOperatoireDisponible(Boolean blocOperatoireDisponible) { this.blocOperatoireDisponible = blocOperatoireDisponible; }
}
