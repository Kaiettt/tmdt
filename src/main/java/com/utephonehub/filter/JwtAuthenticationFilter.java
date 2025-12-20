package com.utephonehub.filter;

import com.utephonehub.util.JwtUtil;
import com.utephonehub.service.RedisService;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * Filter để xác thực JWT token
 */
@WebFilter("/api/v1/*")
public class JwtAuthenticationFilter implements Filter {
    
    private static final Logger logger = LogManager.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    
    // Các endpoint không cần authentication
    private static final String[] PUBLIC_ENDPOINTS = {
        // Auth endpoints
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password/*",  // Forgot password flow: /request, /verify, /reset
        "/api/v1/auth/verify-email",
        
        // Public product endpoints
        "/api/v1/products",
        "/api/v1/products/*",
        
        // Public order lookup (không cần login)
        "/api/v1/orders/lookup",
        
        // Public category & brand
        "/api/v1/categories",
        "/api/v1/categories/*",
        "/api/v1/brands",
        "/api/v1/brands/*",
        
        // Voucher validation (public - can be used before login)
        "/api/v1/vouchers/validate",
        
        // Location/Address public data
        "/api/v1/location/*",
        
        // Health check
        "/api/v1/health"
    };
    
    public JwtAuthenticationFilter() {
        this.jwtUtil = new JwtUtil();
        this.redisService = new RedisService();
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("JWT Authentication Filter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Get request path without context path
        String contextPath = httpRequest.getContextPath();
        String requestURI = httpRequest.getRequestURI();
        String path = requestURI.substring(contextPath.length());
        String method = httpRequest.getMethod();
        
        logger.debug("JWT Filter - {} {} (context: {}, full URI: {})", method, path, contextPath, requestURI);
        
        // Kiểm tra xem endpoint có public không
        if (isPublicEndpoint(path, method)) {
            logger.debug("Public endpoint, skipping authentication: {} {}", method, path);
            chain.doFilter(request, response);
            return;
        }
        
        // Lấy token từ Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        String token = jwtUtil.extractTokenFromHeader(authHeader);
        
        if (token == null) {
            logger.warn("No JWT token found in request: {}", path);
            sendUnauthorizedResponse(httpResponse, "Access token is required");
            return;
        }
        
        // Validate token
        if (!jwtUtil.validateToken(token)) {
            logger.warn("Invalid JWT token for request: {}", requestURI);
            sendUnauthorizedResponse(httpResponse, "Invalid or expired access token");
            return;
        }
        
        // Check if token is blacklisted (logged out)
        if (redisService.isTokenBlacklisted(token)) {
            logger.warn("Blacklisted JWT token (logged out) for request: {}", requestURI);
            sendUnauthorizedResponse(httpResponse, "Token has been revoked. Please login again");
            return;
        }
        
        // Kiểm tra token có hết hạn không
        if (jwtUtil.isTokenExpired(token)) {
            logger.warn("Expired JWT token for request: {}", requestURI);
            sendUnauthorizedResponse(httpResponse, "Access token has expired");
            return;
        }
        
        // Lấy user info từ token và set vào request attributes
        try {
            String email = jwtUtil.getEmailFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            
            httpRequest.setAttribute("currentUserEmail", email);
            httpRequest.setAttribute("currentUserId", userId);
            httpRequest.setAttribute("currentUserRole", role);
            
            logger.debug("User authenticated: {} (userId: {}, role: {})", email, userId, role);
            
            // Kiểm tra quyền admin cho admin endpoints
            if (path.contains("/admin/") && !"admin".equalsIgnoreCase(role)) {
                logger.warn("Non-admin user {} attempted to access admin endpoint: {}", email, path);
                sendForbiddenResponse(httpResponse, "Admin access required");
                return;
            }
        } catch (Exception e) {
            logger.error("Error extracting user info from token", e);
            sendUnauthorizedResponse(httpResponse, "Invalid access token");
            return;
        }
        
        // Tiếp tục filter chain
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        logger.info("JWT Authentication Filter destroyed");
    }
    
    /**
     * Kiểm tra endpoint có public không
     * @param requestURI Request URI
     * @param method HTTP method (GET, POST, etc.)
     * @return true nếu là public endpoint
     */
    private boolean isPublicEndpoint(String requestURI, String method) {
        // Special case: POST /api/v1/products/{id}/reviews requires authentication
        if ("POST".equals(method) && requestURI.matches("/api/v1/products/\\d+/reviews")) {
            return false;
        }
        
        // Special case: GET /api/v1/products/{id}/reviews is public (anyone can read reviews)
        if ("GET".equals(method) && requestURI.matches("/api/v1/products/\\d+/reviews")) {
            return true;
        }
        
        for (String endpoint : PUBLIC_ENDPOINTS) {
            // Exact match
            if (requestURI.equals(endpoint)) {
                return true;
            }
            // Wildcard pattern matching (support /* suffix)
            if (endpoint.endsWith("/*") && requestURI.startsWith(endpoint.substring(0, endpoint.length() - 1))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gửi response 401 Unauthorized
     * @param response HttpServletResponse
     * @param message Error message
     * @throws IOException
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"success\": false, \"message\": \"%s\"}", message
        );
        
        response.getWriter().write(jsonResponse);
    }
    
    /**
     * Gửi response 403 Forbidden
     * @param response HttpServletResponse
     * @param message Error message
     * @throws IOException
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"success\": false, \"message\": \"%s\"}", message
        );
        
        response.getWriter().write(jsonResponse);
    }
}
