package com.utephonehub.controller;

import com.utephonehub.service.GoogleOAuthService;
import com.utephonehub.service.UserService;
import com.utephonehub.dto.response.LoginResponse;
import com.utephonehub.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

/**
 * Google OAuth Controller
 * Handles Google OAuth2 authentication flow
 */
@WebServlet("/oauth2/*")
public class GoogleAuthController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(GoogleAuthController.class);
    private final GoogleOAuthService googleOAuthService;
    private final UserService userService;
    private final JsonUtil jsonUtil;
    
    public GoogleAuthController() {
        this.googleOAuthService = new GoogleOAuthService();
        this.userService = new UserService();
        this.jsonUtil = new JsonUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        try {
            if ("/google".equals(pathInfo)) {
                // Redirect to Google authorization URL
                handleGoogleRedirect(response);
            } else if ("/callback/google".equals(pathInfo)) {
                // Handle callback from Google
                handleGoogleCallback(request, response);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "OAuth endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in GoogleAuthController", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth authentication failed");
        }
    }
    
    /**
     * Redirect user to Google authorization page
     */
    private void handleGoogleRedirect(HttpServletResponse response) throws IOException {
        try {
            String authUrl = googleOAuthService.getAuthorizationUrl();
            response.sendRedirect(authUrl);
        } catch (Exception e) {
            logger.error("Failed to generate Google authorization URL", e);
            throw new IOException("Failed to initiate Google OAuth", e);
        }
    }
    
    /**
     * Handle callback from Google with authorization code
     */
    private void handleGoogleCallback(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Get authorization code from query params
        String code = request.getParameter("code");
        String error = request.getParameter("error");
        
        // Check for errors from Google
        if (error != null) {
            logger.error("Google OAuth error: {}", error);
            response.sendRedirect("/login.jsp?error=oauth_failed");
            return;
        }
        
        if (code == null || code.isEmpty()) {
            logger.error("No authorization code received from Google");
            response.sendRedirect("/login.jsp?error=oauth_failed");
            return;
        }
        
        try {
            // Verify code and get user info from Google
            Map<String, Object> userInfo = googleOAuthService.verifyAndGetUserInfo(code);
            
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String picture = (String) userInfo.get("picture");
            String googleId = (String) userInfo.get("googleId");
            
            logger.info("Google OAuth successful for user: {}", email);
            
            // Find or create user in database
            LoginResponse loginResponse = userService.findOrCreateOAuthUser(email, name, picture, googleId);
            
            // Set refresh token cookie (HttpOnly, secure)
            setRefreshTokenCookie(response, loginResponse.getRefreshToken());
            
            // Set access token cookie (will be read by JavaScript)
            setAccessTokenCookie(response, loginResponse.getAccessToken());
            
            // Redirect to home page (JavaScript will read token from cookie and fetch user info via /api/v1/user/me)
            String homeUrl = request.getContextPath() + "/?oauth_success=true";
            response.sendRedirect(homeUrl);
            
        } catch (SecurityException e) {
            logger.error("Security exception during Google OAuth: {}", e.getMessage());
            response.sendRedirect("/login.jsp?error=oauth_security");
        } catch (Exception e) {
            logger.error("Error processing Google OAuth callback", e);
            response.sendRedirect("/login.jsp?error=oauth_failed");
        }
    }
    
    /**
     * Set refresh token as HttpOnly cookie (secure, not accessible by JavaScript)
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        int maxAge = 7 * 24 * 60 * 60; // 7 days
        String cookieValue = String.format(
            "refreshToken=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax",
            refreshToken,
            maxAge
        );
        response.addHeader("Set-Cookie", cookieValue);
    }
    
    /**
     * Set access token as regular cookie (accessible by JavaScript for API calls)
     */
    private void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        int maxAge = 24 * 60 * 60; // 24 hours
        String cookieValue = String.format(
            "accessToken=%s; Path=/; Max-Age=%d; SameSite=Lax",
            accessToken,
            maxAge
        );
        response.addHeader("Set-Cookie", cookieValue);
    }
    
    /**
     * Send error response as JSON
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) 
            throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = Map.of(
            "success", false,
            "message", message
        );
        
        response.getWriter().write(jsonUtil.toJson(errorResponse));
    }
}
