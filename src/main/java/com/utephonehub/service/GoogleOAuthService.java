package com.utephonehub.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

/**
 * Google OAuth Service
 * Handles Google OAuth2 authentication flow
 */
public class GoogleOAuthService {
    
    private static final Logger logger = LogManager.getLogger(GoogleOAuthService.class);
    
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final NetHttpTransport httpTransport;
    private final GsonFactory jsonFactory;
    
    public GoogleOAuthService() {
        this.clientId = System.getenv("GOOGLE_CLIENT_ID");
        this.clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
        this.redirectUri = System.getenv("GOOGLE_REDIRECT_URI");
        this.httpTransport = new NetHttpTransport();
        this.jsonFactory = GsonFactory.getDefaultInstance();
        
        if (clientId == null || clientSecret == null || redirectUri == null) {
            logger.error("Google OAuth credentials not configured in environment variables");
            throw new IllegalStateException("Google OAuth not properly configured");
        }
    }
    
    /**
     * Get Google OAuth2 authorization URL
     * @return Authorization URL to redirect user to Google login
     */
    public String getAuthorizationUrl() {
        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                clientId,
                clientSecret,
                Collections.singletonList("openid profile email")
            ).build();
            
            String authUrl = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setAccessType("offline")
                .build();
                
            logger.info("Generated Google OAuth authorization URL");
            return authUrl;
        } catch (Exception e) {
            logger.error("Error generating authorization URL", e);
            throw new RuntimeException("Failed to generate Google authorization URL", e);
        }
    }
    
    /**
     * Exchange authorization code for tokens and verify ID token
     * @param code Authorization code from Google callback
     * @return Map containing user info (sub, email, name, picture)
     */
    public Map<String, Object> verifyAndGetUserInfo(String code) throws IOException, GeneralSecurityException {
        // Exchange code for tokens
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
            httpTransport,
            jsonFactory,
            clientId,
            clientSecret,
            code,
            redirectUri
        ).execute();
        
        // Verify ID token
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
            .setAudience(Collections.singletonList(clientId))
            .build();
            
        GoogleIdToken idToken = verifier.verify(tokenResponse.getIdToken());
        if (idToken == null) {
            logger.error("Invalid ID token received from Google");
            throw new SecurityException("Invalid ID token");
        }
        
        // Extract user info
        GoogleIdToken.Payload payload = idToken.getPayload();
        String userId = payload.getSubject();
        String email = payload.getEmail();
        boolean emailVerified = payload.getEmailVerified();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        
        if (!emailVerified) {
            logger.warn("Google email not verified for user: {}", email);
            throw new SecurityException("Email not verified by Google");
        }
        
        logger.info("Successfully verified Google user: {} ({})", name, email);
        
        return Map.of(
            "googleId", userId,
            "email", email,
            "name", name,
            "picture", pictureUrl != null ? pictureUrl : ""
        );
    }
}
