package com.utephonehub.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.utephonehub.dto.request.PaymentInfoRequest;
import com.utephonehub.service.OrderService;
import com.utephonehub.util.JsonUtil;
import com.utephonehub.util.RequestUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/api/v1/checkout", "/api/v1/orders", "/api/v1/orders/*"})
public class OrderController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(OrderController.class);
    private final OrderService orderService;
    private final JsonUtil jsonUtil;
    
    public OrderController() {
        this.orderService = new OrderService();
        this.jsonUtil = new JsonUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String requestURI = request.getRequestURI();
        String pathInfo = request.getPathInfo();
        
        try {
            // GET /api/v1/orders - Get user's orders
            if (requestURI.endsWith("/orders") || (pathInfo != null && pathInfo.equals("/"))) {
                handleGetUserOrders(request, response);
            }
            // GET /api/v1/orders/{orderId} - Get order detail
            else if (pathInfo != null && !pathInfo.equals("/")) {
                handleGetOrderDetail(request, response);
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String requestURI = request.getRequestURI();
        
        try {
            // POST /api/v1/checkout - Create order
            if (requestURI.endsWith("/checkout")) {
                handleCheckout(request, response);
            }
            // POST /api/v1/orders/lookup - Lookup order
            else if (requestURI.endsWith("/orders/lookup")) {
                handleLookupOrder(request, response);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint không tồn tại");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi xử lý yêu cầu: " + e.getMessage());
        }
    }
    
    private void handleCheckout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        // Read request body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        
        String jsonString = sb.toString();
        logger.debug("Checkout request body: {}", jsonString);
        
        JsonObject jsonRequest = jsonUtil.fromJson(jsonString, JsonObject.class);
        
        // Validate required fields
        if (!jsonRequest.has("shippingInfo") || jsonRequest.get("shippingInfo").isJsonNull() || 
            !jsonRequest.has("paymentMethod") || jsonRequest.get("paymentMethod").isJsonNull()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "Thiếu thông tin bắt buộc");
            return;
        }
        
        // Parse shipping info
        JsonObject shippingInfoJson = jsonRequest.getAsJsonObject("shippingInfo");
        Map<String, Object> shippingInfo = new HashMap<>();
        
        // Check if using saved address (addressId) or new address (form fields)
        if (shippingInfoJson.has("addressId") && !shippingInfoJson.get("addressId").isJsonNull()) {
            shippingInfo.put("addressId", shippingInfoJson.get("addressId").getAsLong());
        } else {
            // New address - validate and extract fields
            if (shippingInfoJson.has("recipientName") && !shippingInfoJson.get("recipientName").isJsonNull()) {
                shippingInfo.put("recipientName", shippingInfoJson.get("recipientName").getAsString());
            }
            if (shippingInfoJson.has("phoneNumber") && !shippingInfoJson.get("phoneNumber").isJsonNull()) {
                shippingInfo.put("phoneNumber", shippingInfoJson.get("phoneNumber").getAsString());
            }
            if (shippingInfoJson.has("email") && !shippingInfoJson.get("email").isJsonNull()) {
                shippingInfo.put("email", shippingInfoJson.get("email").getAsString());
            }
            if (shippingInfoJson.has("streetAddress") && !shippingInfoJson.get("streetAddress").isJsonNull()) {
                shippingInfo.put("streetAddress", shippingInfoJson.get("streetAddress").getAsString());
            }
            // Support both 'city' and 'province' field names
            if (shippingInfoJson.has("city") && !shippingInfoJson.get("city").isJsonNull()) {
                shippingInfo.put("city", shippingInfoJson.get("city").getAsString());
            } else if (shippingInfoJson.has("province") && !shippingInfoJson.get("province").isJsonNull()) {
                shippingInfo.put("city", shippingInfoJson.get("province").getAsString());
            }
            // Ward info (no longer using district)
            if (shippingInfoJson.has("ward") && !shippingInfoJson.get("ward").isJsonNull()) {
                shippingInfo.put("ward", shippingInfoJson.get("ward").getAsString());
            }
            if (shippingInfoJson.has("notes") && !shippingInfoJson.get("notes").isJsonNull()) {
                shippingInfo.put("notes", shippingInfoJson.get("notes").getAsString());
            }
        }
        
        // Handle voucherCode - check for null
        String voucherCode = null;
        if (jsonRequest.has("voucherCode") && !jsonRequest.get("voucherCode").isJsonNull()) {
            voucherCode = jsonRequest.get("voucherCode").getAsString();
            // Also check for empty string
            if (voucherCode != null && voucherCode.trim().isEmpty()) {
                voucherCode = null;
            }
        }
        
        String paymentMethod = jsonRequest.get("paymentMethod").getAsString();
        
        // Parse payment info (optional, only for BANK_TRANSFER)
        PaymentInfoRequest paymentInfo = null;
        if (jsonRequest.has("paymentInfo") && !jsonRequest.get("paymentInfo").isJsonNull()) {
            JsonObject paymentInfoJson = jsonRequest.getAsJsonObject("paymentInfo");
            paymentInfo = new PaymentInfoRequest(
                paymentInfoJson.has("cardNumber") && !paymentInfoJson.get("cardNumber").isJsonNull() ? 
                    paymentInfoJson.get("cardNumber").getAsString() : null,
                paymentInfoJson.has("cardHolder") && !paymentInfoJson.get("cardHolder").isJsonNull() ? 
                    paymentInfoJson.get("cardHolder").getAsString() : null,
                paymentInfoJson.has("expiryDate") && !paymentInfoJson.get("expiryDate").isJsonNull() ? 
                    paymentInfoJson.get("expiryDate").getAsString() : null,
                paymentInfoJson.has("cvv") && !paymentInfoJson.get("cvv").isJsonNull() ? 
                    paymentInfoJson.get("cvv").getAsString() : null
            );
        }
        
        // Get userId (null for guest checkout)
        Long userId = getUserIdFromRequest(request);
        
        try {
            Map<String, Object> orderData = orderService.checkout(
                userId, shippingInfo, voucherCode, paymentMethod, paymentInfo);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Đặt hàng thành công.");
            responseData.put("data", orderData);
            
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("trống") || 
                e.getMessage().contains("không hợp lệ") ||
                e.getMessage().contains("không tồn tại") ||
                e.getMessage().contains("chưa đủ") ||
                e.getMessage().contains("cung cấp thông tin")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    private void handleGetUserOrders(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        Long userId = getUserIdFromRequest(request);
        
        if (userId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                "Vui lòng đăng nhập để xem lịch sử đơn hàng");
            return;
        }
        
        // Parse pagination
        int page = parseInteger(request.getParameter("page"), 1);
        int limit = parseInteger(request.getParameter("limit"), 10);
        
        List<Map<String, Object>> orders = orderService.getUserOrders(userId, page, limit);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", "Lấy danh sách đơn hàng thành công.");
        responseData.put("data", orders);
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(jsonUtil.toJson(responseData));
    }
    
    private void handleGetOrderDetail(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        Long userId = getUserIdFromRequest(request);
        
        if (userId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                "Vui lòng đăng nhập để xem chi tiết đơn hàng");
            return;
        }
        
        String pathInfo = request.getPathInfo();
        String orderIdStr = pathInfo.substring(1); // Remove leading /
        
        try {
            Long orderId = Long.parseLong(orderIdStr);
            
            Map<String, Object> orderData = orderService.getOrderDetail(orderId, userId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy chi tiết đơn hàng thành công.");
            responseData.put("data", orderData);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID đơn hàng không hợp lệ");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("không có quyền")) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    private void handleLookupOrder(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        // Read request body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        
        JsonObject jsonRequest = jsonUtil.fromJson(sb.toString(), JsonObject.class);
        
        if (!jsonRequest.has("orderCode") || !jsonRequest.has("email")) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "Thiếu thông tin orderCode hoặc email");
            return;
        }
        
        String orderCode = jsonRequest.get("orderCode").getAsString();
        String email = jsonRequest.get("email").getAsString();
        
        try {
            Map<String, Object> orderData = orderService.lookupOrder(orderCode, email);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Tra cứu đơn hàng thành công.");
            responseData.put("data", orderData);
            
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
    /**
 * Gửi success response
 */
    private void sendSuccessResponse(HttpServletResponse response, String message, Object data) 
            throws IOException {

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", message);
        responseData.put("data", data);

        sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
    }
    private void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) 
            throws IOException {
        
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = jsonUtil.toJson(data);
        response.getWriter().write(jsonResponse);
    }
    
}

