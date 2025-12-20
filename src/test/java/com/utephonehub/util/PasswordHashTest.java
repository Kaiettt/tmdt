package com.utephonehub.util;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Test để generate BCrypt hash cho password admin123
 * Chạy test này để lấy hash đúng format từ jbcrypt
 */
public class PasswordHashTest {
    
    @Test
    public void generateAdminPasswordHash() {
        String password = "admin123";
        int rounds = 12;
        
        System.out.println("========================================");
        System.out.println("GENERATE BCrypt HASH FOR ADMIN123");
        System.out.println("========================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Rounds: " + rounds);
        System.out.println("========================================");
        System.out.println();
        
        // Hash password với BCrypt (same as PasswordUtil)
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(rounds));
        
        System.out.println("[OK] Password hashed successfully!");
        System.out.println();
        System.out.println("Hash: " + hashedPassword);
        System.out.println();
        System.out.println("========================================");
        System.out.println("SQL INSERT statement:");
        System.out.println("========================================");
        System.out.println("INSERT INTO users (username, full_name, email, password_hash, phone_number, role, status, created_at, updated_at)");
        System.out.println("VALUES ('admin123', 'Administrator', 'admin123@utephonehub.me', '" + hashedPassword + "', '0901234567', 'admin', 'active', NOW(), NOW());");
        System.out.println("========================================");
        System.out.println();
        
        // Verify password
        boolean isValid = BCrypt.checkpw(password, hashedPassword);
        System.out.println("Verification: " + (isValid ? "PASSED" : "FAILED"));
        System.out.println();
        
        // Note: Hash $2b$ from Python is NOT compatible with jbcrypt
        // jbcrypt only supports $2a$ format
        System.out.println("Note: jbcrypt only supports $2a$ format, not $2b$ from Python bcrypt");
    }
}

