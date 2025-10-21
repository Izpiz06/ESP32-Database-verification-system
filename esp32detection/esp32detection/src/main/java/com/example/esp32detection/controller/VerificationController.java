package com.example.esp32detection.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.esp32detection.model.Admin;
import com.example.esp32detection.model.User;
import com.example.esp32detection.model.Verification;
import com.example.esp32detection.service.AdminService;
import com.example.esp32detection.service.UserService;
import com.example.esp32detection.service.VerificationService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VerificationController {
    
    private final VerificationService verificationService;
    private final UserService userService;
    private final AdminService adminService;
    
    // Single constructor with all services
    public VerificationController(VerificationService verificationService, 
                                  UserService userService, 
                                  AdminService adminService) {
        this.verificationService = verificationService;
        this.userService = userService;
        this.adminService = adminService;
    }
    
    // ========== AUTHENTICATION ENDPOINTS ==========
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Admin admin = adminService.login(username, password);
            
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("username", admin.getUsername());
            response.put("fullName", admin.getFullName());
            
            System.out.println("✅ Login successful: " + username);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            
            System.out.println("❌ Login failed: " + username);
            
            return ResponseEntity.status(401).body(response);
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        String password = data.get("password");
        String email = data.get("email");
        String fullName = data.get("fullName");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Admin admin = adminService.register(username, password, email, fullName);
            
            response.put("success", true);
            response.put("message", "Account created successfully");
            response.put("username", admin.getUsername());
            
            System.out.println("✅ New account created: " + username);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // ========== USER REGISTRATION ENDPOINTS ==========
    
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        String idNumber = (String) payload.get("id_number");
        String email = (String) payload.getOrDefault("email", "");
        String phone = (String) payload.getOrDefault("phone", "");
        String department = (String) payload.getOrDefault("department", "");
        String faceEncoding = (String) payload.getOrDefault("face_encoding", "");
        String imagePath = (String) payload.getOrDefault("image_path", "");
        
        Map<String, Object> response = new HashMap<>();
        
        if (userService.isUserRegistered(idNumber)) {
            response.put("success", false);
            response.put("message", "User already registered");
            return ResponseEntity.badRequest().body(response);
        }
        
        User user = userService.registerUser(name, idNumber, email, phone, department, faceEncoding, imagePath);
        
        response.put("success", true);
        response.put("message", "User registered successfully");
        response.put("data", user);
        
        System.out.println("✅ New User Registered: " + name + " (" + idNumber + ")");
        
        return ResponseEntity.ok(response);
    }
    
    // ========== VERIFICATION ENDPOINTS ==========
    
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyUser(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        String idNumber = (String) payload.get("id_number");
        Double matchScore = ((Number) payload.get("match_score")).doubleValue();
        String idCardData = payload.get("id_card_data") != null ? payload.get("id_card_data").toString() : "{}";
        
        Map<String, Object> response = new HashMap<>();
        
        User user = userService.findByIdNumber(idNumber);
        if (user == null) {
            Verification verification = verificationService.saveVerification(name, idNumber, matchScore, "NOT_REGISTERED", idCardData);
            response.put("success", false);
            response.put("status", "NOT_REGISTERED");
            response.put("message", "User not found. Please register first.");
            response.put("data", verification);
            System.out.println("❌ User not registered: " + idNumber);
            return ResponseEntity.ok(response);
        }
        
        if ("BLOCKED".equals(user.getStatus())) {
            Verification verification = verificationService.saveVerification(name, idNumber, matchScore, "DENIED", idCardData);
            response.put("success", false);
            response.put("status", "DENIED");
            response.put("message", "Access denied. User is blocked.");
            response.put("data", verification);
            System.out.println("🚫 Blocked user attempted access: " + idNumber);
            return ResponseEntity.ok(response);
        }
        
        String status = matchScore >= 0.85 ? "GRANTED" : "DENIED";
        Verification verification = verificationService.saveVerification(name, idNumber, matchScore, status, idCardData);
        
        response.put("success", status.equals("GRANTED"));
        response.put("status", status);
        response.put("message", status.equals("GRANTED") ? "Access granted" : "Face match failed");
        response.put("data", verification);
        response.put("user", user);
        
        System.out.println((status.equals("GRANTED") ? "✅" : "❌") + " Verification: " + name + " - " + status + " (Score: " + matchScore + ")");
        
        return ResponseEntity.ok(response);
    }
    
    // ========== USER MANAGEMENT ENDPOINTS ==========
    
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @GetMapping("/users/{idNumber}")
    public ResponseEntity<Map<String, Object>> checkUser(@PathVariable String idNumber) {
        User user = userService.findByIdNumber(idNumber);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", user != null);
        response.put("user", user);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/users/{idNumber}/block")
    public ResponseEntity<Map<String, String>> blockUser(@PathVariable String idNumber) {
        userService.blockUser(idNumber);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User blocked");
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/users/{idNumber}/activate")
    public ResponseEntity<Map<String, String>> activateUser(@PathVariable String idNumber) {
        userService.activateUser(idNumber);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User activated");
        return ResponseEntity.ok(response);
    }
    
    // ========== VERIFICATION LOGS ENDPOINTS ==========
    
    @GetMapping("/verifications")
    public ResponseEntity<List<Verification>> getAllVerifications() {
        return ResponseEntity.ok(verificationService.getAllVerifications());
    }
    
    @GetMapping("/verifications/granted")
    public ResponseEntity<List<Verification>> getGrantedAccess() {
        return ResponseEntity.ok(verificationService.getGrantedAccess());
    }
    
    @GetMapping("/verifications/denied")
    public ResponseEntity<List<Verification>> getDeniedAccess() {
        return ResponseEntity.ok(verificationService.getDeniedAccess());
    }
}
