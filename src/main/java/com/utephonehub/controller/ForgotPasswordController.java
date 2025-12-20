package com.utephonehub.controller;

import com.google.gson.Gson;
import com.utephonehub.dto.ApiResponse;
import com.utephonehub.service.EmailService;
import com.utephonehub.service.RedisService;
import com.utephonehub.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

/**
 * Forgot Password Controller
 * Handle forgot password flow: request OTP, verify OTP, reset password
 */
@WebServlet("/api/v1/auth/forgot-password/*")
public class ForgotPasswordController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(ForgotPasswordController.class);
    private final Gson gson = new Gson();
    private final EmailService emailService = new EmailService();
    private final RedisService redisService = new RedisService();
    private final UserService userService = new UserService();
    
    /**
     * POST: Handle forgot password requests
     * Endpoints:
     * - /api/v1/auth/forgot-password/request - Send OTP to email
     * - /api/v1/auth/forgot-password/verify - Verify OTP
     * - /api/v1/auth/forgot-password/reset - Reset password
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            if (pathInfo == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint");
                return;
            }
            
            switch (pathInfo) {
                case "/request":
                    handleRequestOTP(request, response);
                    break;
                case "/verify":
                    handleVerifyOTP(request, response);
                    break;
                case "/reset":
                    handleResetPassword(request, response);
                    break;
                default:
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
            
        } catch (Exception e) {
            logger.error("Error in forgot password flow", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Có lỗi xảy ra, vui lòng thử lại sau");
        }
    }
    
    /**
     * GET: Redirect to homepage
     * Prevent direct access to API endpoints via browser
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("/");
    }
    
    /**
     * Handle request OTP
     */
    private void handleRequestOTP(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        // Parse request body
        Map<String, String> requestData = parseRequestBody(request);
        String email = requestData.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Email không được để trống");
            return;
        }
        
        // Check if user exists
        if (!userService.isEmailExists(email)) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Email này chưa được đăng ký");
            return;
        }
        
        // Check resend cooldown (1 minute)
        if (!redisService.canResendOTP(email)) {
            sendErrorResponse(response, 429, // 429 Too Many Requests
                    "Vui lòng đợi 1 phút trước khi gửi lại mã xác nhận");
            return;
        }
        
        // Generate OTP
        String otp = emailService.generateOTP();
        
        // Store OTP in Redis (5 minutes TTL)
        redisService.storeForgotPasswordOTP(email, otp);
        
        // Clear any previous failed attempts
        redisService.clearOTPAttempts(email);
        
        // Set resend cooldown
        redisService.setResendCooldown(email);
        
        // Send OTP email
        boolean emailSent = emailService.sendForgotPasswordEmail(email, otp);
        
        if (!emailSent) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể gửi email, vui lòng thử lại sau");
            return;
        }
        
        logger.info("Forgot password OTP sent to email: {}", email);
        
        // Send success response
        ApiResponse apiResponse = new ApiResponse(true, "Mã xác nhận đã được gửi đến email của bạn (có hiệu lực 5 phút)", null);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(apiResponse));
    }
    
    /**
     * Handle verify OTP
     */
    private void handleVerifyOTP(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        // Parse request body
        Map<String, String> requestData = parseRequestBody(request);
        String email = requestData.get("email");
        String otp = requestData.get("otp");
        
        if (email == null || otp == null || email.trim().isEmpty() || otp.trim().isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Email và OTP không được để trống");
            return;
        }
        
        // Check if OTP verification is blocked (anti-spam)
        if (redisService.isOTPVerificationBlocked(email)) {
            sendErrorResponse(response, 429, // 429 Too Many Requests
                    "Bạn đã nhập sai mã quá nhiều lần. Vui lòng yêu cầu mã mới");
            return;
        }
        
        // Verify OTP
        boolean isValid = redisService.verifyForgotPasswordOTP(email, otp);
        
        if (!isValid) {
            // Increment failed attempts
            int attempts = redisService.incrementOTPAttempts(email);
            int remaining = 5 - attempts;
            
            if (remaining > 0) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "Mã xác nhận không hợp lệ. Còn " + remaining + " lần thử");
            } else {
                sendErrorResponse(response, 429, 
                        "Bạn đã nhập sai mã quá nhiều lần. Vui lòng yêu cầu mã mới");
            }
            return;
        }
        
        // Clear attempts counter after successful verification
        redisService.clearOTPAttempts(email);
        
        logger.info("Forgot password OTP verified for email: {}", email);
        
        // Send success response
        ApiResponse apiResponse = new ApiResponse(true, "Xác nhận thành công", null);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(apiResponse));
    }
    
    /**
     * Handle reset password
     */
    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        // Parse request body
        Map<String, String> requestData = parseRequestBody(request);
        String email = requestData.get("email");
        String otp = requestData.get("otp");
        String newPassword = requestData.get("newPassword");
        
        System.out.println("=== Reset Password Request ===");
        System.out.println("Email: " + email);
        System.out.println("OTP provided: " + (otp != null ? otp : "null"));
        System.out.println("Password length: " + (newPassword != null ? newPassword.length() : 0));
        
        if (email == null || newPassword == null || email.trim().isEmpty() || newPassword.trim().isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Email và mật khẩu mới không được để trống");
            return;
        }
        
        // Validate password strength (same as register)
        if (newPassword.length() < 6) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }
        
        // Password must contain at least one letter and one number
        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Mật khẩu phải chứa ít nhất một chữ cái và một số");
            return;
        }
        
        System.out.println("Password validation passed");
        
        // Verify OTP one more time before reset (security check)
        if (otp != null && !otp.trim().isEmpty()) {
            System.out.println("Verifying OTP...");
            boolean isValid = redisService.verifyForgotPasswordOTP(email, otp);
            System.out.println("OTP valid: " + isValid);
            if (!isValid) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "Mã xác nhận không hợp lệ hoặc đã hết hạn");
                return;
            }
        }
        
        System.out.println("Calling userService.resetPassword()...");
        
        // Reset password
        boolean success = userService.resetPassword(email, newPassword);
        
        System.out.println("Reset password result: " + success);
        
        if (!success) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể đặt lại mật khẩu, vui lòng thử lại");
            return;
        }
        
        // Delete OTP after successful password reset
        redisService.deleteForgotPasswordOTP(email);
        
        logger.info("Password reset successful for email: {}", email);
        
        // Send success response
        ApiResponse apiResponse = new ApiResponse(true, "Đặt lại mật khẩu thành công", null);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(apiResponse));
    }
    
    /**
     * Parse JSON request body
     */
    private Map<String, String> parseRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return gson.fromJson(sb.toString(), Map.class);
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message)
            throws IOException {
        ApiResponse apiResponse = new ApiResponse(false, message, null);
        response.setStatus(statusCode);
        response.getWriter().write(gson.toJson(apiResponse));
    }
}
