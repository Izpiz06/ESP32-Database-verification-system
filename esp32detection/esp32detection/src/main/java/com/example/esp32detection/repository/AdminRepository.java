package com.example.esp32detection.repository;

import com.example.esp32detection.model.Admin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AdminRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private final RowMapper<Admin> rowMapper = new RowMapper<Admin>() {
        @Override
        public Admin mapRow(ResultSet rs, int rowNum) throws SQLException {
            Admin admin = new Admin();
            admin.setId(rs.getInt("id"));
            admin.setUsername(rs.getString("username"));
            admin.setPassword(rs.getString("password"));
            admin.setEmail(rs.getString("email"));
            admin.setFullName(rs.getString("full_name"));
            admin.setRole(rs.getString("role"));
            admin.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            if (rs.getTimestamp("last_login") != null) {
                admin.setLastLogin(rs.getTimestamp("last_login").toLocalDateTime());
            }
            return admin;
        }
    };
    
    public int save(Admin admin) {
        String sql = "INSERT INTO admin_accounts (username, password, email, full_name) VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql, admin.getUsername(), admin.getPassword(), 
                                    admin.getEmail(), admin.getFullName());
    }
    
    public Admin findByUsername(String username) {
        String sql = "SELECT * FROM admin_accounts WHERE username = ?";
        List<Admin> results = jdbcTemplate.query(sql, rowMapper, username);
        return results.isEmpty() ? null : results.get(0);
    }
    
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM admin_accounts WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }
    
    public void updateLastLogin(String username) {
        String sql = "UPDATE admin_accounts SET last_login = ? WHERE username = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), username);
    }
}
