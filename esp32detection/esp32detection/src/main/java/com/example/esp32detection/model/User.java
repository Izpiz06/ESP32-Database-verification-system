package com.example.esp32detection.model;

import java.time.LocalDateTime;

public class User {
    private Integer id;
    private String name;
    private String idNumber;
    private String email;
    private String phone;
    private String department;
    private String faceEncoding;
    private String idCardImagePath;
    private LocalDateTime registeredAt;
    private String status;
    
    public User() {}
    
    public User(String name, String idNumber) {
        this.name = name;
        this.idNumber = idNumber;
        this.status = "ACTIVE";
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getFaceEncoding() { return faceEncoding; }
    public void setFaceEncoding(String faceEncoding) { this.faceEncoding = faceEncoding; }
    
    public String getIdCardImagePath() { return idCardImagePath; }
    public void setIdCardImagePath(String idCardImagePath) { this.idCardImagePath = idCardImagePath; }
    
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}