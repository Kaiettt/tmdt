package com.utephonehub.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis Configuration
 * Singleton pattern for Redis connection management
 */
public class RedisConfig {
    
    private static final Logger logger = LogManager.getLogger(RedisConfig.class);
    private static JedisPool jedisPool;
    
    static {
        initializeJedisPool();
    }
    
    /**
     * Initialize Jedis Pool
     */
    private static void initializeJedisPool() {
        try {
            // Check if REDIS_URL is provided (Railway/Heroku/Redis Cloud style)
            String redisUrl = getEnvOrProperty("REDIS_URL", null);
            
            // Configure pool
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(20);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(5);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMaxWaitMillis(3000);
            
            // Create pool based on configuration
            if (redisUrl != null && !redisUrl.isEmpty()) {
                // Use REDIS_URL (Railway/Heroku/Redis Cloud style: redis://[username:password@]host:port)
                logger.info("Initializing Redis from REDIS_URL: {}", maskPassword(redisUrl));
                jedisPool = new JedisPool(poolConfig, redisUrl);
            } else {
                // Use individual configuration (legacy mode)
                String redisHost = getEnvOrProperty("REDIS_HOST", "localhost");
                int redisPort = Integer.parseInt(getEnvOrProperty("REDIS_PORT", "6379"));
                String redisPassword = getEnvOrProperty("REDIS_PASSWORD", null);
                String redisUsername = getEnvOrProperty("REDIS_USERNAME", null);
                
                logger.info("Initializing Redis with host: {}, port: {}, username: {}", 
                    redisHost, redisPort, redisUsername != null ? redisUsername : "none");
                
                // Redis Cloud uses ACL with username
                if (redisUsername != null && !redisUsername.isEmpty() && 
                    redisPassword != null && !redisPassword.isEmpty()) {
                    jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 3000, redisUsername, redisPassword);
                } else if (redisPassword != null && !redisPassword.isEmpty()) {
                    jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 3000, redisPassword);
                } else {
                    jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 3000);
                }
            }
            
            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                String pong = jedis.ping();
                logger.info("✓ Redis connection established successfully! PING response: {}", pong);
            }
            
        } catch (Exception e) {
            logger.error("✗ Failed to initialize Redis connection", e);
            logger.error("REDIS_URL: {}", maskPassword(getEnvOrProperty("REDIS_URL", "not set")));
            logger.error("REDIS_HOST: {}", getEnvOrProperty("REDIS_HOST", "not set"));
            logger.error("REDIS_PORT: {}", getEnvOrProperty("REDIS_PORT", "not set"));
            logger.error("REDIS_USERNAME: {}", getEnvOrProperty("REDIS_USERNAME", "not set"));
            throw new RuntimeException("Redis initialization failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mask password in URL/string for logging
     */
    private static String maskPassword(String input) {
        if (input == null || !input.contains("@")) {
            return input;
        }
        return input.replaceAll(":[^:@]+@", ":****@");
    }
    
    /**
     * Get Jedis resource from pool
     * Remember to close the resource after use
     */
    public static Jedis getJedis() {
        if (jedisPool == null) {
            throw new IllegalStateException("Jedis pool is not initialized");
        }
        return jedisPool.getResource();
    }
    
    /**
     * Close Jedis pool
     */
    public static void closePool() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("Redis pool closed");
        }
    }
    
    /**
     * Get environment variable or system property
     */
    private static String getEnvOrProperty(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null) {
            value = System.getProperty(key);
        }
        return value != null ? value : defaultValue;
    }
}
