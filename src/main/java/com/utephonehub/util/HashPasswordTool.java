package com.utephonehub.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Tool để hash password với BCrypt
 * Sử dụng để generate hash cho SQL script
 * 
 * Usage: mvn compile exec:java -Dexec.mainClass="com.utephonehub.util.HashPasswordTool" -Dexec.args="admin123"
 */
public class HashPasswordTool {
    private static final int BCRYPT_ROUNDS = 12;
    
    public static void main(String[] args) {
        String password = "admin123";
        
        if (args.length > 0) {
            password = args[0];
        }
        
        System.out.println("========================================");
        System.out.println("PASSWORD HASHING TOOL");
        System.out.println("========================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Rounds: " + BCRYPT_ROUNDS);
        System.out.println("========================================");
        System.out.println();
        
        try {
            // Hash password với BCrypt (same as PasswordUtil)
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
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
            
            // Verify password
            boolean isValid = BCrypt.checkpw(password, hashedPassword);
            System.out.println();
            System.out.println("Verification: " + (isValid ? "PASSED" : "FAILED"));
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

