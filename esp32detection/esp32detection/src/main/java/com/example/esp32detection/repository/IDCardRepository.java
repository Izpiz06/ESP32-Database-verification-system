package com.example.esp32detection.repository;

import com.example.esp32detection.model.IDCardData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IDCardRepository extends JpaRepository<IDCardData, Long> {
    
    Optional<IDCardData> findByRegisterNumber(String registerNumber);
    
    Optional<IDCardData> findByEmail(String email);
    
    List<IDCardData> findByVerified(Boolean verified);
    
    List<IDCardData> findByNameContainingIgnoreCase(String name);
    
    List<IDCardData> findByCardType(String cardType);
    
    List<IDCardData> findByProgrammeContainingIgnoreCase(String programme);
}
