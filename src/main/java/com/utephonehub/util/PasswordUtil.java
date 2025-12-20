package com.utephonehub.util;

import org.mindrot.jbcrypt.BCrypt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Password Utility
 * Xử lý hash và verify password
 */
public class PasswordUtil {
    
    private static final Logger logger = LogManager.getLogger(PasswordUtil.class);
    private static final int BCRYPT_ROUNDS = 12;
    
    /**
     * Hash password với BCrypt
     * @param plainPassword Mật khẩu gốc
     * @return Mật khẩu đã hash
     */
    public static String hashPassword(String plainPassword) {
        try {
            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
            logger.debug("Password hashed successfully");
            return hashedPassword;
        } catch (Exception e) {
            logger.error("Error hashing password", e);
            throw new RuntimeException("Failed to hash password", e);
        }
    }
    
    /**
     * Verify password
     * @param plainPassword Mật khẩu gốc
     * @param hashedPassword Mật khẩu đã hash
     * @return true nếu mật khẩu đúng
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            boolean isValid = BCrypt.checkpw(plainPassword, hashedPassword);
            logger.debug("Password verification result: {}", isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("Error verifying password", e);
            return false;
        }
    }
    
    /**
     * Kiểm tra mật khẩu có hợp lệ không
     * @param password Mật khẩu cần kiểm tra
     * @return true nếu mật khẩu hợp lệ
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        // Minimum 8 characters, at least one uppercase, lowercase, and number
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
        return password.matches(pattern);
    }
    
    /**
     * Generate random password
     * @param length Độ dài mật khẩu
     * @return Mật khẩu ngẫu nhiên
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        
        return password.toString();
    }
    
    /**
     * Generate random password với độ dài mặc định
     * @return Mật khẩu ngẫu nhiên 12 ký tự
     */
    public static String generateRandomPassword() {
        return generateRandomPassword(12);
    }
}
