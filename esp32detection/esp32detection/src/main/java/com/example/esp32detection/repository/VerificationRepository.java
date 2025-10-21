package com.example.esp32detection.repository;

import com.example.esp32detection.model.Verification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class VerificationRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public VerificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private final RowMapper<Verification> rowMapper = new RowMapper<Verification>() {
        @Override
        public Verification mapRow(ResultSet rs, int rowNum) throws SQLException {
            Verification verification = new Verification();
            verification.setId(rs.getInt("id"));
            verification.setUserId(rs.getObject("user_id") != null ? rs.getInt("user_id") : null);
            verification.setName(rs.getString("name"));
            verification.setIdNumber(rs.getString("id_number"));
            verification.setFaceMatchScore(rs.getDouble("face_match_score"));
            verification.setVerificationStatus(rs.getString("verification_status"));
            verification.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
            verification.setIdCardData(rs.getString("id_card_data"));
            return verification;
        }
    };
    
    public int save(Verification verification) {
        String sql = "INSERT INTO verification_logs (user_id, name, id_number, face_match_score, verification_status, id_card_data) VALUES (?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql, 
            verification.getUserId(),
            verification.getName(), 
            verification.getIdNumber(), 
            verification.getFaceMatchScore(), 
            verification.getVerificationStatus(),
            verification.getIdCardData());
    }
    
    public List<Verification> findAll() {
        String sql = "SELECT * FROM verification_logs ORDER BY id DESC LIMIT 100";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    public List<Verification> findByStatus(String status) {
        String sql = "SELECT * FROM verification_logs WHERE verification_status = ? ORDER BY id DESC";
        return jdbcTemplate.query(sql, rowMapper, status);
    }
}
