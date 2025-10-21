package com.example.esp32detection.repository;

import com.example.esp32detection.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class UserRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private final RowMapper<User> rowMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setIdNumber(rs.getString("id_number"));
            user.setEmail(rs.getString("email"));
            user.setPhone(rs.getString("phone"));
            user.setDepartment(rs.getString("department"));
            user.setFaceEncoding(rs.getString("face_encoding"));
            user.setIdCardImagePath(rs.getString("id_card_image_path"));
            user.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime());
            user.setStatus(rs.getString("status"));
            return user;
        }
    };
    
    public int save(User user) {
        String sql = "INSERT INTO users (name, id_number, email, phone, department, face_encoding, id_card_image_path) VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getIdNumber());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getDepartment());
            ps.setString(6, user.getFaceEncoding());
            ps.setString(7, user.getIdCardImagePath());
            return ps;
        }, keyHolder);
        
        return keyHolder.getKey().intValue();
    }
    
    public User findByIdNumber(String idNumber) {
        String sql = "SELECT * FROM users WHERE id_number = ?";
        List<User> results = jdbcTemplate.query(sql, rowMapper, idNumber);
        return results.isEmpty() ? null : results.get(0);
    }
    
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY registered_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    public List<User> findByStatus(String status) {
        String sql = "SELECT * FROM users WHERE status = ?";
        return jdbcTemplate.query(sql, rowMapper, status);
    }
    
    public int updateStatus(String idNumber, String status) {
        String sql = "UPDATE users SET status = ? WHERE id_number = ?";
        return jdbcTemplate.update(sql, status, idNumber);
    }
}
