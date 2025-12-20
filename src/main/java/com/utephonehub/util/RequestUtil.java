package com.utephonehub.util;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Request Helper Utility
 * Helper methods để extract user info từ request attributes (JWT)
 */
public class RequestUtil {
    
    private static final Logger logger = LogManager.getLogger(RequestUtil.class);
    
    /**
     * Get current user ID từ JWT token (via request attribute)
     * @param request HttpServletRequest
     * @return User ID hoặc null nếu không có
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        try {
            Object userId = request.getAttribute("currentUserId");
            if (userId instanceof Long) {
                return (Long) userId;
            } else if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            
            // Fallback: Try X-User-Id header for backward compatibility
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
                logger.warn("Using deprecated X-User-Id header. Please use JWT authentication.");
                return Long.parseLong(userIdHeader);
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error getting current user ID from request", e);
            return null;
        }
    }
    
    /**
     * Get current user email từ JWT token (via request attribute)
     * @param request HttpServletRequest
     * @return User email hoặc null nếu không có
     */
    public static String getCurrentUserEmail(HttpServletRequest request) {
        try {
            Object email = request.getAttribute("currentUserEmail");
            if (email instanceof String) {
                return (String) email;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting current user email from request", e);
            return null;
        }
    }
    
    /**
     * Get current user role từ JWT token (via request attribute)
     * @param request HttpServletRequest
     * @return User role (admin/customer) hoặc null nếu không có
     */
    public static String getCurrentUserRole(HttpServletRequest request) {
        try {
            Object role = request.getAttribute("currentUserRole");
            if (role instanceof String) {
                return (String) role;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting current user role from request", e);
            return null;
        }
    }
    
    /**
     * Check if current user is admin
     * @param request HttpServletRequest
     * @return true nếu user là admin
     */
    public static boolean isAdmin(HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        return "admin".equalsIgnoreCase(role);
    }
    
    /**
     * Check if current user is customer
     * @param request HttpServletRequest
     * @return true nếu user là customer
     */
    public static boolean isCustomer(HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        return "customer".equalsIgnoreCase(role);
    }
    
    /**
     * Check if user is authenticated (có userId trong request)
     * @param request HttpServletRequest
     * @return true nếu user đã authenticated
     */
    public static boolean isAuthenticated(HttpServletRequest request) {
        return getCurrentUserId(request) != null;
    }
    
    /**
     * Validate user ownership - check if current user owns the resource
     * @param request HttpServletRequest
     * @param resourceUserId ID của user sở hữu resource
     * @return true nếu current user owns resource hoặc là admin
     */
    public static boolean validateOwnership(HttpServletRequest request, Long resourceUserId) {
        if (resourceUserId == null) {
            return false;
        }
        
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return false;
        }
        
        // Admin có thể access mọi resource
        if (isAdmin(request)) {
            return true;
        }
        
        // User chỉ access được resource của mình
        return currentUserId.equals(resourceUserId);
    }
}
