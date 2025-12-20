package com.utephonehub.controller;

import com.utephonehub.util.JsonUtil;

import com.google.gson.JsonObject;
import com.utephonehub.service.CartService;
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

@WebServlet("/api/v1/cart/*")
public class CartController extends HttpServlet {
    
    private final CartService cartService;
    private final JsonUtil jsonUtil;
    
    public CartController() {
        this.cartService = new CartService();
        this.jsonUtil = new JsonUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // TODO: Get userId from JWT token
            // For now using mock userId from header
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để xem giỏ hàng");
                return;
            }
            
            Map<String, Object> cartData = cartService.getCart(userId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy thông tin giỏ hàng thành công.");
            responseData.put("data", cartData);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy thông tin giỏ hàng: " + e.getMessage());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        
        try {
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng");
                return;
            }
            
            if (pathInfo != null && pathInfo.equals("/items")) {
                // Add item to cart
                handleAddItem(request, response, userId);
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
        
        String pathInfo = request.getPathInfo();
        
        try {
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để cập nhật giỏ hàng");
                return;
            }
            
            if (pathInfo != null && pathInfo.startsWith("/items/")) {
                // Update cart item quantity
                String productIdStr = pathInfo.substring("/items/".length());
                Long productId = Long.parseLong(productIdStr);
                
                handleUpdateQuantity(request, response, userId, productId);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint không tồn tại");
            }
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID sản phẩm không hợp lệ");
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
        
        String pathInfo = request.getPathInfo();
        
        try {
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để xóa sản phẩm khỏi giỏ hàng");
                return;
            }
            
            if (pathInfo != null && pathInfo.startsWith("/items/")) {
                // Remove item from cart
                String productIdStr = pathInfo.substring("/items/".length());
                Long productId = Long.parseLong(productIdStr);
                
                handleRemoveItem(request, response, userId, productId);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint không tồn tại");
            }
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID sản phẩm không hợp lệ");
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi xử lý yêu cầu: " + e.getMessage());
        }
    }
    
    private void handleAddItem(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws IOException {
        
        // Read request body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        
        JsonObject jsonRequest = jsonUtil.fromJson(sb.toString(), JsonObject.class);
        
        if (!jsonRequest.has("productId") || !jsonRequest.has("quantity")) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "Thiếu thông tin productId hoặc quantity");
            return;
        }
        
        Long productId = jsonRequest.get("productId").getAsLong();
        Integer quantity = jsonRequest.get("quantity").getAsInt();
        
        if (quantity <= 0) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "Số lượng phải lớn hơn 0");
            return;
        }
        
        try {
            Map<String, Object> cartData = cartService.addItemToCart(userId, productId, quantity);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Thêm sản phẩm vào giỏ hàng thành công.");
            responseData.put("data", cartData);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy sản phẩm")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("vượt quá số lượng tồn kho")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    private void handleUpdateQuantity(HttpServletRequest request, HttpServletResponse response, 
                                     Long userId, Long productId) throws IOException {
        
        // Read request body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        
        JsonObject jsonRequest = jsonUtil.fromJson(sb.toString(), JsonObject.class);
        
        if (!jsonRequest.has("quantity")) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Thiếu thông tin quantity");
            return;
        }
        
        Integer quantity = jsonRequest.get("quantity").getAsInt();
        
        if (quantity <= 0) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "Số lượng phải lớn hơn 0");
            return;
        }
        
        try {
            Map<String, Object> cartData = cartService.updateCartItemQuantity(userId, productId, quantity);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Cập nhật số lượng sản phẩm thành công.");
            responseData.put("data", cartData);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("vượt quá số lượng tồn kho")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    private void handleRemoveItem(HttpServletRequest request, HttpServletResponse response, 
                                 Long userId, Long productId) throws IOException {
        
        try {
            Map<String, Object> cartData = cartService.removeItemFromCart(userId, productId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Xóa sản phẩm khỏi giỏ hàng thành công.");
            responseData.put("data", cartData);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
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
    
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) 
            throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        
        response.setStatus(statusCode);
        response.getWriter().write(jsonUtil.toJson(errorResponse));
    }
}

