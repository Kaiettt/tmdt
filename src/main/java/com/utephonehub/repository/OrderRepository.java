package com.utephonehub.repository;

import com.utephonehub.config.DatabaseConfig;
import com.utephonehub.entity.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrderRepository {
    
    public Order save(Order order) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (order.getId() == null) {
                em.persist(order);
            } else {
                order = em.merge(order);
            }
            tx.commit();
            em.close(); // Close AFTER commit
            return order;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close(); // Close AFTER rollback
            throw e;
        }
    }
    
    public Optional<Order> findById(Long orderId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Order order = em.createQuery(
                "SELECT o FROM Order o " +
                "LEFT JOIN FETCH o.items oi " +
                "LEFT JOIN FETCH oi.product p " +
                "LEFT JOIN FETCH p.category " +
                "LEFT JOIN FETCH p.brand " +
                "LEFT JOIN FETCH o.voucher " +
                "WHERE o.id = :orderId", Order.class)
                .setParameter("orderId", orderId)
                .getSingleResult();
            return Optional.of(order);
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public Optional<Order> findByOrderCodeAndEmail(String orderCode, String email) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Order order = em.createQuery(
                "SELECT o FROM Order o " +
                "LEFT JOIN FETCH o.items oi " +
                "LEFT JOIN FETCH oi.product p " +
                "LEFT JOIN FETCH p.category " +
                "LEFT JOIN FETCH p.brand " +
                "LEFT JOIN FETCH o.voucher " +
                "WHERE o.orderCode = :orderCode AND o.email = :email", Order.class)
                .setParameter("orderCode", orderCode)
                .setParameter("email", email)
                .getSingleResult();
            return Optional.of(order);
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public List<Order> findByUserId(Long userId, int page, int limit) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return em.createQuery(
                "SELECT o FROM Order o " +
                "WHERE o.user.id = :userId " +
                "ORDER BY o.createdAt DESC", Order.class)
                .setParameter("userId", userId)
                .setFirstResult((page - 1) * limit)
                .setMaxResults(limit)
                .getResultList();
        } finally {
            em.close();
        }
    }
    
    public long countByUserId(Long userId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return em.createQuery(
                "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        } finally {
            em.close();
        }
    }
    
    public String generateOrderCode() {
        // Format: UTEHUB-timestamp
        long timestamp = System.currentTimeMillis() / 1000;
        return "UTEHUB-" + timestamp;
    }
    
    public List<Order> findAll() {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            // First fetch orders with user, items, and voucher
            List<Order> orders = em.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                "LEFT JOIN FETCH o.user " +
                "LEFT JOIN FETCH o.items " +
                "LEFT JOIN FETCH o.voucher " +
                "ORDER BY o.createdAt DESC", Order.class)
                .getResultList();
            
            // Second query to fetch products for items (avoid cartesian product)
            if (!orders.isEmpty()) {
                em.createQuery(
                    "SELECT DISTINCT oi FROM OrderItem oi " +
                    "LEFT JOIN FETCH oi.product p " +
                    "WHERE oi.order IN :orders", 
                    com.utephonehub.entity.OrderItem.class)
                    .setParameter("orders", orders)
                    .getResultList();
            }
            
            return orders;
        } finally {
            em.close();
        }
    }
    
    /**
     * Get total revenue by date range (for Dashboard)
     */
    public BigDecimal getTotalRevenueByDateRange(LocalDate startDate, LocalDate endDate) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            BigDecimal result = em.createQuery(
                "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
                "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
                "AND o.status != :cancelledStatus", BigDecimal.class)
                .setParameter("startDate", startDateTime)
                .setParameter("endDate", endDateTime)
                .setParameter("cancelledStatus", Order.OrderStatus.CANCELLED)
                .getSingleResult();
            
            return result;
        } finally {
            em.close();
        }
    }
    
    /**
     * Count orders by date range (for Dashboard)
     */
    public long countOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            return em.createQuery(
                "SELECT COUNT(o) FROM Order o " +
                "WHERE o.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("startDate", startDateTime)
                .setParameter("endDate", endDateTime)
                .getSingleResult();
        } finally {
            em.close();
        }
    }
    
    /**
     * Count orders by status (for Dashboard)
     */
    public Map<String, Long> countOrdersByStatus() {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            List<Object[]> results = em.createQuery(
                "SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status", Object[].class)
                .getResultList();
            
            Map<String, Long> statusCounts = new HashMap<>();
            for (Object[] result : results) {
                Order.OrderStatus status = (Order.OrderStatus) result[0];
                Long count = (Long) result[1];
                statusCounts.put(status.name(), count);
            }
            
            // Ensure all statuses are present (even with 0 count)
            for (Order.OrderStatus status : Order.OrderStatus.values()) {
                statusCounts.putIfAbsent(status.name(), 0L);
            }
            
            return statusCounts;
        } finally {
            em.close();
        }
    }
    
    /**
     * Find recent orders (for Dashboard)
     */
    public List<Order> findRecentOrders(int limit) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return em.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                "LEFT JOIN FETCH o.items oi " +
                "LEFT JOIN FETCH oi.product " +
                "ORDER BY o.createdAt DESC", Order.class)
                .setMaxResults(limit)
                .getResultList();
        } finally {
            em.close();
        }
    }
}
