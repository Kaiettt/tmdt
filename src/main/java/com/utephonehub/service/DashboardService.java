package com.utephonehub.service;

import com.utephonehub.entity.Order;
import com.utephonehub.entity.Product;
import com.utephonehub.repository.OrderRepository;
import com.utephonehub.repository.ProductRepository;
import com.utephonehub.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Dashboard Service
 * Cung cấp thống kê cho Admin Dashboard
 */
public class DashboardService {
    
    private static final Logger logger = LogManager.getLogger(DashboardService.class);
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    public DashboardService() {
        this.orderRepository = new OrderRepository();
        this.productRepository = new ProductRepository();
        this.userRepository = new UserRepository();
    }
    
    /**
     * Get comprehensive dashboard statistics
     */
    public Map<String, Object> getDashboardStats(LocalDate startDate, LocalDate endDate) {
        logger.info("Getting dashboard stats from {} to {}", startDate, endDate);
        
        Map<String, Object> stats = new HashMap<>();
        
        // Total revenue
        BigDecimal totalRevenue = orderRepository.getTotalRevenueByDateRange(startDate, endDate);
        BigDecimal previousPeriodRevenue = getPreviousPeriodRevenue(startDate, endDate);
        double revenueChange = calculatePercentageChange(totalRevenue, previousPeriodRevenue);
        
        stats.put("totalRevenue", totalRevenue);
        stats.put("revenueChange", revenueChange);
        
        // Total orders
        long totalOrders = orderRepository.countOrdersByDateRange(startDate, endDate);
        long previousPeriodOrders = orderRepository.countOrdersByDateRange(
            startDate.minusDays(endDate.toEpochDay() - startDate.toEpochDay()),
            startDate.minusDays(1)
        );
        double ordersChange = calculatePercentageChange(totalOrders, previousPeriodOrders);
        
        stats.put("totalOrders", totalOrders);
        stats.put("ordersChange", ordersChange);
        
        // Total products
        long totalProducts = productRepository.countActiveProducts();
        long newProducts = productRepository.countNewProducts(startDate, endDate);
        
        stats.put("totalProducts", totalProducts);
        stats.put("productsChange", newProducts + " mới");
        
        // Total users
        long totalUsers = userRepository.countActiveUsers();
        long newUsers = userRepository.countNewUsers(startDate, endDate);
        double usersChange = calculatePercentageChange(newUsers, totalUsers - newUsers);
        
        stats.put("totalUsers", totalUsers);
        stats.put("usersChange", usersChange);
        
        // Orders by status
        Map<String, Long> ordersByStatus = orderRepository.countOrdersByStatus();
        stats.put("ordersByStatus", ordersByStatus);
        
        logger.info("Dashboard stats retrieved successfully");
        return stats;
    }
    
    /**
     * Get recent orders
     */
    public List<Map<String, Object>> getRecentOrders(int limit) {
        logger.info("Getting {} recent orders", limit);
        
        List<Order> orders = orderRepository.findRecentOrders(limit);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Order order : orders) {
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("id", order.getId());
            orderData.put("orderCode", order.getOrderCode());
            orderData.put("recipientName", order.getRecipientName());
            orderData.put("totalAmount", order.getTotalAmount());
            orderData.put("status", order.getStatus().name());
            orderData.put("paymentMethod", order.getPaymentMethod().name());
            orderData.put("createdAt", order.getCreatedAt());
            orderData.put("itemCount", order.getItems().size());
            
            result.add(orderData);
        }
        
        return result;
    }
    
    /**
     * Get low stock products
     */
    public List<Map<String, Object>> getLowStockProducts(int threshold, int limit) {
        logger.info("Getting low stock products (threshold: {}, limit: {})", threshold, limit);
        
        List<Product> products = productRepository.findLowStockProducts(threshold, limit);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Product product : products) {
            Map<String, Object> productData = new HashMap<>();
            productData.put("id", product.getId());
            productData.put("name", product.getName());
            productData.put("stockQuantity", product.getStockQuantity());
            productData.put("price", product.getPrice());
            productData.put("thumbnailUrl", product.getThumbnailUrl());
            
            if (product.getCategory() != null) {
                productData.put("categoryName", product.getCategory().getName());
            }
            
            if (product.getBrand() != null) {
                productData.put("brandName", product.getBrand().getName());
            }
            
            result.add(productData);
        }
        
        return result;
    }
    
    /**
     * Get revenue for previous period
     */
    private BigDecimal getPreviousPeriodRevenue(LocalDate startDate, LocalDate endDate) {
        long daysDiff = endDate.toEpochDay() - startDate.toEpochDay();
        LocalDate prevStart = startDate.minusDays(daysDiff);
        LocalDate prevEnd = startDate.minusDays(1);
        return orderRepository.getTotalRevenueByDateRange(prevStart, prevEnd);
    }
    
    /**
     * Calculate percentage change
     */
    private double calculatePercentageChange(Number current, Number previous) {
        if (previous == null || previous.doubleValue() == 0) {
            return 0.0;
        }
        
        double diff = current.doubleValue() - previous.doubleValue();
        return (diff / previous.doubleValue()) * 100;
    }
}

