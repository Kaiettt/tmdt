package com.utephonehub.controller;

import com.utephonehub.dto.response.OrderResponse;
import com.utephonehub.dto.response.OrderItemResponse;
import com.utephonehub.entity.Order;
import com.utephonehub.entity.User;
import com.utephonehub.repository.OrderRepository;
import com.utephonehub.repository.UserRepository;
import com.utephonehub.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Order Management Controller
 * Quản lý đơn hàng - chỉ dành cho admin
 */
@WebServlet("/api/v1/admin/orders/*")
public class AdminOrderController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(AdminOrderController.class);
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final JsonUtil jsonUtil;
    
    public AdminOrderController() {
        this.orderRepository = new OrderRepository();
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
        logger.info("AdminOrderController GET request: {}", pathInfo);
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/v1/admin/orders - Lấy danh sách orders
                handleGetOrders(request, response);
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/v1/admin/orders/{id} - Chi tiết order
                handleGetOrderDetail(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in AdminOrderController GET", e);
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
        logger.info("AdminOrderController PUT request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+/status")) {
                // PUT /api/v1/admin/orders/{id}/status - Cập nhật trạng thái order
                handleUpdateOrderStatus(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint");
            }
        } catch (Exception e) {
            logger.error("Error in AdminOrderController PUT", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * Lấy danh sách orders với filters
     */
    private void handleGetOrders(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Get query parameters
            String statusStr = request.getParameter("status");
            String userIdStr = request.getParameter("userId");
            String orderCode = request.getParameter("orderCode");
            
            List<Order> orders;
            
            // Get all orders
            orders = orderRepository.findAll();
            
            // Filter by status if provided
            if (statusStr != null && !statusStr.isEmpty()) {
                try {
                    Order.OrderStatus status = Order.OrderStatus.valueOf(statusStr.toUpperCase());
                    orders = orders.stream()
                        .filter(o -> o.getStatus() == status)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid order status: {}", statusStr);
                }
            }
            
            // Filter by userId if provided
            if (userIdStr != null && !userIdStr.isEmpty()) {
                Long userId = Long.parseLong(userIdStr);
                orders = orders.stream()
                    .filter(o -> o.getUser().getId().equals(userId))
                    .collect(Collectors.toList());
            }
            
            // Filter by orderCode if provided
            if (orderCode != null && !orderCode.isEmpty()) {
                orders = orders.stream()
                    .filter(o -> o.getOrderCode().contains(orderCode))
                    .collect(Collectors.toList());
            }
            
            // Convert to response DTOs
            List<OrderResponse> orderResponses = orders.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy danh sách đơn hàng thành công");
            responseData.put("data", orderResponses);
            responseData.put("total", orderResponses.size());
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (Exception e) {
            logger.error("Error getting orders", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy danh sách đơn hàng");
        }
    }
    
    /**
     * Lấy chi tiết order
     */
    private void handleGetOrderDetail(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long orderId = Long.parseLong(pathInfo.substring(1));
            
            Order order = orderRepository.findById(orderId)
                .orElse(null);
            
            if (order == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy đơn hàng");
                return;
            }
            
            OrderResponse orderResponse = convertToOrderResponse(order);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy thông tin đơn hàng thành công");
            responseData.put("data", orderResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Order ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error getting order detail", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy thông tin đơn hàng");
        }
    }
    
    /**
     * Cập nhật trạng thái order
     */
    private void handleUpdateOrderStatus(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            // Extract order ID from path: /123/status -> 123
            String idPart = pathInfo.substring(1, pathInfo.lastIndexOf("/"));
            Long orderId = Long.parseLong(idPart);
            
            // Check order exists
            Order order = orderRepository.findById(orderId)
                .orElse(null);
            
            if (order == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy đơn hàng");
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
                Order.OrderStatus status = Order.OrderStatus.valueOf(newStatus.toUpperCase());
                
                // Validate status transition
                if (!isValidStatusTransition(order.getStatus(), status)) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "Không thể chuyển trạng thái từ " + order.getStatus() + " sang " + status);
                    return;
                }
                
                order.setStatus(status);
                Order updatedOrder = orderRepository.save(order);
                
                // Re-fetch với relations để tránh LazyInitializationException
                updatedOrder = orderRepository.findById(updatedOrder.getId())
                    .orElseThrow(() -> new RuntimeException("Order not found after save"));
                
                OrderResponse orderResponse = convertToOrderResponse(updatedOrder);
                
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "Cập nhật trạng thái đơn hàng thành công");
                responseData.put("data", orderResponse);
                
                sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
                
            } catch (IllegalArgumentException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Trạng thái không hợp lệ. Giá trị hợp lệ: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED");
            }
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Order ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error updating order status", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi cập nhật trạng thái đơn hàng");
        }
    }
    
    /**
     * Kiểm tra chuyển trạng thái có hợp lệ không
     */
    private boolean isValidStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        // CANCELLED can't be changed
        if (currentStatus == Order.OrderStatus.CANCELLED) {
            return false;
        }
        
        // DELIVERED can only be changed to itself (no change)
        if (currentStatus == Order.OrderStatus.DELIVERED && newStatus != Order.OrderStatus.DELIVERED) {
            return false;
        }
        
        // Can always cancel (except CANCELLED and DELIVERED)
        if (newStatus == Order.OrderStatus.CANCELLED) {
            return currentStatus != Order.OrderStatus.DELIVERED;
        }
        
        // Normal flow: PENDING -> PROCESSING -> SHIPPED -> DELIVERED
        return true;
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
     * Convert Order entity to OrderResponse DTO
     */
    private OrderResponse convertToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderCode(order.getOrderCode());
        response.setUserId(order.getUser().getId());
        response.setUserEmail(order.getEmail());
        response.setFullName(order.getRecipientName());
        response.setPhoneNumber(order.getPhoneNumber());
        response.setShippingAddress(order.getStreetAddress());
        response.setShippingWard("");  // Not in Order entity
        response.setShippingDistrict("");  // Not in Order entity
        response.setShippingProvince(order.getCity());
        response.setPaymentMethod(order.getPaymentMethod().toString());
        response.setStatus(order.getStatus().toString());
        response.setTotalAmount(order.getTotalAmount());
        response.setDiscountAmount(BigDecimal.ZERO);  // Calculated field
        response.setFinalAmount(order.getTotalAmount());  // Same as totalAmount for now
        response.setVoucherCode(order.getVoucher() != null ? order.getVoucher().getCode() : null);
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        
        // Convert order items
        List<OrderItemResponse> items = order.getItems().stream()
            .map(item -> {
                OrderItemResponse itemResponse = new OrderItemResponse();
                itemResponse.setId(item.getId());
                itemResponse.setProductId(item.getProduct().getId());
                itemResponse.setProductName(item.getProduct().getName());
                itemResponse.setThumbnailUrl(item.getProduct().getThumbnailUrl());
                itemResponse.setQuantity(item.getQuantity());
                itemResponse.setPrice(item.getPrice());
                // Calculate subtotal: price * quantity
                BigDecimal subtotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
                itemResponse.setSubtotal(subtotal);
                return itemResponse;
            })
            .collect(Collectors.toList());
        
        response.setItems(items);
        
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
