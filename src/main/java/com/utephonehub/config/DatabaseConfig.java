package com.utephonehub.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Database Configuration Manager
 * Quản lý kết nối database và EntityManager
 */
public class DatabaseConfig {
    
    private static final Logger logger = LogManager.getLogger(DatabaseConfig.class);
    private static final String PERSISTENCE_UNIT_NAME = "ute-phone-hub-pu";
    
    private static EntityManagerFactory entityManagerFactory;
    private static final ThreadLocal<EntityManager> entityManagerThreadLocal = new ThreadLocal<>();
    
    static {
        initializeEntityManagerFactory();
    }
    
    /**
     * Khởi tạo EntityManagerFactory
     */
    private static void initializeEntityManagerFactory() {
        try {
            logger.info("Initializing EntityManagerFactory...");
            
            // Load .env file if exists
            loadEnvFile();
            
            // Tạo properties map với environment variables override
            Map<String, String> properties = new HashMap<>();
            
            // Build JDBC URL from environment variables or system properties
            String dbHost = getEnvOrProperty("DB_HOST");
            String dbPort = getEnvOrProperty("DB_PORT");
            String dbName = getEnvOrProperty("DB_NAME");
            String dbUser = getEnvOrProperty("DB_USER");
            String dbPassword = getEnvOrProperty("DB_PASSWORD");

            // Apply local defaults when environment variables are missing
            if (dbHost == null || dbHost.isBlank()) {
                dbHost = "localhost";
                logger.info("DB_HOST not provided - falling back to localhost");
            }
            if (dbPort == null || dbPort.isBlank()) {
                dbPort = "5432";
                logger.info("DB_PORT not provided - falling back to 5432");
            }
            if (dbName == null || dbName.isBlank()) {
                dbName = "utephonehub_dev";
                logger.info("DB_NAME not provided - falling back to utephonehub_dev");
            }
            if (dbUser == null || dbUser.isBlank()) {
                dbUser = "utephonehub_user";
                logger.info("DB_USER not provided - falling back to utephonehub_user");
            }
            if (dbPassword == null || dbPassword.isBlank()) {
                dbPassword = "utephonehub_password";
                logger.info("DB_PASSWORD not provided - falling back to default development password");
            }
            
            // Construct JDBC URL if components are provided
            String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);
            properties.put("jakarta.persistence.jdbc.url", jdbcUrl);
            properties.put("jakarta.persistence.jdbc.user", dbUser);
            properties.put("jakarta.persistence.jdbc.password", dbPassword);
            
            logger.info("Using database URL: {}", jdbcUrl);
            logger.info("Using database user: {}", dbUser);
            
            entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);
            logger.info("EntityManagerFactory initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize EntityManagerFactory", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    /**
     * Load .env file for local development
     */
    private static void loadEnvFile() {
        try {
            java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
            if (java.nio.file.Files.exists(envPath)) {
                logger.info("Loading .env file for local development");
                java.util.List<String> lines = java.nio.file.Files.readAllLines(envPath);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex > 0) {
                        String key = line.substring(0, equalsIndex).trim();
                        String value = line.substring(equalsIndex + 1).trim();
                        
                        // Remove quotes if present
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        
                        // Only set if not already set by system
                        if (System.getenv(key) == null) {
                            // Note: Cannot modify System.getenv, so we'll use System.setProperty
                            System.setProperty(key, value);
                            logger.debug("Loaded env variable: {}", key);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load .env file: {}", e.getMessage());
        }
    }
    
    /**
     * Get environment variable or system property
     */
    private static String getEnvOrProperty(String key) {
        String value = System.getenv(key);
        if (value == null) {
            value = System.getProperty(key);
        }
        return value;
    }
    
    /**
     * Lấy EntityManager cho thread hiện tại
     * @return EntityManager instance
     */
    public static EntityManager getEntityManager() {
        EntityManager em = entityManagerThreadLocal.get();
        if (em == null || !em.isOpen()) {
            em = entityManagerFactory.createEntityManager();
            entityManagerThreadLocal.set(em);
            logger.debug("Created new EntityManager for thread: {}", Thread.currentThread().getName());
        } else {
            // Clear cache để tránh stale data
            em.clear();
            logger.debug("Cleared EntityManager cache for thread: {}", Thread.currentThread().getName());
        }
        return em;
    }
    
    /**
     * Đóng EntityManager cho thread hiện tại
     */
    public static void closeEntityManager() {
        EntityManager em = entityManagerThreadLocal.get();
        if (em != null && em.isOpen()) {
            em.close();
            entityManagerThreadLocal.remove();
            logger.debug("Closed EntityManager for thread: {}", Thread.currentThread().getName());
        }
    }
    
    /**
     * Bắt đầu transaction
     */
    public static void beginTransaction() {
        EntityManager em = getEntityManager();
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
            logger.debug("Transaction started");
        }
    }
    
    /**
     * Commit transaction
     */
    public static void commitTransaction() {
        EntityManager em = getEntityManager();
        if (em.getTransaction().isActive()) {
            em.getTransaction().commit();
            logger.debug("Transaction committed");
        }
    }
    
    /**
     * Rollback transaction
     */
    public static void rollbackTransaction() {
        EntityManager em = getEntityManager();
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
            logger.debug("Transaction rolled back");
        }
    }
    
    /**
     * Đóng EntityManagerFactory
     */
    public static void shutdown() {
        try {
            closeEntityManager();
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
                logger.info("EntityManagerFactory closed successfully");
            }
        } catch (Exception e) {
            logger.error("Error closing EntityManagerFactory", e);
        }
    }
    
    /**
     * Kiểm tra kết nối database
     * @return true nếu kết nối thành công
     */
    public static boolean testConnection() {
        try {
            EntityManager em = getEntityManager();
            em.createNativeQuery("SELECT 1").getSingleResult();
            logger.info("Database connection test successful");
            return true;
        } catch (Exception e) {
            logger.error("Database connection test failed", e);
            return false;
        }
    }
}
