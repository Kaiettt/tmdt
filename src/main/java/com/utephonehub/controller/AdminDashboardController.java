package com.utephonehub.controller;

import com.utephonehub.entity.Order;
import com.utephonehub.entity.User;
import com.utephonehub.repository.OrderRepository;
import com.utephonehub.repository.ProductRepository;
import com.utephonehub.repository.UserRepository;
import com.utephonehub.service.DashboardService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Dashboard Controller
 * Thống kê dữ liệu cho admin dashboard
 */
@WebServlet("/api/v1/admin/dashboard/*")
public class AdminDashboardController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(AdminDashboardController.class);
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final JsonUtil jsonUtil;
    
    public AdminDashboardController() {
        this.orderRepository = new OrderRepository();
        this.productRepository = new ProductRepository();
        this.userRepository = new UserRepository();
        this.dashboardService = new DashboardService();
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
        logger.info("AdminDashboardController GET request: {}", pathInfo);
        
        try {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/summary")) {
                // GET /api/v1/admin/dashboard/summary - Dashboard summary statistics
                handleGetDashboardSummary(request, response);
            } else if (pathInfo.equals("/stats")) {
                // GET /api/v1/admin/dashboard/stats - New stats using DashboardService
                handleGetDashboardStats(request, response);
            } else if (pathInfo.equals("/recent-orders")) {
                // GET /api/v1/admin/dashboard/recent-orders
                handleGetRecentOrders(request, response);
            } else if (pathInfo.equals("/low-stock")) {
                // GET /api/v1/admin/dashboard/low-stock
                handleGetLowStockProducts(request, response);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in AdminDashboardController GET", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * Get dashboard stats using DashboardService (NEW)
     */
    private void handleGetDashboardStats(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        try {
            // Parse date range (optional)
            String startDateStr = request.getParameter("startDate");
            String endDateStr = request.getParameter("endDate");
            
            LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : LocalDate.now().minusDays(30);
            LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : LocalDate.now();
            
            // Get stats from service
            Map<String, Object> stats = dashboardService.getDashboardStats(startDate, endDate);
            
            sendSuccessResponse(response, "Lấy thống kê dashboard thành công", stats);
            
        } catch (Exception e) {
            logger.error("Error getting dashboard stats", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy thống kê dashboard");
        }
    }
    
    /**
     * Get recent orders (NEW)
     */
    private void handleGetRecentOrders(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        try {
            int limit = request.getParameter("limit") != null ? 
                Integer.parseInt(request.getParameter("limit")) : 10;
            
            List<Map<String, Object>> recentOrders = dashboardService.getRecentOrders(limit);
            
            sendSuccessResponse(response, "Lấy đơn hàng gần đây thành công", recentOrders);
            
        } catch (Exception e) {
            logger.error("Error getting recent orders", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy đơn hàng gần đây");
        }
    }
    
    /**
     * Get low stock products (NEW)
     */
    private void handleGetLowStockProducts(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        try {
            int threshold = request.getParameter("threshold") != null ? 
                Integer.parseInt(request.getParameter("threshold")) : 10;
            int limit = request.getParameter("limit") != null ? 
                Integer.parseInt(request.getParameter("limit")) : 10;
            
            List<Map<String, Object>> lowStockProducts = 
                dashboardService.getLowStockProducts(threshold, limit);
            
            sendSuccessResponse(response, "Lấy sản phẩm sắp hết hàng thành công", lowStockProducts);
            
        } catch (Exception e) {
            logger.error("Error getting low stock products", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy sản phẩm sắp hết hàng");
        }
    }
    
    /**
     * Lấy thống kê tổng quan dashboard
     */
    private void handleGetDashboardSummary(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Get all data
            List<Order> allOrders = orderRepository.findAll();
            List<User> allUsers = userRepository.findAll();
            long totalProducts = productRepository.count();
            
            // Calculate time ranges
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfToday = now.truncatedTo(ChronoUnit.DAYS);
            LocalDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            LocalDateTime startOfYear = now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
            
            // Orders statistics
            Map<String, Object> ordersStats = new HashMap<>();
            ordersStats.put("total", allOrders.size());
            ordersStats.put("pending", countOrdersByStatus(allOrders, Order.OrderStatus.PENDING));
            ordersStats.put("processing", countOrdersByStatus(allOrders, Order.OrderStatus.PROCESSING));
            ordersStats.put("shipped", countOrdersByStatus(allOrders, Order.OrderStatus.SHIPPED));
            ordersStats.put("delivered", countOrdersByStatus(allOrders, Order.OrderStatus.DELIVERED));
            ordersStats.put("cancelled", countOrdersByStatus(allOrders, Order.OrderStatus.CANCELLED));
            ordersStats.put("today", countOrdersSince(allOrders, startOfToday));
            ordersStats.put("thisMonth", countOrdersSince(allOrders, startOfMonth));
            ordersStats.put("thisYear", countOrdersSince(allOrders, startOfYear));
            
            // Revenue statistics
            Map<String, Object> revenueStats = new HashMap<>();
            BigDecimal totalRevenue = calculateTotalRevenue(allOrders);
            BigDecimal todayRevenue = calculateRevenueSince(allOrders, startOfToday);
            BigDecimal monthRevenue = calculateRevenueSince(allOrders, startOfMonth);
            BigDecimal yearRevenue = calculateRevenueSince(allOrders, startOfYear);
            
            // Last 7 days revenue
            LocalDateTime last7Days = now.minusDays(7);
            BigDecimal last7DaysRevenue = calculateRevenueSince(allOrders, last7Days);
            
            revenueStats.put("total", totalRevenue);
            revenueStats.put("today", todayRevenue);
            revenueStats.put("thisMonth", monthRevenue);
            revenueStats.put("thisYear", yearRevenue);
            revenueStats.put("last7Days", last7DaysRevenue);
            revenueStats.put("averageOrderValue", 
                allOrders.isEmpty() ? BigDecimal.ZERO : totalRevenue.divide(new BigDecimal(allOrders.size()), 2, java.math.RoundingMode.HALF_UP));
            
            // Users statistics
            Map<String, Object> usersStats = new HashMap<>();
            usersStats.put("total", allUsers.size());
            usersStats.put("customers", countUsersByRole(allUsers, User.UserRole.customer));
            usersStats.put("admins", countUsersByRole(allUsers, User.UserRole.admin));
            usersStats.put("active", countUsersByStatus(allUsers, User.UserStatus.active));
            usersStats.put("locked", countUsersByStatus(allUsers, User.UserStatus.locked));
            usersStats.put("pending", countUsersByStatus(allUsers, User.UserStatus.pending));
            usersStats.put("newToday", countUsersSince(allUsers, startOfToday));
            usersStats.put("newThisMonth", countUsersSince(allUsers, startOfMonth));
            usersStats.put("newThisYear", countUsersSince(allUsers, startOfYear));
            
            // Products statistics
            Map<String, Object> productsStats = new HashMap<>();
            productsStats.put("total", totalProducts);
            productsStats.put("inStock", productRepository.findInStock().size());
            productsStats.put("lowStock", productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() > 0 && p.getStockQuantity() < 10)
                .count());
            productsStats.put("outOfStock", productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() == 0)
                .count());
            
            // Recent orders (last 10)
            List<Map<String, Object>> recentOrders = allOrders.stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(10)
                .map(order -> {
                    Map<String, Object> orderData = new HashMap<>();
                    orderData.put("id", order.getId());
                    orderData.put("orderCode", order.getOrderCode());
                    orderData.put("customerName", order.getRecipientName());
                    orderData.put("totalAmount", order.getTotalAmount());
                    orderData.put("status", order.getStatus().toString());
                    orderData.put("createdAt", order.getCreatedAt());
                    return orderData;
                })
                .collect(Collectors.toList());
            
            // Top selling products (by order items count)
            Map<Long, Integer> productSales = new HashMap<>();
            allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
                .forEach(order -> 
                    order.getItems().forEach(item -> {
                        Long productId = item.getProduct().getId();
                        productSales.put(productId, 
                            productSales.getOrDefault(productId, 0) + item.getQuantity());
                    })
                );
            
            List<Map<String, Object>> topProducts = productSales.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(entry -> {
                    var product = productRepository.findById(entry.getKey()).orElse(null);
                    if (product == null) return null;
                    
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("id", product.getId());
                    productData.put("name", product.getName());
                    productData.put("soldQuantity", entry.getValue());
                    productData.put("thumbnailUrl", product.getThumbnailUrl());
                    productData.put("price", product.getPrice());
                    return productData;
                })
                .filter(p -> p != null)
                .collect(Collectors.toList());
            
            // Build response
            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("orders", ordersStats);
            dashboardData.put("revenue", revenueStats);
            dashboardData.put("users", usersStats);
            dashboardData.put("products", productsStats);
            dashboardData.put("recentOrders", recentOrders);
            dashboardData.put("topProducts", topProducts);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy thống kê dashboard thành công");
            responseData.put("data", dashboardData);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (Exception e) {
            logger.error("Error getting dashboard summary", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy thống kê dashboard");
        }
    }
    
    /**
     * Đếm số orders theo status
     */
    private long countOrdersByStatus(List<Order> orders, Order.OrderStatus status) {
        return orders.stream()
            .filter(o -> o.getStatus() == status)
            .count();
    }
    
    /**
     * Đếm số orders từ một thời điểm
     */
    private long countOrdersSince(List<Order> orders, LocalDateTime since) {
        return orders.stream()
            .filter(o -> o.getCreatedAt().isAfter(since))
            .count();
    }
    
    /**
     * Tính tổng doanh thu
     */
    private BigDecimal calculateTotalRevenue(List<Order> orders) {
        return orders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Tính doanh thu từ một thời điểm
     */
    private BigDecimal calculateRevenueSince(List<Order> orders, LocalDateTime since) {
        return orders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
            .filter(o -> o.getCreatedAt().isAfter(since))
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Đếm số users theo role
     */
    private long countUsersByRole(List<User> users, User.UserRole role) {
        return users.stream()
            .filter(u -> u.getRole() == role)
            .count();
    }
    
    /**
     * Đếm số users theo status
     */
    private long countUsersByStatus(List<User> users, User.UserStatus status) {
        return users.stream()
            .filter(u -> u.getStatus() == status)
            .count();
    }
    
    /**
     * Đếm số users mới từ một thời điểm
     */
    private long countUsersSince(List<User> users, LocalDateTime since) {
        return users.stream()
            .filter(u -> u.getCreatedAt().isAfter(since))
            .count();
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
    private void sendSuccessResponse(HttpServletResponse response, String message, Object data) 
            throws IOException {

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", message);
        responseData.put("data", data);

        sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
    }
}
