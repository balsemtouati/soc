package com.example.Controller;

import com.example.Model.Hospital;
import com.example.Service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospitals")
@CrossOrigin(origins = "*") // Pour le développement
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    // CREATE
    @PostMapping
    public ResponseEntity<Hospital> createHospital(@RequestBody Hospital hospital) {
        Hospital savedHospital = hospitalService.createHospital(hospital);
        return new ResponseEntity<>(savedHospital, HttpStatus.CREATED);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Hospital>> getAllHospitals() {
        List<Hospital> hospitals = hospitalService.getAllHospitals();
        return ResponseEntity.ok(hospitals);
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Hospital> getHospitalById(@PathVariable Long id) {
        Hospital hospital = hospitalService.getHospitalById(id);
        return ResponseEntity.ok(hospital);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Hospital> updateHospital(@PathVariable Long id,
                                                   @RequestBody Hospital hospitalDetails) {
        Hospital updatedHospital = hospitalService.updateHospital(id, hospitalDetails);
        return ResponseEntity.ok(updatedHospital);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteHospital(@PathVariable Long id) {
        hospitalService.deleteHospital(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    // ENDPOINTS MÉTIERS SPÉCIFIQUES

    @GetMapping("/ville/{ville}")
    public ResponseEntity<List<Hospital>> getHospitalsByVille(@PathVariable String ville) {
        List<Hospital> hospitals = hospitalService.getHospitalsByVille(ville);
        return ResponseEntity.ok(hospitals);
    }

    @GetMapping("/urgence/ouvert")
    public ResponseEntity<List<Hospital>> getEmergencyHospitals() {
        List<Hospital> hospitals = hospitalService.getEmergencyHospitals();
        return ResponseEntity.ok(hospitals);
    }

    @GetMapping("/lits/disponibles")
    public ResponseEntity<List<Hospital>> getHospitalsWithAvailableBeds() {
        List<Hospital> hospitals = hospitalService.getHospitalsWithAvailableBeds();
        return ResponseEntity.ok(hospitals);
    }

    @GetMapping("/specialite/{specialite}")
    public ResponseEntity<List<Hospital>> getHospitalsBySpecialite(@PathVariable String specialite) {
        List<Hospital> hospitals = hospitalService.getHospitalsBySpecialite(specialite);
        return ResponseEntity.ok(hospitals);
    }

    @GetMapping("/surcharge/{niveau}")
    public ResponseEntity<List<Hospital>> getHospitalsBySurchargeLevel(@PathVariable String niveau) {
        List<Hospital> hospitals = hospitalService.getHospitalsBySurchargeLevel(niveau);
        return ResponseEntity.ok(hospitals);
    }

    @PutMapping("/{id}/lits")
    public ResponseEntity<Hospital> updateBedStatus(@PathVariable Long id,
                                                    @RequestParam Integer litsOccupees) {
        Hospital hospital = hospitalService.updateBedStatus(id, litsOccupees);
        return ResponseEntity.ok(hospital);
    }

    @GetMapping("/proximite")
    public ResponseEntity<List<Hospital>> findNearbyHospitals(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {

        List<Hospital> hospitals = hospitalService.findNearbyHospitals(latitude, longitude, radiusKm);
        return ResponseEntity.ok(hospitals);
    }

    // ENDPOINT POUR LE WORKFLOW - Trouver l'hôpital le plus adapté
    @GetMapping("/recommandation")
    public ResponseEntity<List<Hospital>> getRecommendedHospitals(
            @RequestParam(required = false) String specialite,
            @RequestParam(required = false) String ville,
            @RequestParam(defaultValue = "false") Boolean urgence,
            @RequestParam(defaultValue = "0") Integer minLits) {

        // Logique de recommandation (simplifiée)
        List<Hospital> recommendations;

        if (urgence) {
            recommendations = hospitalService.getEmergencyHospitals();
            // Filtrer par lits disponibles si spécifié
            if (minLits > 0) {
                recommendations.removeIf(h -> h.getLitsDisponibles() < minLits);
            }
        } else if (specialite != null) {
            recommendations = hospitalService.getHospitalsBySpecialite(specialite);
        } else if (ville != null) {
            recommendations = hospitalService.getHospitalsByVille(ville);
        } else {
            recommendations = hospitalService.getAllHospitals();
        }

        return ResponseEntity.ok(recommendations);
    }
}
