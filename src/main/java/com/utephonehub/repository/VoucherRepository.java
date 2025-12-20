package com.utephonehub.repository;

import com.utephonehub.config.DatabaseConfig;
import com.utephonehub.entity.Voucher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class VoucherRepository {
    
    public Optional<Voucher> findByCode(String code) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Voucher voucher = em.createQuery(
                "SELECT v FROM Voucher v WHERE v.code = :code", Voucher.class)
                .setParameter("code", code)
                .getSingleResult();
            return Optional.of(voucher);
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public boolean isVoucherValid(Voucher voucher) {
        // Check status
        if (voucher.getStatus() != Voucher.VoucherStatus.ACTIVE) {
            return false;
        }
        
        // Check expiry date
        if (voucher.getExpiryDate() != null && 
            voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        // Check max usage
        if (voucher.getMaxUsage() != null) {
            EntityManager em = DatabaseConfig.getEntityManager();
            try {
                long usageCount = em.createQuery(
                    "SELECT COUNT(o) FROM Order o WHERE o.voucher.id = :voucherId", Long.class)
                    .setParameter("voucherId", voucher.getId())
                    .getSingleResult();
                
                if (usageCount >= voucher.getMaxUsage()) {
                    return false;
                }
            } finally {
                em.close();
            }
        }
        
        return true;
    }
    
    public Voucher save(Voucher voucher) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (voucher.getId() == null) {
                em.persist(voucher);
            } else {
                voucher = em.merge(voucher);
            }
            tx.commit();
            em.close(); // Close AFTER commit
            return voucher;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close(); // Close AFTER rollback
            throw e;
        }
    }
    
    public Optional<Voucher> findById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Voucher voucher = em.find(Voucher.class, id);
            return Optional.ofNullable(voucher);
        } finally {
            em.close();
        }
    }
    
    public List<Voucher> findAll() {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            List<Voucher> vouchers = em.createQuery(
                "SELECT v FROM Voucher v ORDER BY v.createdAt DESC", Voucher.class)
                .getResultList();
            // Force initialization of lazy collections before EntityManager closes
            for (Voucher voucher : vouchers) {
                voucher.getOrders().size(); // Initialize orders collection
            }
            return vouchers;
        } finally {
            em.close();
        }
    }
    
    /**
     * Count total times a voucher has been used
     */
    public long countVoucherUsage(Long voucherId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return em.createQuery(
                "SELECT COUNT(o) FROM Order o WHERE o.voucher.id = :voucherId", Long.class)
                .setParameter("voucherId", voucherId)
                .getSingleResult();
        } finally {
            em.close();
        }
    }
    
    /**
     * Count how many times a specific user has used a voucher
     */
    public long countUserUsage(Long voucherId, Long userId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return em.createQuery(
                "SELECT COUNT(o) FROM Order o WHERE o.voucher.id = :voucherId AND o.user.id = :userId", 
                Long.class)
                .setParameter("voucherId", voucherId)
                .setParameter("userId", userId)
                .getSingleResult();
        } finally {
            em.close();
        }
    }
    
    /**
     * Increment the used count for a voucher
     * This method doesn't exist in entity yet, so we just track via orders count
     * If you add usedCount field to Voucher entity, you can update it here
     */
    public void incrementUsedCount(Long voucherId) {
        // For now, we just count via orders relationship
        // If Voucher entity has a usedCount field, implement update here:
        // UPDATE vouchers SET current_usage = current_usage + 1 WHERE id = :voucherId
        
        // Current implementation: No-op since we count dynamically
        // This is safe for MVP, but consider adding usedCount field for performance
    }
}
