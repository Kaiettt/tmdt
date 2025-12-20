package com.utephonehub.controller;

import com.utephonehub.service.UserService;
import com.utephonehub.service.RedisService;
import com.utephonehub.dto.request.LoginRequest;
import com.utephonehub.dto.request.RegisterRequest;
import com.utephonehub.dto.response.LoginResponse;
import com.utephonehub.dto.response.UserResponse;
import com.utephonehub.exception.BusinessException;
import com.utephonehub.exception.ValidationException;
import com.utephonehub.util.JsonUtil;
import com.utephonehub.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 * Xử lý các request liên quan đến authentication
 */
@WebServlet("/api/v1/auth/*")
public class AuthController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(AuthController.class);
    private final UserService userService;
    private final RedisService redisService;
    private final JsonUtil jsonUtil;
    private final JwtUtil jwtUtil;
    
    public AuthController() {
        this.userService = new UserService();
        this.redisService = new RedisService();
        this.jsonUtil = new JsonUtil();
        this.jwtUtil = new JwtUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        try {
            if ("/login".equals(pathInfo)) {
                // Forward to login page
                request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(request, response);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Page not found");
            }
        } catch (Exception e) {
            logger.error("Error in doGet", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        try {
            switch (pathInfo) {
                case "/register":
                    handleRegister(request, response);
                    break;
                case "/login":
                    handleLogin(request, response);
                    break;
                case "/refresh":
                    handleRefreshToken(request, response);
                    break;
                case "/logout":
                    handleLogout(request, response);
                    break;
                default:
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in AuthController", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * Xử lý đăng ký user
     */
    private void handleRegister(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Parse request body
            RegisterRequest registerRequest = jsonUtil.parseJson(request, RegisterRequest.class);
            
            // Register user
            LoginResponse loginResponse = userService.register(registerRequest);
            
            // Set refresh token as HttpOnly cookie (SECURITY IMPROVEMENT)
            setRefreshTokenCookie(response, loginResponse.getRefreshToken());
            
            // Remove refresh token from response body
            loginResponse.setRefreshToken(null);
            
            // Send success response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Đăng ký tài khoản thành công");
            responseData.put("data", loginResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, responseData);
            
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("validation")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else if (e.getMessage() != null && e.getMessage().contains("đã tồn tại")) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            } else {
                logger.error("Error in register: " + e.getMessage(), e);
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống");
            }
        }
    }
    
    /**
     * Xử lý đăng nhập user
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Parse request body
            LoginRequest loginRequest = jsonUtil.parseJson(request, LoginRequest.class);
            
            // Login user
            LoginResponse loginResponse = userService.login(loginRequest);
            
            // Set refresh token as HttpOnly cookie (SECURITY IMPROVEMENT)
            setRefreshTokenCookie(response, loginResponse.getRefreshToken());
            
            // Remove refresh token from response body
            loginResponse.setRefreshToken(null);
            
            // Send success response (only accessToken and user info)
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Đăng nhập thành công");
            responseData.put("data", loginResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("validation")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else if (e.getMessage() != null && (e.getMessage().contains("không đúng") || e.getMessage().contains("khóa"))) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            } else {
                logger.error("Error in login: " + e.getMessage(), e);
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống");
            }
        }
    }
    
    /**
     * Xử lý refresh token
     */
    private void handleRefreshToken(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        try {
            // Get refresh token from HttpOnly cookie (SECURITY IMPROVEMENT)
            String refreshToken = getRefreshTokenFromCookie(request);
            
            // Fallback: Try to get from request body (for backward compatibility)
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                Map<String, String> requestBody = jsonUtil.parseJson(request, Map.class);
                refreshToken = requestBody.get("refreshToken");
            }
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Refresh token không được để trống");
                return;
            }
            
            // Verify refresh token
            String email = jwtUtil.getEmailFromToken(refreshToken);
            Long userId = jwtUtil.getUserIdFromToken(refreshToken);
            String role = jwtUtil.getRoleFromToken(refreshToken);
            
            if (email == null || !jwtUtil.validateToken(refreshToken)) {
                // Clear invalid cookie
                clearRefreshTokenCookie(response);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn");
                return;
            }
            
            // Check if refresh token exists in Redis
            String storedRefreshToken = redisService.getRefreshToken(email);
            if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
                clearRefreshTokenCookie(response);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Refresh token không hợp lệ");
                return;
            }
            
            // Generate new access token with userId and role
            String newAccessToken = jwtUtil.generateToken(userId, email, role);
            
            // Send response with new access token (refresh token stays in cookie)
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Làm mới token thành công");
            Map<String, String> data = new HashMap<>();
            data.put("accessToken", newAccessToken);
            responseData.put("data", data);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (Exception e) {
            logger.error("Error in refresh token", e);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Làm mới token thất bại");
        }
    }
    
    /**
     * Xử lý logout - xóa refresh token
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        String email = null;
        String token = null;
        
        try {
            // Get token from Authorization header
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            
            if (token == null) {
                // Try to get from request body
                try {
                    Map<String, String> requestBody = jsonUtil.parseJson(request, Map.class);
                    token = requestBody.get("accessToken");
                } catch (Exception e) {
                    logger.warn("Could not parse request body for logout: {}", e.getMessage());
                }
            }
            
            if (token != null && !token.trim().isEmpty()) {
                try {
                    // Extract email from token
                    email = jwtUtil.getEmailFromToken(token);
                    
                    if (email != null && !email.trim().isEmpty()) {
                        // Delete refresh token from Redis
                        redisService.deleteRefreshToken(email);
                        
                        // Blacklist the access token
                        try {
                            long expiryTime = jwtUtil.getExpirationDateFromToken(token).getTime() - System.currentTimeMillis();
                            if (expiryTime > 0) {
                                redisService.blacklistAccessToken(token, expiryTime / 1000);
                            }
                        } catch (Exception e) {
                            logger.warn("Could not blacklist access token: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error extracting email from token", e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in logout flow", e);
        } finally {
            // ALWAYS clear refresh token cookie regardless of errors
            clearRefreshTokenCookie(response);
            
            // Send success response (logout always succeeds from client perspective)
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Đăng xuất thành công");
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        }
    }
    
    /**
     * Set refresh token as HttpOnly cookie
     * SECURITY: HttpOnly cookie cannot be accessed by JavaScript (XSS protection)
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        // Use Set-Cookie header directly for better control over SameSite attribute
        String cookieValue = String.format(
            "refreshToken=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax",
            refreshToken,
            7 * 24 * 60 * 60 // 7 days in seconds
        );
        response.addHeader("Set-Cookie", cookieValue);
    }
    
    /**
     * Get refresh token from cookie
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Clear refresh token cookie
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        // Use Set-Cookie header to properly delete the cookie
        String cookieValue = "refreshToken=; Path=/; HttpOnly; Max-Age=0; SameSite=Lax";
        response.addHeader("Set-Cookie", cookieValue);
    }
    
    /**
     * Gửi JSON response thành công
     */
    private void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) 
            throws IOException {
        
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = jsonUtil.toJson(data);
        response.getWriter().write(jsonResponse);
        
        logger.debug("Sent JSON response: {}", jsonResponse);
    }
    
    /**
     * Gửi error response
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) 
            throws IOException {
        
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        
        String jsonResponse = jsonUtil.toJson(errorResponse);
        response.getWriter().write(jsonResponse);
        
        logger.debug("Sent error response: {}", jsonResponse);
    }
}
