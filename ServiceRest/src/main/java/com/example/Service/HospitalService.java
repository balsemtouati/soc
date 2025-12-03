package com.example.Service;

import com.example.Model.Hospital;
import com.example.Repository.HospitalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    // CREATE
    public Hospital createHospital(Hospital hospital) {
        return hospitalRepository.save(hospital);
    }

    // READ BY ID
    public Hospital getHospitalById(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hôpital non trouvé avec l'id: " + id));
    }

    // READ ALL
    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }

    // UPDATE COMPLET
    public Hospital updateHospital(Long id, Hospital hospitalDetails) {
        Hospital hospital = getHospitalById(id);

        // Mettre à jour tous les champs
        if (hospitalDetails.getNomHopital() != null) {
            hospital.setNomHopital(hospitalDetails.getNomHopital());
        }
        if (hospitalDetails.getType() != null) {
            hospital.setType(hospitalDetails.getType());
        }
        if (hospitalDetails.getVille() != null) {
            hospital.setVille(hospitalDetails.getVille());
        }
        if (hospitalDetails.getTelephone() != null) {
            hospital.setTelephone(hospitalDetails.getTelephone());
        }
        if (hospitalDetails.getAdresse() != null) {
            hospital.setAdresse(hospitalDetails.getAdresse());
        }
        if (hospitalDetails.getLitsTotal() != null) {
            hospital.setLitsTotal(hospitalDetails.getLitsTotal());
        }
        if (hospitalDetails.getLitsOccupees() != null) {
            hospital.setLitsOccupees(hospitalDetails.getLitsOccupees());
            // Recalculer lits disponibles
            if (hospital.getLitsTotal() != null) {
                hospital.setLitsDisponibles(hospital.getLitsTotal() - hospitalDetails.getLitsOccupees());
            }
        }
        if (hospitalDetails.getSpecialitesPrincipales() != null && !hospitalDetails.getSpecialitesPrincipales().isEmpty()) {
            hospital.setSpecialitesPrincipales(hospitalDetails.getSpecialitesPrincipales());
        }
        if (hospitalDetails.getLatitude() != null) {
            hospital.setLatitude(hospitalDetails.getLatitude());
        }
        if (hospitalDetails.getLongitude() != null) {
            hospital.setLongitude(hospitalDetails.getLongitude());
        }
        if (hospitalDetails.getUrgenceOuvert() != null) {
            hospital.setUrgenceOuvert(hospitalDetails.getUrgenceOuvert());
        }
        if (hospitalDetails.getTempsAttenteUrgence() != null) {
            hospital.setTempsAttenteUrgence(hospitalDetails.getTempsAttenteUrgence());
        }
        if (hospitalDetails.getNiveauSurcharge() != null) {
            hospital.setNiveauSurcharge(hospitalDetails.getNiveauSurcharge());
        }
        if (hospitalDetails.getNbMedecinsDisponibles() != null) {
            hospital.setNbMedecinsDisponibles(hospitalDetails.getNbMedecinsDisponibles());
        }
        if (hospitalDetails.getNbInfirmiersDisponibles() != null) {
            hospital.setNbInfirmiersDisponibles(hospitalDetails.getNbInfirmiersDisponibles());
        }
        if (hospitalDetails.getNbAmbulancesDisponibles() != null) {
            hospital.setNbAmbulancesDisponibles(hospitalDetails.getNbAmbulancesDisponibles());
        }
        if (hospitalDetails.getRespirateursDisponibles() != null) {
            hospital.setRespirateursDisponibles(hospitalDetails.getRespirateursDisponibles());
        }
        if (hospitalDetails.getBlocOperatoireDisponible() != null) {
            hospital.setBlocOperatoireDisponible(hospitalDetails.getBlocOperatoireDisponible());
        }

        return hospitalRepository.save(hospital);
    }

    // DELETE
    public void deleteHospital(Long id) {
        Hospital hospital = getHospitalById(id);
        hospitalRepository.delete(hospital);
    }

    // MÉTHODES MÉTIERS SPÉCIFIQUES
    public List<Hospital> getHospitalsByVille(String ville) {
        return hospitalRepository.findByVille(ville);
    }

    public List<Hospital> getHospitalsWithAvailableBeds() {
        return hospitalRepository.findWithAvailableBeds();
    }

    public List<Hospital> getEmergencyHospitals() {
        return hospitalRepository.findByUrgenceOuvertTrue();
    }

    public Hospital updateBedStatus(Long id, Integer litsOccupees) {
        Hospital hospital = getHospitalById(id);
        hospital.setLitsOccupees(litsOccupees);
        // Recalculer les lits disponibles
        if (hospital.getLitsTotal() != null) {
            hospital.setLitsDisponibles(hospital.getLitsTotal() - litsOccupees);
        }
        return hospitalRepository.save(hospital);
    }

    // MÉTHODES MANQUANTES - COMPLÉTÉES

    public List<Hospital> getHospitalsBySpecialite(String specialite) {
        return hospitalRepository.findBySpecialiteContaining(specialite);
    }

    public List<Hospital> findNearbyHospitals(Double latitude, Double longitude, Double radiusKm) {
        // Convertir radiusKm en degrés (approximation: 1° ≈ 111km)
        Double radiusDegrees = radiusKm / 111.0;

        return hospitalRepository.findByProximity(latitude, longitude)
                .stream()
                .filter(h -> calculateDistance(latitude, longitude, h.getLatitude(), h.getLongitude()) <= radiusKm)
                .collect(Collectors.toList());
    }

    public List<Hospital> getHospitalsBySurchargeLevel(String niveau) {
        return hospitalRepository.findByNiveauSurcharge(niveau);
    }



    // Méthodes utilitaires
    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }

        final int R = 6371; // Rayon de la Terre en km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private int surchargePriority(String niveau) {
        if (niveau == null) return 4;
        switch (niveau.toLowerCase()) {
            case "faible": return 1;
            case "moyen": return 2;
            case "élevé": return 3;
            case "critique": return 4;
            default: return 5;
        }
    }

    // NOUVELLES MÉTHODES UTILES

    public List<Hospital> getHospitalsWithMinBeds(Integer minBeds) {
        return hospitalRepository.findByLitsDisponiblesGreaterThan(minBeds);
    }

    public List<Hospital> searchHospitals(String ville, String specialite, Boolean urgence, Integer minLits) {
        List<Hospital> results = getAllHospitals();

        if (ville != null && !ville.isEmpty()) {
            results = results.stream()
                    .filter(h -> ville.equalsIgnoreCase(h.getVille()))
                    .collect(Collectors.toList());
        }

        if (specialite != null && !specialite.isEmpty()) {
            results = results.stream()
                    .filter(h -> h.getSpecialitesPrincipales() != null &&
                            h.getSpecialitesPrincipales().stream()
                                    .anyMatch(s -> s.toLowerCase().contains(specialite.toLowerCase())))
                    .collect(Collectors.toList());
        }

        if (urgence != null) {
            results = results.stream()
                    .filter(h -> h.getUrgenceOuvert() != null && h.getUrgenceOuvert().equals(urgence))
                    .collect(Collectors.toList());
        }

        if (minLits != null && minLits > 0) {
            results = results.stream()
                    .filter(h -> h.getLitsDisponibles() != null && h.getLitsDisponibles() >= minLits)
                    .collect(Collectors.toList());
        }

        return results;
    }

    public Hospital updateHospitalResources(Long id, Integer medecins, Integer infirmiers,
                                            Integer ambulances, Integer respirateurs) {
        Hospital hospital = getHospitalById(id);

        if (medecins != null) hospital.setNbMedecinsDisponibles(medecins);
        if (infirmiers != null) hospital.setNbInfirmiersDisponibles(infirmiers);
        if (ambulances != null) hospital.setNbAmbulancesDisponibles(ambulances);
        if (respirateurs != null) hospital.setRespirateursDisponibles(respirateurs);

        return hospitalRepository.save(hospital);
    }
}