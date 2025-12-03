package com.example.Service;

import com.example.Model.Hospital;
import com.example.Repository.HospitalRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class CsvDataLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(CsvDataLoaderService.class);

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${csv.file.path:data/hospitaldata.csv}")
    private String csvFilePath;

    @PostConstruct
    public void init() {
        logger.info("🚀 Initialisation du chargement CSV...");
        loadHospitalsFromCsv();
    }

    public void loadHospitalsFromCsv() {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + csvFilePath);

            if (!resource.exists()) {
                logger.error("❌ Fichier CSV non trouvé: {}", csvFilePath);
                return;
            }

            logger.info("📁 Fichier CSV trouvé: {}", resource.getURI());

            // Lire le contenu brut pour vérifier le BOM
            byte[] fileBytes = StreamUtils.copyToByteArray(resource.getInputStream());
            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);

            // Vérifier et supprimer le BOM si présent
            if (fileContent.startsWith("\uFEFF")) {
                logger.info("⚠️ BOM détecté dans le fichier CSV, suppression...");
                fileContent = fileContent.substring(1); // Supprimer le BOM
            }

            // Lire les premières lignes pour debug
            String[] lines = fileContent.split("\n", 4);
            logger.info("📄 Premières lignes (sans BOM):");
            for (int i = 0; i < Math.min(3, lines.length); i++) {
                logger.info("Ligne {}: {}", i + 1, lines[i]);
            }

            // Utiliser le contenu nettoyé
            Reader reader = new StringReader(fileContent);

            // Configurer le parser CSV
            CSVParser csvParser = CSVFormat.DEFAULT
                    .withDelimiter(';')
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim()
                    .withIgnoreEmptyLines()
                    .parse(reader);

            // Afficher les en-têtes CORRIGÉS (sans BOM)
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            logger.info("📊 En-têtes détectés ({}):", headerMap.size());

            List<String> cleanedHeaders = new ArrayList<>();
            for (String header : headerMap.keySet()) {
                // Nettoyer chaque en-tête des caractères invisibles
                String cleanedHeader = cleanHeader(header);
                cleanedHeaders.add(cleanedHeader);
                logger.info("  - Original: '{}' → Nettoyé: '{}'",
                        escapeSpecialChars(header), cleanedHeader);
            }

            List<Hospital> hospitals = new ArrayList<>();
            int lineNumber = 0;
            int successCount = 0;
            int errorCount = 0;

            for (CSVRecord record : csvParser) {
                lineNumber++;
                logger.info("\n--- Traitement ligne {} ---", lineNumber);

                // Afficher les données brutes pour debug
                logger.info("Données brutes: {}", record.toString());

                try {
                    Hospital hospital = mapCsvToHospital(record, lineNumber, cleanedHeaders);

                    if (hospital == null) {
                        logger.warn("⚠️ Ligne {}: Hospital est null", lineNumber);
                        errorCount++;
                        continue;
                    }

                    // Vérification CRITIQUE
                    if (hospital.getNomHopital() == null || hospital.getNomHopital().trim().isEmpty()) {
                        logger.warn("⚠️ Ligne {} ignorée: NomHopital est vide ou null", lineNumber);
                        logger.info("Hospital object: {}", hospital);
                        errorCount++;
                        continue;
                    }

                    // Calculer les lits disponibles si nécessaire
                    if (hospital.getLitsDisponibles() == null &&
                            hospital.getLitsTotal() != null &&
                            hospital.getLitsOccupees() != null) {
                        hospital.setLitsDisponibles(hospital.getLitsTotal() - hospital.getLitsOccupees());
                    }

                    hospitals.add(hospital);
                    successCount++;
                    logger.info("✅ Ligne {} traitée: {} (Ville: {}, Lits: {})",
                            lineNumber, hospital.getNomHopital(), hospital.getVille(), hospital.getLitsTotal());

                } catch (Exception e) {
                    logger.error("❌ Erreur ligne {}: {}", lineNumber, e.getMessage(), e);
                    errorCount++;
                }
            }

            if (!hospitals.isEmpty()) {
                try {
                    logger.info("💾 Sauvegarde de {} hôpitaux en base...", hospitals.size());
                    List<Hospital> savedHospitals = hospitalRepository.saveAll(hospitals);
                    logger.info("✅ SUCCÈS! {} hôpitaux sauvegardés ({} succès, {} erreurs)",
                            savedHospitals.size(), successCount, errorCount);

                    // Vérifier la base
                    long countInDb = hospitalRepository.count();
                    logger.info("📊 Nombre total d'hôpitaux en base: {}", countInDb);

                    if (countInDb > 0) {
                        List<Hospital> firstThree = hospitalRepository.findAll().stream()
                                .limit(3)
                                .toList();
                        logger.info("📋 Échantillon:");
                        for (Hospital h : firstThree) {
                            logger.info("  - ID: {}, Nom: {}, Ville: {}, Lits: {}",
                                    h.getId(), h.getNomHopital(), h.getVille(), h.getLitsTotal());
                        }
                    }

                } catch (Exception e) {
                    logger.error("❌ ERREUR lors de la sauvegarde: {}", e.getMessage(), e);
                }
            } else {
                logger.warn("⚠️ Aucun hôpital chargé (succès: {}, erreurs: {})", successCount, errorCount);
            }

            csvParser.close();
            reader.close();

        } catch (Exception e) {
            logger.error("❌ Erreur fatale lors du chargement CSV: {}", e.getMessage(), e);
        }
    }

    private Hospital mapCsvToHospital(CSVRecord record, int lineNumber, List<String> cleanedHeaders) {
        try {
            Hospital hospital = new Hospital();

            // Récupérer les noms de colonnes CORRIGÉS
            Map<String, String> recordMap = new HashMap<>();
            Map<String, Integer> headerMap = record.getParser().getHeaderMap();

            // Créer une map avec les noms nettoyés
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                String originalHeader = entry.getKey();
                String cleanedHeader = cleanHeader(originalHeader);
                String value = record.get(originalHeader);
                recordMap.put(cleanedHeader, value);
                logger.debug("Colonne: '{}' → '{}' = '{}'",
                        escapeSpecialChars(originalHeader), cleanedHeader, value);
            }

            logger.info("🔍 Noms de colonnes disponibles: {}", recordMap.keySet());

            // 1. NomHopital - GÉRER LE BOM
            String nomHopital = getValueFromMap(recordMap, lineNumber,
                    "NomHopital", "nomhopital", "nom_hopital", "nom");
            hospital.setNomHopital(nomHopital);
            logger.info("  NomHopital: '{}'", nomHopital);

            // 2. Type
            hospital.setType(getValueFromMap(recordMap, lineNumber, "Type", "type"));

            // 3. Ville - VOTRE CSV a "Ville " avec espace
            hospital.setVille(getValueFromMap(recordMap, lineNumber, "Ville", "ville", "Ville "));

            // 4. Telephone
            hospital.setTelephone(getValueFromMap(recordMap, lineNumber, "Telephone", "telephone", "tel"));

            // 5. Adresse - VOTRE CSV a "Adresse " avec espace
            hospital.setAdresse(getValueFromMap(recordMap, lineNumber, "Adresse", "adresse", "Adresse "));

            // 6. LitsTotales
            Integer litsTotal = getIntegerFromMap(recordMap, lineNumber, "LitsTotales", "litstotales", "lits_total");
            hospital.setLitsTotal(litsTotal);

            // 7. litsOccupees
            Integer litsOccupees = getIntegerFromMap(recordMap, lineNumber, "litsOccupees", "litsoccupees", "lits_occupees");
            hospital.setLitsOccupees(litsOccupees);

            // 8. litsDisponibles
            Integer litsDisponibles = getIntegerFromMap(recordMap, lineNumber, "litsDisponibles", "litsdisponibles", "lits_disponibles");
            hospital.setLitsDisponibles(litsDisponibles);

            // 9. Spécialités
            String specialites = getValueFromMap(recordMap, lineNumber,
                    "Specialites Principales", "specialites", "specialites_principales");
            if (specialites != null && !specialites.trim().isEmpty()) {
                // Votre CSV utilise ", " comme séparateur
                String[] specialitesArray = specialites.split(",\\s*");
                List<String> specialitesList = Arrays.asList(specialitesArray);
                hospital.setSpecialitesPrincipales(specialitesList);
                logger.info("  Specialites: {}", specialitesList);
            }

            // 10. Latitude
            Double latitude = getDoubleFromMap(recordMap, lineNumber, "latitude", "lat");
            hospital.setLatitude(latitude);

            // 11. Longitude
            Double longitude = getDoubleFromMap(recordMap, lineNumber, "longitude", "long", "lon");
            hospital.setLongitude(longitude);

            // 12. urgence_ouvert
            Boolean urgenceOuvert = getBooleanFromMap(recordMap, lineNumber,
                    "urgence_ouvert", "urgenceouvert", "urgence");
            hospital.setUrgenceOuvert(urgenceOuvert);

            // 13. temps_attente_urgence - VOTRE CSV a "61.0" (Double)
            Double tempsAttente = getDoubleFromMap(recordMap, lineNumber,
                    "temps_attente_urgence", "tempsattente", "temps_attente");
            if (tempsAttente != null) {
                hospital.setTempsAttenteUrgence(tempsAttente.intValue());
                logger.info("  temps_attente_urgence: {} (de {})",
                        tempsAttente.intValue(), tempsAttente);
            }

            // 14. niveau_surcharge
            hospital.setNiveauSurcharge(getValueFromMap(recordMap, lineNumber,
                    "niveau_surcharge", "niveausurcharge", "surcharge"));

            // 15-18. Les nombres disponibles
            hospital.setNbMedecinsDisponibles(getIntegerFromMap(recordMap, lineNumber,
                    "nb_medecins_disponibles", "nbmedecins"));
            hospital.setNbInfirmiersDisponibles(getIntegerFromMap(recordMap, lineNumber,
                    "nb_infirmiers_disponibles", "nbinfirmiers"));
            hospital.setNbAmbulancesDisponibles(getIntegerFromMap(recordMap, lineNumber,
                    "nb_ambulances_disponibles", "nbambulances"));
            hospital.setRespirateursDisponibles(getIntegerFromMap(recordMap, lineNumber,
                    "respirateurs_disponibles", "respirateurs"));

            // 19. bloc_operatoire_disponible
            hospital.setBlocOperatoireDisponible(getBooleanFromMap(recordMap, lineNumber,
                    "bloc_operatoire_disponible", "blocoperatoire", "bloc"));

            return hospital;

        } catch (Exception e) {
            logger.error("❌ Erreur mapping ligne {}: {}", lineNumber, e.getMessage(), e);
            return null;
        }
    }

    // Méthodes helper pour nettoyer et chercher les valeurs
    private String cleanHeader(String header) {
        if (header == null) return null;
        // Supprimer le BOM et autres caractères invisibles
        String cleaned = header.replace("\uFEFF", "")  // BOM
                .replace("\u200B", "")  // Zero-width space
                .trim();
        // Normaliser les espaces
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    private String escapeSpecialChars(String text) {
        if (text == null) return "null";
        return text.chars()
                .mapToObj(c -> c < 32 ? String.format("\\u%04x", c) : String.valueOf((char)c))
                .collect(java.util.stream.Collectors.joining());
    }

    private String getValueFromMap(Map<String, String> map, int lineNumber, String... possibleKeys) {
        for (String key : possibleKeys) {
            String value = map.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
            // Essayer sans espaces à la fin
            String trimmedKey = key.trim();
            if (!trimmedKey.equals(key)) {
                value = map.get(trimmedKey);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        logger.warn("Ligne {}: Aucune valeur pour les clés: {}", lineNumber, Arrays.toString(possibleKeys));
        return null;
    }

    private Integer getIntegerFromMap(Map<String, String> map, int lineNumber, String... possibleKeys) {
        String value = getValueFromMap(map, lineNumber, possibleKeys);
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Ligne {}: Valeur non numérique '{}' pour {}", lineNumber, value, Arrays.toString(possibleKeys));
            return null;
        }
    }

    private Double getDoubleFromMap(Map<String, String> map, int lineNumber, String... possibleKeys) {
        String value = getValueFromMap(map, lineNumber, possibleKeys);
        if (value == null) return null;
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            logger.warn("Ligne {}: Valeur non numérique '{}' pour {}", lineNumber, value, Arrays.toString(possibleKeys));
            return null;
        }
    }

    private Boolean getBooleanFromMap(Map<String, String> map, int lineNumber, String... possibleKeys) {
        String value = getValueFromMap(map, lineNumber, possibleKeys);
        if (value == null) return null;

        String lowerValue = value.toLowerCase().trim();
        if (lowerValue.equals("true") || lowerValue.equals("vrai") ||
                lowerValue.equals("1") || lowerValue.equals("oui") ||
                lowerValue.equals("yes") || value.equals("VRAI")) {
            return true;
        }
        if (lowerValue.equals("false") || lowerValue.equals("faux") ||
                lowerValue.equals("0") || lowerValue.equals("non") ||
                lowerValue.equals("no")) {
            return false;
        }
        return null;
    }
}