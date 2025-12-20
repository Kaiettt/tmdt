package com.utephonehub.util;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Create Admin User Utility
 * Script to create admin account with proper BCrypt password hashing
 * 
 * Usage: Run this as a standalone Java program with database credentials
 */
public class CreateAdminUser {
    
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/utephonehub_dev";
    private static final String DB_USER = "utephonehub_user";
    private static final String DB_PASSWORD = "utephonehub_password";
    
    public static void main(String[] args) {
        String username = "admin123";
        String password = "admin123";
        String fullName = "Administrator";
        String email = "admin123@utephonehub.com";
        String phoneNumber = "0900000000";
        
        // Allow command line arguments
        if (args.length >= 2) {
            username = args[0];
            password = args[1];
        }
        if (args.length >= 3) {
            email = args[2];
        }
        if (args.length >= 4) {
            fullName = args[3];
        }
        
        System.out.println("========================================");
        System.out.println("CREATE ADMIN USER UTILITY");
        System.out.println("========================================");
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
        System.out.println("Email: " + email);
        System.out.println("Full Name: " + fullName);
        System.out.println("========================================\n");
        
        try {
            // Hash password using BCrypt (same as PasswordUtil)
            String hashedPassword = hashPassword(password);
            System.out.println("✅ Password hashed successfully");
            System.out.println("Hash: " + hashedPassword);
            System.out.println();
            
            // Connect to database
            System.out.println("Connecting to database...");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("✅ Connected to database");
            System.out.println();
            
            // Check if user exists
            String checkSql = "SELECT id, username, email FROM users WHERE username = ? OR email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            checkStmt.setString(2, email);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("⚠️  User already exists!");
                System.out.println("ID: " + rs.getLong("id"));
                System.out.println("Username: " + rs.getString("username"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println();
                
                // Update password
                System.out.println("Updating password...");
                String updateSql = "UPDATE users SET password_hash = ?, updated_at = NOW() WHERE username = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, hashedPassword);
                updateStmt.setString(2, username);
                int updated = updateStmt.executeUpdate();
                
                if (updated > 0) {
                    System.out.println("✅ Password updated successfully!");
                }
                updateStmt.close();
            } else {
                // Insert new user
                System.out.println("Creating new admin user...");
                String insertSql = "INSERT INTO users (username, full_name, email, phone_number, password_hash, role, status, created_at, updated_at) " +
                                   "VALUES (?, ?, ?, ?, ?, 'admin', 'active', NOW(), NOW())";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setString(1, username);
                insertStmt.setString(2, fullName);
                insertStmt.setString(3, email);
                insertStmt.setString(4, phoneNumber);
                insertStmt.setString(5, hashedPassword);
                
                int inserted = insertStmt.executeUpdate();
                
                if (inserted > 0) {
                    System.out.println("✅ Admin user created successfully!");
                }
                insertStmt.close();
            }
            
            // Verify the user
            System.out.println();
            System.out.println("Verifying user...");
            String verifySql = "SELECT id, username, full_name, email, role, status FROM users WHERE username = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            verifyStmt.setString(1, username);
            ResultSet verifyRs = verifyStmt.executeQuery();
            
            if (verifyRs.next()) {
                System.out.println("========================================");
                System.out.println("USER DETAILS:");
                System.out.println("========================================");
                System.out.println("ID: " + verifyRs.getLong("id"));
                System.out.println("Username: " + verifyRs.getString("username"));
                System.out.println("Full Name: " + verifyRs.getString("full_name"));
                System.out.println("Email: " + verifyRs.getString("email"));
                System.out.println("Role: " + verifyRs.getString("role"));
                System.out.println("Status: " + verifyRs.getString("status"));
                System.out.println("========================================");
            }
            
            verifyStmt.close();
            checkStmt.close();
            conn.close();
            
            System.out.println();
            System.out.println("✅ COMPLETED SUCCESSFULLY!");
            System.out.println();
            System.out.println("You can now login with:");
            System.out.println("  Username: " + username);
            System.out.println("  Password: " + password);
            
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Hash password with BCrypt (same implementation as PasswordUtil)
     * @param plainPassword Plain text password
     * @return BCrypt hashed password
     */
    private static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
    
    /**
     * Verify password (for testing)
     * @param plainPassword Plain text password
     * @param hashedPassword BCrypt hashed password
     * @return true if password matches
     */
    private static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
