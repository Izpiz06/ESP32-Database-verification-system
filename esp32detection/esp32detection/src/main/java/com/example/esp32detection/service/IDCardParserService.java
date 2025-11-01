package com.example.esp32detection.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.esp32detection.model.IDCardData;

@Service
public class IDCardParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(IDCardParserService.class);

    // Inner class for field extraction results with confidence scoring
    private static class FieldResult {
        String value;
        int confidence;
        
        FieldResult(String value, int confidence) {
            this.value = value;
            this.confidence = confidence;
        }
    }
    
    // Field types for validation
    private enum FieldType {
        NAME, REGISTER_NUMBER, PROGRAMME, DATE, BLOOD_GROUP, 
        PIN_CODE, PHONE, EMAIL, ADDRESS, GENERIC
    }

    public IDCardData parseIDCardText(String ocrText) {
        IDCardData idCardData = new IDCardData();
        
        // Preprocess and normalize OCR text
        String cleanedText = normalizeOCRText(ocrText);
        
        logger.debug("Processing OCR text. Length: {}", cleanedText.length());
        
        // Determine card type
        String cardType = determineCardType(cleanedText);
        idCardData.setCardType(cardType);
        
        if ("FRONT".equals(cardType)) {
            parseFrontSide(cleanedText, idCardData);
        } else if ("BACK".equals(cardType)) {
            parseBackSide(cleanedText, idCardData);
        } else {
            logger.warn("Unable to determine card type from text");
        }
        
        idCardData.setRawText(ocrText);
        return idCardData;
    }

    /**
     * Normalize common OCR errors and clean text
     */
    private String normalizeOCRText(String text) {
        if (text == null || text.isEmpty()) return "";
        
        return text
            // Replace common special character misreads
            .replaceAll("[©€]", ":")
            // Fix common number/letter confusions
            .replaceAll("8\\s*Tech", "B.Tech")
            .replaceAll("(?i)4VE", "B +ve")
            .replaceAll("(?i)apri1", "April")
            .replaceAll("(?i)0ct", "Oct")
            // Normalize whitespace
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Determine card type based on content
     */
    private String determineCardType(String text) {
        int frontScore = 0;
        int backScore = 0;
        
        // Front side indicators
        if (text.contains("FACULTY")) frontScore += 3;
        if (text.contains("Programme") || text.contains("Program")) frontScore += 2;
        if (text.contains("Register")) frontScore += 2;
        if (text.contains("Valid From") || text.contains("Valid To")) frontScore += 2;
        
        // Back side indicators
        if (text.contains("Blood Group")) backScore += 3;
        if (text.contains("Address")) backScore += 2;
        if (text.contains("Pin") && text.matches(".*Pin\\s*[:©€+]?\\s*\\d{6}.*")) backScore += 2;
        if (text.contains("Cont.No") || text.contains("Contact")) backScore += 2;
        if (text.contains("Date of Birth") || text.contains("Birth")) backScore += 2;
        
        logger.debug("Card type scoring - Front: {}, Back: {}", frontScore, backScore);
        
        return frontScore > backScore ? "FRONT" : (backScore > 0 ? "BACK" : "UNKNOWN");
    }

    private void parseFrontSide(String text, IDCardData data) {
        logger.info("Parsing front side of ID card");
        
        // Extract Institution
        String institution = "SRM INSTITUTE OF SCIENCE & TECHNOLOGY";
        data.setInstitution(institution);

        // Extract Faculty
        String faculty = "FACULTY OF ENGINEERING & TECHNOLOGY";
        data.setFaculty(faculty);

        // Extract Name with multiple patterns and validation
        String name = extractName(text);
        data.setName(validateAndClean(name, FieldType.NAME));
        logger.debug("Extracted name: {}", data.getName());

        // Extract Programme
        String programme = extractProgramme(text);
        data.setProgramme(validateAndClean(programme, FieldType.PROGRAMME));
        logger.debug("Extracted programme: {}", data.getProgramme());

        // Extract Register Number
        String registerNo = extractRegisterNumber(text);
        data.setRegisterNumber(validateAndClean(registerNo, FieldType.REGISTER_NUMBER));
        logger.debug("Extracted register number: {}", data.getRegisterNumber());

        // Extract Valid From
        String validFrom = extractValidFrom(text);
        data.setValidFrom(validateAndClean(validFrom, FieldType.DATE));
        logger.debug("Extracted valid from: {}", data.getValidFrom());

        // Extract Valid To
        String validTo = extractValidTo(text);
        data.setValidTo(validateAndClean(validTo, FieldType.DATE));
        logger.debug("Extracted valid to: {}", data.getValidTo());
    }

    private void parseBackSide(String text, IDCardData data) {
        logger.info("Parsing back side of ID card");
        
        // Extract Blood Group
        String bloodGroup = extractBloodGroup(text);
        data.setBloodGroup(validateAndClean(bloodGroup, FieldType.BLOOD_GROUP));
        logger.debug("Extracted blood group: {}", data.getBloodGroup());

        // Extract Date of Birth
        String dob = extractDateOfBirth(text);
        data.setDateOfBirth(normalizeDateOfBirth(dob));
        logger.debug("Extracted date of birth: {}", data.getDateOfBirth());

        // Extract Address
        String address = extractAddress(text);
        data.setAddress(validateAndClean(address, FieldType.ADDRESS));
        logger.debug("Extracted address: {}", data.getAddress());

        // Extract Pin
        String pin = extractPin(text);
        data.setPin(validateAndClean(pin, FieldType.PIN_CODE));
        logger.debug("Extracted pin: {}", data.getPin());

        // Extract Permanent Contact
        String permContact = extractPermanentContact(text);
        data.setPermanentContact(validateAndClean(permContact, FieldType.PHONE));
        logger.debug("Extracted permanent contact: {}", data.getPermanentContact());

        // Extract Emergency Contact
        String emgContact = extractEmergencyContact(text);
        data.setEmergencyContact(validateAndClean(emgContact, FieldType.PHONE));
        logger.debug("Extracted emergency contact: {}", data.getEmergencyContact());

        // Extract Email
        String email = extractEmail(text);
        data.setEmail(validateAndClean(email, FieldType.EMAIL));
        logger.debug("Extracted email: {}", data.getEmail());
    }

    /**
     * Extract name with multiple pattern attempts
     */
    private String extractName(String text) {
        String[] patterns = {
            "(?i)Name\\s*[:©€]?\\s*([A-Z][A-Z\\s]{2,50}?)(?=\\n|Programme|Register|$)",
            "(?i)Name[^:]*[:©€]\\s*([A-Z][A-Z\\s]+?)(?=\\n|Programme)",
            "(?<=TECHNOLOGY\\s{1,30})([A-Z][A-Z\\s]{2,50}?)(?=\\s*Programme|\\s*Register|\\s*Name)"
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        return result != null && isValidName(result.value) ? result.value : "";
    }

    /**
     * Extract programme with multiple patterns
     */
    private String extractProgramme(String text) {
        String[] patterns = {
            "(?i)Programme\\s*[:©€]?\\s*([A-Z0-9\\.\\(\\)\\s&-]+?)(?=\\n|Register|Valid|$)",
            "(?i)Program\\s*[:©€]?\\s*([A-Z0-9\\.\\(\\)\\s&-]+?)(?=\\n|Register|Valid|$)",
            "(?i)[:©€]\\s*(B\\.?\\s*Tech[^\\n]*)",
            "(?i)(B\\.?\\s*Tech\\s*\\([^)]+\\))"
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        return result != null ? result.value : "";
    }

    /**
     * Extract register number with validation
     */
    private String extractRegisterNumber(String text) {
        String[] patterns = {
            "(?i)Register\\s*No\\.?\\s*[:©€]?\\s*([A-Z]{2}\\d{8,12})",
            "(?i)Reg\\.?\\s*No\\.?\\s*[:©€]?\\s*([A-Z]{2}\\d{8,12})",
            "(?i)Register\\s*Number\\s*[:©€]?\\s*([A-Z]{2}\\d{8,12})",
            "([A-Z]{2}\\d{10})" // Direct pattern for common format
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        return result != null ? result.value : "";
    }

    /**
     * Extract valid from date
     */
    private String extractValidFrom(String text) {
        String[] patterns = {
            "(?i)Valid\\s*From\\s*[:©€]?\\s*([A-Za-z]+[-\\s]?\\d{4})",
            "(?i)From\\s*[:©€]?\\s*([A-Za-z]{3,9}[-\\s]?\\d{4})"
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        return result != null ? result.value : "";
    }

    /**
     * Extract valid to date
     */
    private String extractValidTo(String text) {
        String[] patterns = {
            "(?i)To\\s*[:©€]?\\s*([A-Za-z]+[-\\s]?\\d{4})",
            "(?i)Valid\\s*To\\s*[:©€]?\\s*([A-Za-z]+[-\\s]?\\d{4})"
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        return result != null ? result.value : "";
    }

    /**
     * Extract blood group with normalization
     */
    private String extractBloodGroup(String text) {
        String[] patterns = {
            "(?i)Blood\\s*Group\\s*[:©€]?\\s*([ABO]+\\s*[+\\-]\\s*(?:ve|VE)?)",
            "(?i)Blood\\s*Group\\s*[:©€]?\\s*([ABO]\\s*[+\\-])",
            "([ABO]+\\s*[+\\-]\\s*(?:ve|VE)?)",
            "([0-9ABO]+\\s*[vV+\\-]?[eE€]?)" // Handle misreads
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        if (result != null) {
            return normalizeBloodGroup(result.value);
        }
        return "";
    }

    /**
     * Extract date of birth
     */
    private String extractDateOfBirth(String text) {
        String[] patterns = {
            "(?i)Date\\s*of\\s*Birth\\s*[:©€]?\\s*(\\d{1,2}[-\\/\\\\]\\w{3,9}[-\\/\\\\]\\d{4})",
            "(?i)Birth\\s*[:©€]?\\s*(\\d{1,2}[-\\/\\\\]\\w{3,9}[-\\/\\\\]\\d{4})",
            "(?i)DOB\\s*[:©€]?\\s*(\\d{1,2}[-\\/\\\\]\\w{3,9}[-\\/\\\\]\\d{4})",
            "(\\d{1,2}\\s*[-\\/\\\\]?\\s*[A-Za-z]{3,9}\\s*[-\\/\\\\]?\\s*\\d{4})"
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        return result != null ? result.value : "";
    }

    /**
     * Extract address with improved context handling
     */
    private String extractAddress(String text) {
        // Primary pattern: between "Address" and "Pin"
        Pattern pattern = Pattern.compile(
            "(?i)Address\\s*[:©€]?\\s*([\\s\\S]{10,200}?)(?=Pin\\s*[:©€]?\\s*\\d{6}|Perm\\.?\\s*Cont|$)",
            Pattern.MULTILINE
        );
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String address = cleanAddress(matcher.group(1));
            if (isValidAddress(address)) {
                return address;
            }
        }
        
        // Fallback: extract between DOB and Pin
        pattern = Pattern.compile(
            "(?i)(?:Birth|DOB)[^\\n]*\\n\\s*([\\s\\S]{10,200}?)(?=Pin\\s*[:©€]?\\s*\\d{6})",
            Pattern.MULTILINE
        );
        matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String address = cleanAddress(matcher.group(1));
            if (isValidAddress(address)) {
                return address;
            }
        }
        
        return "";
    }

    /**
     * Extract pin code
     */
    private String extractPin(String text) {
        String[] patterns = {
            "(?i)Pin\\s*(?:Code)?\\s*[:©€+]?\\s*(\\d{6})",
            "(?i)Pincode\\s*[:©€+]?\\s*(\\d{6})",
            "(?i)Pin\\s*[:©€+]?\\s*(\\d{6})",
            "(\\d{6})(?=\\s|$)" // Any 6-digit number as fallback
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        return result != null ? result.value : "";
    }

    /**
     * Extract permanent contact
     */
    private String extractPermanentContact(String text) {
        String[] patterns = {
            "(?i)Perm\\.?\\s*Cont\\.?\\s*No\\.?\\s*[:©€]?\\s*(\\d{10})",
            "(?i)Permanent\\s*Contact\\s*[:©€]?\\s*(\\d{10})",
            "(?i)Perm\\s*[:©€]?\\s*(\\d{10})"
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        return result != null ? result.value : "";
    }

    /**
     * Extract emergency contact
     */
    private String extractEmergencyContact(String text) {
        String[] patterns = {
            "(?i)Emg\\.?\\s*Cont\\.?\\s*No\\.?\\s*[:©€]?\\s*(\\d{10})",
            "(?i)Emergency\\s*Contact\\s*[:©€]?\\s*(\\d{10})",
            "(?i)Emg\\s*[:©€]?\\s*(\\d{10})"
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        return result != null ? result.value : "";
    }

    /**
     * Extract email
     */
    private String extractEmail(String text) {
        String[] patterns = {
            "(?i)E[-\\s]?mail\\s*ID\\s*[:©€]?\\s*([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})",
            "(?i)Email\\s*[:©€]?\\s*([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})",
            "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
        };
        
        FieldResult result = extractFieldWithConfidence(text, patterns);
        return result != null ? result.value : "";
    }

    /**
     * Extract field with confidence scoring from multiple patterns
     */
    private FieldResult extractFieldWithConfidence(String text, String... regexes) {
        FieldResult bestResult = null;
        
        for (String regex : regexes) {
            try {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(text);
                
                if (matcher.find()) {
                    int groupCount = matcher.groupCount();
                    if (groupCount >= 1) {
                        String value = matcher.group(groupCount);
                        int confidence = calculateConfidence(value, regex);
                        
                        if (bestResult == null || confidence > bestResult.confidence) {
                            bestResult = new FieldResult(value, confidence);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing regex pattern: {}", regex, e);
            }
        }
        
        return bestResult;
    }

    /**
     * Calculate confidence score for extracted value
     */
    private int calculateConfidence(String value, String pattern) {
        if (value == null || value.trim().isEmpty()) return 0;
        
        int score = 50; // Base score
        
        // Length bonus (reasonable length is good)
        if (value.length() >= 3 && value.length() <= 100) score += 10;
        
        // Pattern specificity bonus (more specific patterns get higher scores)
        if (pattern.contains("(?i)")) score += 5; // Case insensitive is more flexible
        if (pattern.contains("\\s*")) score += 5; // Handles whitespace variations
        
        // Content quality checks
        if (!value.matches(".*[^a-zA-Z0-9\\s@.\\-+()&:/,].*")) score += 10; // No weird chars
        if (value.trim().equals(value)) score += 5; // No leading/trailing spaces
        
        return score;
    }

    /**
     * Validate and clean extracted fields
     */
    private String validateAndClean(String value, FieldType fieldType) {
        if (value == null || value.trim().isEmpty()) return "";
        
        value = value.trim();
        
        switch (fieldType) {
            case NAME:
                return isValidName(value) ? value : "";
                
            case REGISTER_NUMBER:
                return value.matches("[A-Z]{2}\\d{8,12}") ? value : "";
                
            case PIN_CODE:
                return value.matches("\\d{6}") ? value : "";
                
            case PHONE:
                String cleaned = value.replaceAll("\\D", "");
                return cleaned.matches("\\d{10}") ? cleaned : "";
                
            case EMAIL:
                return value.matches("^[\\w._%+-]+@[\\w.-]+\\.\\w{2,}$") ? value.toLowerCase() : "";
                
            case BLOOD_GROUP:
                return normalizeBloodGroup(value);
                
            case ADDRESS:
                return isValidAddress(value) ? value : "";
                
            case PROGRAMME:
            case DATE:
            case GENERIC:
            default:
                return value;
        }
    }

    /**
     * Validate name format
     */
    private boolean isValidName(String name) {
        if (name == null || name.length() < 3) return false;
        
        // Should contain only letters and spaces
        if (!name.matches("[A-Za-z\\s]+")) return false;
        
        // Should not be all uppercase noise
        if (name.matches("[A-Z\\s]+") && name.split("\\s+").length < 2) return false;
        
        // Should have at least 2 words for full name
        String[] words = name.trim().split("\\s+");
        return words.length >= 2 || name.length() >= 5;
    }

    /**
     * Clean and validate address
     */
    private String cleanAddress(String address) {
        if (address == null) return "";
        
        return address
            .replaceAll("(?i)(Blood Group|Date of Birth|DOB|Birth)[^\\n]*", "")
            .replaceAll("[\\\\]+", ", ")
            .replaceAll("\\s+", " ")
            .replaceAll(",\\s*,", ",")
            .trim();
    }

    /**
     * Validate address
     */
    private boolean isValidAddress(String address) {
        if (address == null || address.length() < 10) return false;
        
        // Should contain letters
        if (!address.matches(".*[A-Za-z]{3,}.*")) return false;
        
        // Should not contain only special characters
        if (address.replaceAll("[^A-Za-z0-9]", "").length() < 5) return false;
        
        return true;
    }

    /**
     * Normalize blood group format
     */
    private String normalizeBloodGroup(String bloodGroup) {
        if (bloodGroup == null || bloodGroup.isEmpty()) return "";
        
        // Common OCR error fixes
        bloodGroup = bloodGroup
            .replace("4VE", "B +ve")
            .replace("4", "A")
            .replace("0", "O")
            .replace("€", "e")
            .replaceAll("\\s+", " ")
            .trim();
        
        // Normalize format to "X +ve" or "X -ve"
        if (bloodGroup.matches("[ABO]+\\s*[+\\-].*")) {
            String type = bloodGroup.replaceAll("[^ABO]", "");
            String rh = bloodGroup.contains("+") ? "+" : "-";
            return type + " " + rh + "ve";
        }
        
        return bloodGroup;
    }

    /**
     * Normalize date format
     */
    private String normalizeDateOfBirth(String dob) {
        if (dob == null || dob.isEmpty()) return "";
        
        // Replace backslashes with hyphens
        dob = dob.replace("\\", "-").replace("/", "-");
        
        // Try to parse and reformat date
        String[] dateFormats = {
            "dd-MMM-yyyy", "dd-MMMM-yyyy", "dd MMM yyyy", 
            "dd MMMM yyyy", "dd-MM-yyyy", "dd/MM/yyyy"
        };
        
        for (String format : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
                sdf.setLenient(false);
                Date date = sdf.parse(dob);
                return new SimpleDateFormat("dd-MMM-yyyy").format(date);
            } catch (ParseException ignored) {
                // Try next format
            }
        }
        
        // Return as-is if parsing fails
        return dob;
    }

    /**
     * Merge front and back card data
     */
    public IDCardData mergeCardData(IDCardData front, IDCardData back) {
        IDCardData merged = new IDCardData();
        
        if (front != null) {
            merged.setName(front.getName());
            merged.setRegisterNumber(front.getRegisterNumber());
            merged.setProgramme(front.getProgramme());
            merged.setValidFrom(front.getValidFrom());
            merged.setValidTo(front.getValidTo());
            merged.setInstitution(front.getInstitution());
            merged.setFaculty(front.getFaculty());
        }
        
        if (back != null) {
            merged.setBloodGroup(back.getBloodGroup());
            merged.setDateOfBirth(back.getDateOfBirth());
            merged.setAddress(back.getAddress());
            merged.setPin(back.getPin());
            merged.setPermanentContact(back.getPermanentContact());
            merged.setEmergencyContact(back.getEmergencyContact());
            merged.setEmail(back.getEmail());
        }
        
        // Combine raw text
        StringBuilder combinedRaw = new StringBuilder();
        if (front != null && front.getRawText() != null) {
            combinedRaw.append("FRONT:\n").append(front.getRawText()).append("\n\n");
        }
        if (back != null && back.getRawText() != null) {
            combinedRaw.append("BACK:\n").append(back.getRawText());
        }
        
        merged.setRawText(combinedRaw.toString());
        merged.setCardType("MERGED");
        
        logger.info("Successfully merged front and back card data");
        
        return merged;
    }
}