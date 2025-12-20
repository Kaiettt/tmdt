package com.utephonehub.controller;

import com.utephonehub.service.OrderService;
import com.utephonehub.util.JwtUtil;
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
 * Controller để xử lý views của order (JSP pages)
 */
@WebServlet(urlPatterns = {"/orders/*", "/order-lookup"})
public class OrderViewController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(OrderViewController.class);
    private final OrderService orderService;
    private final JwtUtil jwtUtil;
    
    public OrderViewController() {
        this.orderService = new OrderService();
        this.jwtUtil = new JwtUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        try {
            // GET /orders/{orderId} - Order detail or success page
            if (pathInfo != null && !pathInfo.equals("/")) {
                handleOrderDetail(request, response);
            }
            // GET /order-lookup - Order lookup page
            else if (request.getRequestURI().endsWith("/order-lookup")) {
                request.getRequestDispatcher("/WEB-INF/views/order/lookup.jsp")
                       .forward(request, response);
            }
            else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            
        } catch (Exception e) {
            logger.error("Error handling order view request", e);
            request.setAttribute("error", "Không thể tải thông tin đơn hàng");
            request.getRequestDispatcher("/WEB-INF/views/error/500.jsp")
                   .forward(request, response);
        }
    }
    
    private void handleOrderDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Extract order ID from path
        String pathInfo = request.getPathInfo(); // /123
        String orderIdStr = pathInfo.substring(1); // 123
        
        try {
            Long orderId = Long.parseLong(orderIdStr);
            
            // Get user ID from JWT token (if logged in)
            Long userId = getUserIdFromToken(request);
            
            // Load order details
            Map<String, Object> order = orderService.getOrderDetail(orderId, userId);
            
            request.setAttribute("order", order);
            
            // Check if this is a success redirect (just placed order)
            String success = request.getParameter("success");
            if ("true".equals(success)) {
                // Show success page
                request.getRequestDispatcher("/WEB-INF/views/order/success.jsp")
                       .forward(request, response);
            } else {
                // Show order detail page
                request.getRequestDispatcher("/WEB-INF/views/order/detail.jsp")
                       .forward(request, response);
            }
            
        } catch (NumberFormatException e) {
            logger.error("Invalid order ID: {}", orderIdStr);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Order ID không hợp lệ");
        } catch (RuntimeException e) {
            logger.error("Error loading order details", e);
            
            // If order not found or access denied
            if (e.getMessage().contains("không tìm thấy") || e.getMessage().contains("không có quyền")) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    /**
     * Get user ID from JWT token
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.getUserIdFromToken(token);
            } catch (Exception e) {
                logger.debug("Could not extract user ID from token", e);
            }
        }
        
        return null;
    }
}

