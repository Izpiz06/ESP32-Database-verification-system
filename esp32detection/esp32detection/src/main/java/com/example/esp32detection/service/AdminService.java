package com.example.esp32detection.service;

import org.springframework.stereotype.Service;

import com.example.esp32detection.model.Admin;
import com.example.esp32detection.repository.AdminRepository;

@Service
public class AdminService {
    
    private final AdminRepository repository;
    
    public AdminService(AdminRepository repository) {
        this.repository = repository;
    }
    
    public Admin register(String username, String password, String email, String fullName) {
        if (repository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        
        Admin admin = new Admin(username, password);
        admin.setEmail(email);
        admin.setFullName(fullName);
        
        repository.save(admin);
        return admin;
    }
    
    public Admin login(String username, String password) {
        Admin admin = repository.findByUsername(username);
        
        if (admin == null) {
            throw new RuntimeException("User not found");
        }
        
        if (!admin.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }
        
        repository.updateLastLogin(username);
        return admin;
    }
    
    public boolean isValidCredentials(String username, String password) {
        Admin admin = repository.findByUsername(username);
        return admin != null && admin.getPassword().equals(password);
    }
}
