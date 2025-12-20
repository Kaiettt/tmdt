package com.utephonehub.controller;

import com.utephonehub.dto.response.UserResponse;
import com.utephonehub.entity.User;
import com.utephonehub.repository.UserRepository;
import com.utephonehub.util.JsonUtil;
import com.utephonehub.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin User Management Controller
 * Quản lý người dùng - chỉ dành cho admin
 */
@WebServlet("/api/v1/admin/users/*")
public class AdminUserController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(AdminUserController.class);
    private final UserRepository userRepository;
    private final JsonUtil jsonUtil;
    
    public AdminUserController() {
        this.userRepository = new UserRepository();
        this.jsonUtil = new JsonUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check admin authentication
        if (!isAdmin(request, response)) {
            return;
        }
        
        String pathInfo = request.getPathInfo();
        logger.info("AdminUserController GET request: {}", pathInfo);
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/v1/admin/users - Lấy danh sách users
                handleGetUsers(request, response);
            } else if (pathInfo.equals("/stats")) {
                // GET /api/v1/admin/users/stats - Thống kê users
                handleGetUserStats(request, response);
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/v1/admin/users/{id} - Chi tiết user
                handleGetUserDetail(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in AdminUserController GET", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check admin authentication
        if (!isAdmin(request, response)) {
            return;
        }
        
        logger.info("AdminUserController POST request");
        
        try {
            // POST /api/v1/admin/users - Tạo user mới
            handleCreateUser(request, response);
        } catch (Exception e) {
            logger.error("Error in AdminUserController POST", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check admin authentication
        if (!isAdmin(request, response)) {
            return;
        }
        
        String pathInfo = request.getPathInfo();
        logger.info("AdminUserController PUT request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+/status")) {
                // PUT /api/v1/admin/users/{id}/status - Cập nhật trạng thái user
                handleUpdateUserStatus(request, response, pathInfo);
            } else if (pathInfo != null && pathInfo.matches("/\\d+/role")) {
                // PUT /api/v1/admin/users/{id}/role - Cập nhật role user
                handleUpdateUserRole(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint");
            }
        } catch (Exception e) {
            logger.error("Error in AdminUserController PUT", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check admin authentication
        if (!isAdmin(request, response)) {
            return;
        }
        
        String pathInfo = request.getPathInfo();
        logger.info("AdminUserController DELETE request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // DELETE /api/v1/admin/users/{id} - Xóa user
                handleDeleteUser(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "User ID is required");
            }
        } catch (Exception e) {
            logger.error("Error in AdminUserController DELETE", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * Lấy danh sách users với filters
     */
    private void handleGetUsers(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Get query parameters
            String roleStr = request.getParameter("role");
            String statusStr = request.getParameter("status");
            String keyword = request.getParameter("keyword");
            
            List<User> users = userRepository.findAll();
            
            // Filter by role if provided
            if (roleStr != null && !roleStr.isEmpty()) {
                try {
                    User.UserRole role = User.UserRole.valueOf(roleStr.toLowerCase());
                    users = users.stream()
                        .filter(u -> u.getRole() == role)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid user role: {}", roleStr);
                }
            }
            
            // Filter by status if provided
            if (statusStr != null && !statusStr.isEmpty()) {
                try {
                    User.UserStatus status = User.UserStatus.valueOf(statusStr.toLowerCase());
                    users = users.stream()
                        .filter(u -> u.getStatus() == status)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid user status: {}", statusStr);
                }
            }
            
            // Filter by keyword (search in username, email, fullName)
            if (keyword != null && !keyword.isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                users = users.stream()
                    .filter(u -> 
                        u.getUsername().toLowerCase().contains(lowerKeyword) ||
                        u.getEmail().toLowerCase().contains(lowerKeyword) ||
                        u.getFullName().toLowerCase().contains(lowerKeyword)
                    )
                    .collect(Collectors.toList());
            }
            
            // Convert to response DTOs
            List<UserResponse> userResponses = users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy danh sách người dùng thành công");
            responseData.put("data", userResponses);
            responseData.put("total", userResponses.size());
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (Exception e) {
            logger.error("Error getting users", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy danh sách người dùng");
        }
    }
    
    /**
     * Lấy thống kê users
     */
    private void handleGetUserStats(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            List<User> allUsers = userRepository.findAll();
            
            long total = allUsers.size();
            long active = allUsers.stream().filter(u -> u.getStatus() == User.UserStatus.active).count();
            long locked = allUsers.stream().filter(u -> u.getStatus() == User.UserStatus.locked).count();
            long pending = allUsers.stream().filter(u -> u.getStatus() == User.UserStatus.pending).count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("active", active);
            stats.put("locked", locked);
            stats.put("pending", pending);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("data", stats);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (Exception e) {
            logger.error("Error getting user stats", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy thống kê người dùng");
        }
    }
    
    /**
     * Tạo user mới
     */
    private void handleCreateUser(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Parse request body
            Map<String, Object> requestBody = jsonUtil.parseJson(request, Map.class);
            
            String email = (String) requestBody.get("email");
            String username = (String) requestBody.get("username");
            String fullName = (String) requestBody.get("fullName");
            String password = (String) requestBody.get("password");
            String phoneNumber = (String) requestBody.get("phoneNumber");
            String roleStr = (String) requestBody.get("role");
            String statusStr = (String) requestBody.get("status");
            
            // Validate required fields
            if (email == null || email.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Email không được để trống");
                return;
            }
            
            // Username is optional, auto-generate from email if not provided
            if (username == null || username.trim().isEmpty()) {
                // Extract username from email (part before @)
                username = email.split("@")[0];
            }
            
            if (fullName == null || fullName.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Họ tên không được để trống");
                return;
            }
            
            if (password == null || password.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Mật khẩu không được để trống");
                return;
            }
            
            if (password.length() < 6) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Mật khẩu phải có ít nhất 6 ký tự");
                return;
            }
            
            // Check email uniqueness
            if (userRepository.findByEmail(email).isPresent()) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Email đã tồn tại");
                return;
            }
            
            // Check username uniqueness
            if (userRepository.findByUsername(username).isPresent()) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Username đã tồn tại");
                return;
            }
            
            // Create new user
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setFullName(fullName);
            user.setPasswordHash(PasswordUtil.hashPassword(password));
            user.setPhoneNumber(phoneNumber);
            
            // Set role
            if (roleStr != null && !roleStr.trim().isEmpty()) {
                try {
                    User.UserRole role = User.UserRole.valueOf(roleStr.toLowerCase());
                    user.setRole(role);
                } catch (IllegalArgumentException e) {
                    user.setRole(User.UserRole.customer);
                }
            } else {
                user.setRole(User.UserRole.customer);
            }
            
            // Set status
            if (statusStr != null && !statusStr.trim().isEmpty()) {
                try {
                    User.UserStatus status = User.UserStatus.valueOf(statusStr.toLowerCase());
                    user.setStatus(status);
                } catch (IllegalArgumentException e) {
                    user.setStatus(User.UserStatus.active);
                }
            } else {
                user.setStatus(User.UserStatus.active);
            }
            
            // Save user
            User savedUser = userRepository.save(user);
            UserResponse userResponse = convertToUserResponse(savedUser);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Tạo người dùng thành công");
            responseData.put("data", userResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, responseData);
            
        } catch (Exception e) {
            logger.error("Error creating user", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi tạo người dùng");
        }
    }
    
    /**
     * Lấy chi tiết user
     */
    private void handleGetUserDetail(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long userId = Long.parseLong(pathInfo.substring(1));
            
            User user = userRepository.findById(userId)
                .orElse(null);
            
            if (user == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy người dùng");
                return;
            }
            
            UserResponse userResponse = convertToUserResponse(user);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy thông tin người dùng thành công");
            responseData.put("data", userResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "User ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error getting user detail", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy thông tin người dùng");
        }
    }
    
    /**
     * Cập nhật trạng thái user
     */
    private void handleUpdateUserStatus(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            // Extract user ID from path: /123/status -> 123
            String idPart = pathInfo.substring(1, pathInfo.lastIndexOf("/"));
            Long userId = Long.parseLong(idPart);
            
            // Check user exists
            User user = userRepository.findById(userId)
                .orElse(null);
            
            if (user == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy người dùng");
                return;
            }
            
            // Prevent admin from locking themselves
            Long adminId = (Long) request.getAttribute("currentUserId");
            if (adminId != null && userId.equals(adminId)) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Không thể thay đổi trạng thái của chính bạn");
                return;
            }
            
            // Parse request body
            Map<String, String> requestBody = jsonUtil.parseJson(request, Map.class);
            String newStatus = requestBody.get("status");
            
            if (newStatus == null || newStatus.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Trạng thái không được để trống");
                return;
            }
            
            // Validate and set new status
            try {
                User.UserStatus status = User.UserStatus.valueOf(newStatus.toLowerCase());
                user.setStatus(status);
                User updatedUser = userRepository.save(user);
                
                UserResponse userResponse = convertToUserResponse(updatedUser);
                
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "Cập nhật trạng thái người dùng thành công");
                responseData.put("data", userResponse);
                
                sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
                
            } catch (IllegalArgumentException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Trạng thái không hợp lệ. Giá trị hợp lệ: active, locked, pending");
            }
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "User ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error updating user status", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi cập nhật trạng thái người dùng");
        }
    }
    
    /**
     * Cập nhật role user
     */
    private void handleUpdateUserRole(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            // Extract user ID from path: /123/role -> 123
            String idPart = pathInfo.substring(1, pathInfo.lastIndexOf("/"));
            Long userId = Long.parseLong(idPart);
            
            // Check user exists
            User user = userRepository.findById(userId)
                .orElse(null);
            
            if (user == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy người dùng");
                return;
            }
            
            // Prevent admin from changing their own role
            Long adminId = (Long) request.getAttribute("currentUserId");
            if (adminId != null && userId.equals(adminId)) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Không thể thay đổi quyền của chính bạn");
                return;
            }
            
            // Parse request body
            Map<String, String> requestBody = jsonUtil.parseJson(request, Map.class);
            String newRole = requestBody.get("role");
            
            if (newRole == null || newRole.trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Role không được để trống");
                return;
            }
            
            // Validate and set new role
            try {
                User.UserRole role = User.UserRole.valueOf(newRole.toLowerCase());
                user.setRole(role);
                User updatedUser = userRepository.save(user);
                
                UserResponse userResponse = convertToUserResponse(updatedUser);
                
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "Cập nhật quyền người dùng thành công");
                responseData.put("data", userResponse);
                
                sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
                
            } catch (IllegalArgumentException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Role không hợp lệ. Giá trị hợp lệ: customer, admin");
            }
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "User ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error updating user role", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi cập nhật quyền người dùng");
        }
    }
    
    /**
     * Xóa user
     */
    private void handleDeleteUser(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long userId = Long.parseLong(pathInfo.substring(1));
            
            // Check user exists
            User user = userRepository.findById(userId)
                .orElse(null);
            
            if (user == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy người dùng");
                return;
            }
            
            // Prevent admin from deleting themselves
            Long adminId = (Long) request.getAttribute("currentUserId");
            if (adminId != null && userId.equals(adminId)) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Không thể xóa tài khoản của chính bạn");
                return;
            }
            
            // Soft delete: set status to inactive instead of actually deleting
            user.setStatus(User.UserStatus.locked);
            userRepository.save(user);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Xóa người dùng thành công");
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "User ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi xóa người dùng");
        }
    }
    
    /**
     * Kiểm tra quyền admin
     */
    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        // Get userId from request attribute (set by JwtAuthenticationFilter)
        Long userId = (Long) request.getAttribute("currentUserId");
        
        if (userId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - Missing user ID");
            return false;
        }
        
        try {
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - User not found");
                return false;
            }
            
            if (user.getRole() != User.UserRole.admin) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                    "Forbidden - Admin access required");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking admin role", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            return false;
        }
    }
    
    /**
     * Convert User entity to UserResponse DTO
     */
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole().toString());
        response.setStatus(user.getStatus().toString());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
    
    /**
     * Gửi JSON response
     */
    private void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) 
            throws IOException {
        
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = jsonUtil.toJson(data);
        response.getWriter().write(jsonResponse);
    }
    
    /**
     * Gửi error response
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) 
            throws IOException {
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("message", message);
        
        sendJsonResponse(response, statusCode, errorData);
    }
}
