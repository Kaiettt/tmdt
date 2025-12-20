package com.utephonehub.controller;

import com.utephonehub.config.DatabaseConfig;
import com.utephonehub.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Kiểm tra trạng thái hệ thống
 */
@WebServlet("/api/v1/health")
public class HealthController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(HealthController.class);
    private final JsonUtil jsonUtil;
    
    public HealthController() {
        this.jsonUtil = new JsonUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        logger.info("Health check request received");
        
        try {
            Map<String, Object> healthData = new HashMap<>();
            healthData.put("status", "UP");
            healthData.put("timestamp", LocalDateTime.now());
            healthData.put("version", "1.0.0");
            healthData.put("environment", "development");
            
            // Check database connection
            boolean dbStatus = DatabaseConfig.testConnection();
            healthData.put("database", dbStatus ? "UP" : "DOWN");
            
            // Check overall status
            // if (!dbStatus) {
            //     healthData.put("status", "DOWN");
            // }
            
            // Send response
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            String jsonResponse = "{\"status\":\"" + healthData.get("status") + "\",\"timestamp\":\"" + 
                healthData.get("timestamp") + "\",\"version\":\"" + healthData.get("version") + 
                "\",\"environment\":\"" + healthData.get("environment") + "\",\"database\":\"" + 
                healthData.get("database") + "\"}";
            response.getWriter().write(jsonResponse);
            
            logger.info("Health check completed: {}", healthData.get("status"));
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("status", "DOWN");
            errorData.put("timestamp", LocalDateTime.now());
            errorData.put("error", "Health check failed");
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            String jsonResponse = "{\"status\":\"DOWN\",\"timestamp\":\"" + 
                LocalDateTime.now() + "\",\"error\":\"Health check failed\"}";
            response.getWriter().write(jsonResponse);
        }
    }
}
