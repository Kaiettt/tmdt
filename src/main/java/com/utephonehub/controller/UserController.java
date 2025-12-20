package com.utephonehub.controller;

import com.utephonehub.service.UserService;
import com.utephonehub.dto.request.UpdateProfileRequest;
import com.utephonehub.dto.request.ChangePasswordRequest;
import com.utephonehub.dto.response.UserResponse;
import com.utephonehub.util.JsonUtil;
import com.utephonehub.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@WebServlet("/api/v1/user/*")
public class UserController extends HttpServlet {
    private final UserService userService = new UserService();
    private final JsonUtil jsonUtil = new JsonUtil();
    private final JwtUtil jwtUtil = new JwtUtil();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        switch (pathInfo) {
            case "/me":
                handleGetCurrentUser(request, response);
                break;
            default:
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Page not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        switch (pathInfo) {
            case "/profile":
                handleUpdateProfile(request, response);
                break;
            case "/password":
                handleChangePassword(request, response);
                break;
            default:
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }



    private void handleGetCurrentUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = extractToken(request);
        String email = jwtUtil.getEmailFromToken(token);
        UserResponse user = userService.getUserByEmail(email);
        sendJsonResponse(response, HttpServletResponse.SC_OK, Map.of("success", true, "data", user));
    }

    private void handleUpdateProfile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = extractToken(request);
        String email = jwtUtil.getEmailFromToken(token);
        UpdateProfileRequest req = jsonUtil.parseJson(request, UpdateProfileRequest.class);
        UserResponse user = userService.updateProfile(email, req);
        sendJsonResponse(response, HttpServletResponse.SC_OK, Map.of("success", true, "data", user));
    }

    private void handleChangePassword(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = extractToken(request);
        String email = jwtUtil.getEmailFromToken(token);
        ChangePasswordRequest req = jsonUtil.parseJson(request, ChangePasswordRequest.class);
        userService.changePassword(email, req);
        sendJsonResponse(response, HttpServletResponse.SC_OK, Map.of("success", true, "message", "Đổi mật khẩu thành công"));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Access token required");
        }
        return authHeader.substring(7);
    }

    private void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String jsonResponse = jsonUtil.toJson(data);
        response.getWriter().write(jsonResponse);
    }

    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        String jsonResponse = jsonUtil.toJson(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
