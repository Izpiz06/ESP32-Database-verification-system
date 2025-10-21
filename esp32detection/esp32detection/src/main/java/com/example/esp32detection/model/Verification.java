package com.example.esp32detection.model;

import java.time.LocalDateTime;

public class Verification {
    private Integer id;
    private Integer userId;
    private String name;
    private String idNumber;
    private Double faceMatchScore;
    private String verificationStatus;
    private LocalDateTime timestamp;
    private String idCardData;
    
    public Verification() {}
    
    public Verification(String name, String idNumber, Double faceMatchScore, String verificationStatus) {
        this.name = name;
        this.idNumber = idNumber;
        this.faceMatchScore = faceMatchScore;
        this.verificationStatus = verificationStatus;
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    
    public Double getFaceMatchScore() { return faceMatchScore; }
    public void setFaceMatchScore(Double faceMatchScore) { this.faceMatchScore = faceMatchScore; }
    
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getIdCardData() { return idCardData; }
    public void setIdCardData(String idCardData) { this.idCardData = idCardData; }
}
