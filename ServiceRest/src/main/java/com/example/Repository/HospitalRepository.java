package com.example.Repository;

import com.example.Model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    // Recherche par ville
    List<Hospital> findByVille(String ville);

    // Recherche par spécialité (corrigé pour List<String>)
    @Query("SELECT h FROM Hospital h JOIN h.specialitesPrincipales s WHERE s LIKE %?1%")
    List<Hospital> findBySpecialiteContaining(String specialite);

    // Recherche des hôpitaux avec urgences ouvertes
    List<Hospital> findByUrgenceOuvertTrue();

    // Recherche par niveau de surcharge
    List<Hospital> findByNiveauSurcharge(String niveauSurcharge);

    // Trouver les hôpitaux avec lits disponibles
    @Query("SELECT h FROM Hospital h WHERE h.litsDisponibles > 0")
    List<Hospital> findWithAvailableBeds();

    // Trouver les hôpitaux par proximité géographique (approximatif)
    @Query("SELECT h FROM Hospital h WHERE h.latitude BETWEEN ?1 - 0.1 AND ?1 + 0.1 " +
            "AND h.longitude BETWEEN ?2 - 0.1 AND ?2 + 0.1")
    List<Hospital> findByProximity(Double latitude, Double longitude);

    // Recherche par plusieurs critères
    List<Hospital> findByVilleAndUrgenceOuvertTrueAndLitsDisponiblesGreaterThan(String ville, Integer minLits);

    // Nouvelle méthode: recherche par nombre minimum de lits disponibles
    List<Hospital> findByLitsDisponiblesGreaterThan(Integer minLits);
}