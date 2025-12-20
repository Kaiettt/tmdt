package com.utephonehub.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Utility
 * Xử lý tạo và verify JWT tokens
 */
public class JwtUtil {
    
    private static final Logger logger = LogManager.getLogger(JwtUtil.class);
    
    // JWT Configuration
    private static final String SECRET_KEY = "ute-phone-hub-super-secret-key-for-jwt-token-generation-2025-secure";
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours
    private static final long REFRESH_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000; // 7 days
    
    private final SecretKey key;
    
    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
    
    /**
     * Generate JWT token with userId and role
     * @param userId User ID
     * @param email User email
     * @param role User role (customer/admin)
     * @return JWT token
     */
    public String generateToken(Long userId, String email, String role) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
            
            String token = Jwts.builder()
                    .setSubject(email)
                    .claim("userId", userId)
                    .claim("role", role)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(key, SignatureAlgorithm.HS512)
                    .compact();
            
            logger.debug("JWT token generated for user: {} (userId: {}, role: {})", email, userId, role);
            return token;
        } catch (Exception e) {
            logger.error("Error generating JWT token for user: {}", email, e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }
    
    /**
     * Generate JWT token (backward compatibility - email only)
     * @param email User email
     * @return JWT token
     * @deprecated Use generateToken(Long userId, String email, String role) instead
     */
    @Deprecated
    public String generateToken(String email) {
        return generateToken(0L, email, "customer");
    }
    
    /**
     * Generate refresh token with userId and role
     * @param userId User ID
     * @param email User email
     * @param role User role
     * @return Refresh token
     */
    public String generateRefreshToken(Long userId, String email, String role) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + REFRESH_EXPIRATION_TIME);
            
            String token = Jwts.builder()
                    .setSubject(email)
                    .claim("userId", userId)
                    .claim("role", role)
                    .claim("type", "refresh")
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(key, SignatureAlgorithm.HS512)
                    .compact();
            
            logger.debug("Refresh token generated for user: {} (userId: {}, role: {})", email, userId, role);
            return token;
        } catch (Exception e) {
            logger.error("Error generating refresh token for user: {}", email, e);
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }
    
    /**
     * Generate refresh token (backward compatibility)
     * @param email User email
     * @return Refresh token
     * @deprecated Use generateRefreshToken(Long userId, String email, String role) instead
     */
    @Deprecated
    public String generateRefreshToken(String email) {
        return generateRefreshToken(0L, email, "customer");
    }
    
    /**
     * Validate JWT token
     * @param token JWT token
     * @return true nếu token hợp lệ
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get email từ JWT token
     * @param token JWT token
     * @return Email
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Error extracting email from token", e);
            throw new RuntimeException("Failed to extract email from token", e);
        }
    }
    
    /**
     * Get userId từ JWT token
     * @param token JWT token
     * @return User ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token)
                    .getBody();
            
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            } else if (userId instanceof Long) {
                return (Long) userId;
            }
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Error extracting userId from token", e);
            throw new RuntimeException("Failed to extract userId from token", e);
        }
    }
    
    /**
     * Get role từ JWT token
     * @param token JWT token
     * @return User role (customer/admin)
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.get("role", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Error extracting role from token", e);
            throw new RuntimeException("Failed to extract role from token", e);
        }
    }
    
    /**
     * Check if user is admin from token
     * @param token JWT token
     * @return true if user is admin
     */
    public boolean isAdmin(String token) {
        try {
            String role = getRoleFromToken(token);
            return "admin".equalsIgnoreCase(role);
        } catch (Exception e) {
            logger.error("Error checking admin role from token", e);
            return false;
        }
    }
    
    /**
     * Get expiration date từ JWT token
     * @param token JWT token
     * @return Expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration();
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Error extracting expiration date from token", e);
            throw new RuntimeException("Failed to extract expiration date from token", e);
        }
    }
    
    /**
     * Kiểm tra token có hết hạn không
     * @param token JWT token
     * @return true nếu token đã hết hạn
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            logger.error("Error checking token expiration", e);
            return true;
        }
    }
    
    /**
     * Extract token từ Authorization header
     * @param authHeader Authorization header value
     * @return JWT token
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    /**
     * Get token type (access or refresh)
     * @param token JWT token
     * @return Token type
     */
    public String getTokenType(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token)
                    .getBody();
            
            String type = claims.get("type", String.class);
            return type != null ? type : "access";
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Error extracting token type", e);
            return "access";
        }
    }
}
