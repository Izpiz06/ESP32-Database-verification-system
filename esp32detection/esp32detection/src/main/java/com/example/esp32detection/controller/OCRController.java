package com.example.esp32detection.controller;

import com.example.esp32detection.model.IDCardData;
import com.example.esp32detection.repository.IDCardRepository;
import com.example.esp32detection.service.IDCardParserService;
import com.example.esp32detection.service.OCRService;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")
public class OCRController {

    @Autowired
    private OCRService ocrService;

    @Autowired
    private IDCardParserService parserService;

    @Autowired
    private IDCardRepository idCardRepository;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerNewUser(
            @RequestParam("front") MultipartFile frontFile,
            @RequestParam("back") MultipartFile backFile) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract text from front side
            String frontText = ocrService.extractIDCardData(frontFile);
            
            // DEBUG: Print raw OCR text
            System.out.println("\n===== FRONT OCR RAW TEXT =====");
            System.out.println(frontText);
            System.out.println("==============================\n");
            
            IDCardData frontData = parserService.parseIDCardText(frontText);
            
            // Extract text from back side
            String backText = ocrService.extractIDCardData(backFile);
            
            // DEBUG: Print raw OCR text
            System.out.println("\n===== BACK OCR RAW TEXT =====");
            System.out.println(backText);
            System.out.println("=============================\n");
            
            IDCardData backData = parserService.parseIDCardText(backText);
            
            // Merge both sides data
            IDCardData mergedData = parserService.mergeCardData(frontData, backData);
            mergedData.setFileName(frontFile.getOriginalFilename() + " & " + backFile.getOriginalFilename());
            mergedData.setVerified(false);
            
            // Check if user already exists by register number
            if (mergedData.getRegisterNumber() != null && 
                idCardRepository.findByRegisterNumber(mergedData.getRegisterNumber()).isPresent()) {
                response.put("status", "error");
                response.put("message", "User with this register number already exists");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
            // Save merged data
            IDCardData savedData = idCardRepository.save(mergedData);
            
            // DEBUG: Print parsed data
            System.out.println("\n===== PARSED DATA =====");
            System.out.println("Name: " + savedData.getName());
            System.out.println("Register Number: " + savedData.getRegisterNumber());
            System.out.println("Programme: " + savedData.getProgramme());
            System.out.println("Email: " + savedData.getEmail());
            System.out.println("Blood Group: " + savedData.getBloodGroup());
            System.out.println("Date of Birth: " + savedData.getDateOfBirth());
            System.out.println("Valid From: " + savedData.getValidFrom());
            System.out.println("Valid To: " + savedData.getValidTo());
            System.out.println("=======================\n");
            
            response.put("status", "success");
            response.put("message", "New user registered successfully");
            response.put("data", savedData);
            response.put("userId", savedData.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (IOException | TesseractException e) {
            response.put("status", "error");
            response.put("message", "Failed to process ID card: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(
            @RequestParam("file") MultipartFile frontFile) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract text from front side
            String frontText = ocrService.extractIDCardData(frontFile);
            
            // DEBUG: Print raw OCR text
            System.out.println("\n===== LOGIN OCR RAW TEXT =====");
            System.out.println(frontText);
            System.out.println("==============================\n");
            
            IDCardData scannedData = parserService.parseIDCardText(frontText);
            
            String registerNumber = scannedData.getRegisterNumber();
            
            System.out.println("Extracted Register Number: " + registerNumber);
            System.out.println("Extracted Name: " + scannedData.getName());
            
            if (registerNumber == null || registerNumber.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Could not extract register number from ID card");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return idCardRepository.findByRegisterNumber(registerNumber)
                    .map(userData -> {
                        boolean nameMatches = scannedData.getName() != null && 
                            userData.getName() != null &&
                            userData.getName().toLowerCase().contains(
                                scannedData.getName().toLowerCase().split(" ")[0].toLowerCase()
                            );
                        
                        if (nameMatches) {
                            response.put("status", "success");
                            response.put("message", "Login successful");
                            response.put("authenticated", true);
                            response.put("user", userData);
                            return ResponseEntity.ok(response);
                        } else {
                            response.put("status", "error");
                            response.put("message", "ID card details do not match records");
                            response.put("authenticated", false);
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                        }
                    })
                    .orElseGet(() -> {
                        response.put("status", "error");
                        response.put("message", "User not found. Please register first.");
                        response.put("authenticated", false);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
            
        } catch (IOException | TesseractException e) {
            response.put("status", "error");
            response.put("message", "Failed to process ID card: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<IDCardData>> getAllUsers() {
        List<IDCardData> users = idCardRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<IDCardData> getUserById(@PathVariable Long id) {
        return idCardRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        return idCardRepository.findById(id)
                .map(user -> {
                    user.setVerified(true);
                    idCardRepository.save(user);
                    response.put("status", "success");
                    response.put("message", "User verified successfully");
                    response.put("user", user);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("status", "error");
                    response.put("message", "User not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        if (idCardRepository.existsById(id)) {
            idCardRepository.deleteById(id);
            response.put("status", "success");
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/users/search/register/{registerNumber}")
    public ResponseEntity<Map<String, Object>> searchByRegisterNumber(@PathVariable String registerNumber) {
        Map<String, Object> response = new HashMap<>();
        
        return idCardRepository.findByRegisterNumber(registerNumber)
                .map(user -> {
                    response.put("status", "success");
                    response.put("user", user);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("status", "not_found");
                    response.put("message", "User not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }
}