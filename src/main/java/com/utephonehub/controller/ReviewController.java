package com.utephonehub.controller;

import com.google.gson.JsonObject;
import com.utephonehub.service.ReviewService;
import com.utephonehub.util.JsonUtil;
import com.utephonehub.util.RequestUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/v1/reviews/*"})
public class ReviewController extends HttpServlet {
    
    private final ReviewService reviewService;
    private final JsonUtil jsonUtil;
    
    public ReviewController() {
        this.reviewService = new ReviewService();
        this.jsonUtil = new JsonUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // GET /api/v1/reviews?productId={id}
            String productIdParam = request.getParameter("productId");
            if (productIdParam != null && !productIdParam.isEmpty()) {
                handleGetProductReviews(request, response);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Thiếu productId");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi xử lý yêu cầu: " + e.getMessage());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo(); // e.g., null or "/{id}/like"
        
        try {
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để thực hiện chức năng này");
                return;
            }
            
            // POST /api/v1/reviews?productId={id} - Create review
            String productIdParam = request.getParameter("productId");
            if (productIdParam != null && !productIdParam.isEmpty() && 
                (pathInfo == null || pathInfo.equals("/"))) {
                handleCreateReview(request, response, userId);
            }
            // POST /api/v1/reviews/{reviewId}/like - Like review
            else if (pathInfo != null && pathInfo.endsWith("/like")) {
                handleLikeReview(request, response, userId);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint không tồn tại");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi xử lý yêu cầu: " + e.getMessage());
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo(); // e.g., "/{id}"
        
        try {
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để thực hiện chức năng này");
                return;
            }
            
            // PUT /api/v1/reviews/{reviewId} - Update review
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                handleUpdateReview(request, response, userId, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint không tồn tại");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi xử lý yêu cầu: " + e.getMessage());
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo(); // e.g., "/{id}" or "/{id}/like"
        
        try {
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để thực hiện chức năng này");
                return;
            }
            
            // DELETE /api/v1/reviews/{reviewId}/like - Unlike review
            if (pathInfo != null && pathInfo.endsWith("/like")) {
                handleUnlikeReview(request, response, userId);
            }
            // DELETE /api/v1/reviews/{reviewId} - Delete review
            else if (pathInfo != null && pathInfo.matches("/\\d+")) {
                handleDeleteReview(request, response, userId, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint không tồn tại");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi xử lý yêu cầu: " + e.getMessage());
        }
    }
    
    private void handleGetProductReviews(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        // Get productId from query parameter
        String productIdParam = request.getParameter("productId");
        Long productId;
        
        try {
            productId = Long.parseLong(productIdParam);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID sản phẩm không hợp lệ");
            return;
        }
        
        // Parse pagination parameters
        int page = parseInteger(request.getParameter("page"), 1);
        int limit = parseInteger(request.getParameter("limit"), 5);
        
        try {
            Map<String, Object> result = reviewService.getProductReviews(productId, page, limit);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy danh sách đánh giá thành công.");
            responseData.put("data", result.get("reviews"));
            responseData.put("metadata", result.get("metadata"));
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy sản phẩm")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    private void handleCreateReview(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws IOException {
        
        // Get productId from query parameter
        String productIdParam = request.getParameter("productId");
        Long productId;
        
        try {
            productId = Long.parseLong(productIdParam);
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID sản phẩm không hợp lệ");
            return;
        }
        
        // Read request body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        
        JsonObject jsonRequest = jsonUtil.fromJson(sb.toString(), JsonObject.class);
        
        if (!jsonRequest.has("rating")) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Thiếu thông tin rating");
            return;
        }
        
        Integer rating = jsonRequest.get("rating").getAsInt();
        String comment = jsonRequest.has("comment") ? jsonRequest.get("comment").getAsString() : null;
        
        try {
            Map<String, Object> reviewData = reviewService.createReview(userId, productId, rating, comment);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Gửi đánh giá thành công.");
            responseData.put("data", reviewData);
            
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy sản phẩm")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("đã đánh giá")) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            } else if (e.getMessage().contains("phải từ 1 đến 5")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    private void handleLikeReview(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws IOException {
        
        String requestURI = request.getRequestURI();
        
        // Extract reviewId from URL: /api/v1/reviews/{reviewId}/like
        String[] parts = requestURI.split("/");
        Long reviewId = null;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("reviews") && i + 1 < parts.length) {
                try {
                    reviewId = Long.parseLong(parts[i + 1]);
                    break;
                } catch (NumberFormatException e) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID đánh giá không hợp lệ");
                    return;
                }
            }
        }
        
        if (reviewId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Thiếu ID đánh giá");
            return;
        }
        
        try {
            Map<String, Object> result = reviewService.likeReview(userId, reviewId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Thích đánh giá thành công.");
            responseData.put("data", result);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy đánh giá")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("đã thích")) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    private void handleUnlikeReview(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws IOException {
        
        String requestURI = request.getRequestURI();
        
        // Extract reviewId from URL
        String[] parts = requestURI.split("/");
        Long reviewId = null;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("reviews") && i + 1 < parts.length) {
                try {
                    reviewId = Long.parseLong(parts[i + 1]);
                    break;
                } catch (NumberFormatException e) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID đánh giá không hợp lệ");
                    return;
                }
            }
        }
        
        if (reviewId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Thiếu ID đánh giá");
            return;
        }
        
        try {
            Map<String, Object> result = reviewService.unlikeReview(userId, reviewId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Bỏ thích đánh giá thành công.");
            responseData.put("data", result);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy đánh giá")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("chưa thích")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    private void handleUpdateReview(HttpServletRequest request, HttpServletResponse response, 
                                    Long userId, String pathInfo) throws IOException {
        try {
            // Extract reviewId from path
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID đánh giá không hợp lệ");
                return;
            }
            
            Long reviewId = Long.parseLong(pathParts[1]);
            
            // Read request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject jsonRequest = jsonUtil.fromJson(sb.toString(), JsonObject.class);
            
            if (!jsonRequest.has("rating")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Thiếu thông tin rating");
                return;
            }
            
            Integer rating = jsonRequest.get("rating").getAsInt();
            String comment = jsonRequest.has("comment") ? jsonRequest.get("comment").getAsString() : null;
            
            Map<String, Object> reviewData = reviewService.updateReview(userId, reviewId, rating, comment);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Cập nhật đánh giá thành công.");
            responseData.put("data", reviewData);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID đánh giá không hợp lệ");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy đánh giá")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("không có quyền")) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            } else if (e.getMessage().contains("phải từ 1 đến 5")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    private void handleDeleteReview(HttpServletRequest request, HttpServletResponse response,
                                    Long userId, String pathInfo) throws IOException {
        try {
            // Extract reviewId from path
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID đánh giá không hợp lệ");
                return;
            }
            
            Long reviewId = Long.parseLong(pathParts[1]);
            
            reviewService.deleteReview(userId, reviewId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Xóa đánh giá thành công.");
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID đánh giá không hợp lệ");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy đánh giá")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("không có quyền")) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    /**
     * Get user ID from JWT token (via request attribute)
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        return RequestUtil.getCurrentUserId(request);
    }
    
    private int parseInteger(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) 
            throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        
        response.setStatus(statusCode);
        response.getWriter().write(jsonUtil.toJson(errorResponse));
    }
}

