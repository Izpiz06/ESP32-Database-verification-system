package com.example.esp32detection.service;

import com.example.esp32detection.model.User;
import com.example.esp32detection.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    
    private final UserRepository repository;
    
    public UserService(UserRepository repository) {
        this.repository = repository;
    }
    
    public User registerUser(String name, String idNumber, String email, String phone, String department, String faceEncoding, String imagePath) {
        User user = new User(name, idNumber);
        user.setEmail(email);
        user.setPhone(phone);
        user.setDepartment(department);
        user.setFaceEncoding(faceEncoding);
        user.setIdCardImagePath(imagePath);
        
        repository.save(user);
        return user;
    }
    
    public User findByIdNumber(String idNumber) {
        return repository.findByIdNumber(idNumber);
    }
    
    public boolean isUserRegistered(String idNumber) {
        return repository.findByIdNumber(idNumber) != null;
    }
    
    public List<User> getAllUsers() {
        return repository.findAll();
    }
    
    public void blockUser(String idNumber) {
        repository.updateStatus(idNumber, "BLOCKED");
    }
    
    public void activateUser(String idNumber) {
        repository.updateStatus(idNumber, "ACTIVE");
    }
}
