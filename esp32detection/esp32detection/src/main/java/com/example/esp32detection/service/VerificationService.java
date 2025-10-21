package com.example.esp32detection.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.esp32detection.model.User;
import com.example.esp32detection.model.Verification;
import com.example.esp32detection.repository.VerificationRepository;

@Service
public class VerificationService {
    
    private final VerificationRepository verificationRepository;
    private final UserService userService;
    
    public VerificationService(VerificationRepository verificationRepository, UserService userService) {
        this.verificationRepository = verificationRepository;
        this.userService = userService;
    }
    
    public Verification saveVerification(String name, String idNumber, Double matchScore, String status, String idCardData) {
        Verification verification = new Verification(name, idNumber, matchScore, status);
        verification.setIdCardData(idCardData);
        
        User user = userService.findByIdNumber(idNumber);
        if (user != null) {
            verification.setUserId(user.getId());
        }
        
        verificationRepository.save(verification);
        return verification;
    }
    
    public List<Verification> getAllVerifications() {
        return verificationRepository.findAll();
    }
    
    public List<Verification> getGrantedAccess() {
        return verificationRepository.findByStatus("GRANTED");
    }
    
    public List<Verification> getDeniedAccess() {
        return verificationRepository.findByStatus("DENIED");
    }
}
