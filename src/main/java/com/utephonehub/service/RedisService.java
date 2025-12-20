package com.utephonehub.service;

import com.utephonehub.config.RedisConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

/**
 * Redis Service
 * Handle Redis operations for tokens, OTP, cache
 */
public class RedisService {
    
    private static final Logger logger = LogManager.getLogger(RedisService.class);
    
    // Key prefixes
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String ACCESS_TOKEN_PREFIX = "access_token:";
    private static final String FORGOT_PASSWORD_PREFIX = "forgot_password:";
    private static final String EMAIL_VERIFICATION_PREFIX = "email_verification:";
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    
    // TTL constants
    private static final int REFRESH_TOKEN_TTL = 7 * 24 * 60 * 60; // 7 days in seconds
    private static final int ACCESS_TOKEN_TTL = 24 * 60 * 60; // 24 hours in seconds
    private static final int FORGOT_PASSWORD_TTL = 5 * 60; // 5 minutes in seconds
    private static final int EMAIL_VERIFICATION_TTL = 15 * 60; // 15 minutes in seconds
    private static final int PRODUCT_CACHE_TTL = 60 * 60; // 1 hour in seconds
    private static final int RESEND_OTP_COOLDOWN = 60; // 1 minute in seconds
    
    /**
     * Store refresh token
     */
    public void storeRefreshToken(String email, String refreshToken) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = REFRESH_TOKEN_PREFIX + email;
            jedis.setex(key, REFRESH_TOKEN_TTL, refreshToken);
        } catch (Exception e) {
            logger.error("Error storing refresh token for user: {}", email, e);
            throw new RuntimeException("Failed to store refresh token", e);
        }
    }
    
    /**
     * Get refresh token
     */
    public String getRefreshToken(String email) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = REFRESH_TOKEN_PREFIX + email;
            return jedis.get(key);
        } catch (Exception e) {
            logger.error("Error getting refresh token for user: {}", email, e);
            return null;
        }
    }
    
    /**
     * Delete refresh token (for logout)
     */
    public void deleteRefreshToken(String email) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = REFRESH_TOKEN_PREFIX + email;
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting refresh token for user: {}", email, e);
            throw new RuntimeException("Failed to delete refresh token", e);
        }
    }
    
    /**
     * Blacklist access token (for logout/revoke)
     */
    public void blacklistAccessToken(String token, long expirySeconds) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = ACCESS_TOKEN_PREFIX + "blacklist:" + token;
            jedis.setex(key, (int) expirySeconds, "1");
        } catch (Exception e) {
            logger.error("Error blacklisting access token", e);
        }
    }
    
    /**
     * Check if access token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = ACCESS_TOKEN_PREFIX + "blacklist:" + token;
            return jedis.exists(key);
        } catch (Exception e) {
            logger.error("Error checking token blacklist", e);
            return false;
        }
    }
    
    /**
     * Store forgot password OTP
     */
    public void storeForgotPasswordOTP(String email, String otp) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = FORGOT_PASSWORD_PREFIX + email;
            jedis.setex(key, FORGOT_PASSWORD_TTL, otp);
        } catch (Exception e) {
            logger.error("Error storing forgot password OTP for user: {}", email, e);
            throw new RuntimeException("Failed to store forgot password OTP", e);
        }
    }
    
    /**
     * Verify forgot password OTP (does NOT delete OTP - call deleteForgotPasswordOTP after reset)
     */
    public boolean verifyForgotPasswordOTP(String email, String otp) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = FORGOT_PASSWORD_PREFIX + email;
            String storedOtp = jedis.get(key);
            
            if (storedOtp != null && storedOtp.equals(otp)) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error verifying forgot password OTP for user: {}", email, e);
            return false;
        }
    }
    
    /**
     * Delete forgot password OTP (after successful password reset)
     */
    public void deleteForgotPasswordOTP(String email) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = FORGOT_PASSWORD_PREFIX + email;
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting forgot password OTP for user: {}", email, e);
        }
    }
    
    /**
     * Check if resend OTP is allowed (rate limiting - 1 minute cooldown)
     */
    public boolean canResendOTP(String email) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String cooldownKey = FORGOT_PASSWORD_PREFIX + "cooldown:" + email;
            return !jedis.exists(cooldownKey);
        } catch (Exception e) {
            logger.error("Error checking resend cooldown for user: {}", email, e);
            return true; // Allow resend on error
        }
    }
    
    /**
     * Set resend OTP cooldown (1 minute)
     */
    public void setResendCooldown(String email) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String cooldownKey = FORGOT_PASSWORD_PREFIX + "cooldown:" + email;
            jedis.setex(cooldownKey, RESEND_OTP_COOLDOWN, "1");
        } catch (Exception e) {
            logger.error("Error setting resend cooldown for user: {}", email, e);
        }
    }
    
    /**
     * Track OTP verification attempts (anti-spam)
     * Returns number of failed attempts
     */
    public int incrementOTPAttempts(String email) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String attemptKey = FORGOT_PASSWORD_PREFIX + "attempts:" + email;
            Long attempts = jedis.incr(attemptKey);
            
            // Set expiry on first attempt (5 minutes - same as OTP TTL)
            if (attempts == 1) {
                jedis.expire(attemptKey, FORGOT_PASSWORD_TTL);
            }
            
            return attempts.intValue();
        } catch (Exception e) {
            logger.error("Error tracking OTP attempts for user: {}", email, e);
            return 0;
        }
    }
    
    /**
     * Check if OTP verification is blocked due to too many failed attempts
     */
    public boolean isOTPVerificationBlocked(String email) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String attemptKey = FORGOT_PASSWORD_PREFIX + "attempts:" + email;
            String attempts = jedis.get(attemptKey);
            
            if (attempts != null) {
                int attemptCount = Integer.parseInt(attempts);
                return attemptCount >= 5; // Block after 5 failed attempts
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking OTP verification block for user: {}", email, e);
            return false; // Allow on error
        }
    }
    
    /**
     * Clear OTP attempts counter (after successful verification)
     */
    public void clearOTPAttempts(String email) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String attemptKey = FORGOT_PASSWORD_PREFIX + "attempts:" + email;
            jedis.del(attemptKey);
        } catch (Exception e) {
            logger.error("Error clearing OTP attempts for user: {}", email, e);
        }
    }
    
    /**
     * Store email verification code
     */
    public void storeEmailVerificationCode(String email, String code) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = EMAIL_VERIFICATION_PREFIX + email;
            jedis.setex(key, EMAIL_VERIFICATION_TTL, code);
        } catch (Exception e) {
            logger.error("Error storing email verification code for user: {}", email, e);
            throw new RuntimeException("Failed to store email verification code", e);
        }
    }
    
    /**
     * Verify email verification code
     */
    public boolean verifyEmailCode(String email, String code) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = EMAIL_VERIFICATION_PREFIX + email;
            String storedCode = jedis.get(key);
            
            if (storedCode != null && storedCode.equals(code)) {
                jedis.del(key);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error verifying email code for user: {}", email, e);
            return false;
        }
    }
    
    /**
     * Cache product data
     */
    public void cacheProduct(String productId, String productData) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = PRODUCT_CACHE_PREFIX + productId;
            jedis.setex(key, PRODUCT_CACHE_TTL, productData);
        } catch (Exception e) {
            logger.error("Error caching product: {}", productId, e);
        }
    }
    
    /**
     * Get cached product
     */
    public String getCachedProduct(String productId) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = PRODUCT_CACHE_PREFIX + productId;
            return jedis.get(key);
        } catch (Exception e) {
            logger.error("Error getting cached product: {}", productId, e);
            return null;
        }
    }
    
    /**
     * Invalidate product cache
     */
    public void invalidateProductCache(String productId) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            String key = PRODUCT_CACHE_PREFIX + productId;
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error invalidating product cache: {}", productId, e);
        }
    }
    
    /**
     * Set generic key-value with TTL
     */
    public void set(String key, String value, int ttlSeconds) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            jedis.setex(key, ttlSeconds, value);
        } catch (Exception e) {
            logger.error("Error setting key: {}", key, e);
            throw new RuntimeException("Failed to set key in Redis", e);
        }
    }
    
    /**
     * Get value by key
     */
    public String get(String key) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            return jedis.get(key);
        } catch (Exception e) {
            logger.error("Error getting key: {}", key, e);
            return null;
        }
    }
    
    /**
     * Delete key
     */
    public void delete(String key) {
        try (Jedis jedis = RedisConfig.getJedis()) {
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting key: {}", key, e);
        }
    }
}
