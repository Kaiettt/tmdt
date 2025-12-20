package com.utephonehub.repository;

import com.utephonehub.config.DatabaseConfig;
import com.utephonehub.entity.Review;
import com.utephonehub.entity.ReviewLike;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;

import java.util.List;
import java.util.Optional;

public class ReviewRepository {
    
    public List<Review> findByProductId(Long productId, int page, int limit) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return em.createQuery(
                "SELECT r FROM Review r " +
                "LEFT JOIN FETCH r.user " +
                "LEFT JOIN FETCH r.likes " +
                "WHERE r.product.id = :productId " +
                "ORDER BY r.createdAt DESC", Review.class)
                .setParameter("productId", productId)
                .setFirstResult((page - 1) * limit)
                .setMaxResults(limit)
                .getResultList();
        } finally {
            em.close();
        }
    }
    
    public long countByProductId(Long productId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return em.createQuery(
                "SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId", Long.class)
                .setParameter("productId", productId)
                .getSingleResult();
        } finally {
            em.close();
        }
    }
    
    public Review save(Review review) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Review saved;
            if (review.getId() == null) {
                em.persist(review);
                saved = review;
            } else {
                saved = em.merge(review);
            }
            tx.commit();
            em.close(); // Close AFTER commit
            return saved;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close(); // Close AFTER rollback
            throw e;
        }
    }
    
    public Optional<Review> findById(Long reviewId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Review review = em.createQuery(
                "SELECT r FROM Review r " +
                "LEFT JOIN FETCH r.user " +
                "LEFT JOIN FETCH r.likes " +
                "WHERE r.id = :reviewId", Review.class)
                .setParameter("reviewId", reviewId)
                .getSingleResult();
            return Optional.of(review);
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public boolean hasUserReviewedProduct(Long userId, Long productId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            long count = em.createQuery(
                "SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId AND r.product.id = :productId", Long.class)
                .setParameter("userId", userId)
                .setParameter("productId", productId)
                .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }
    
    public boolean hasUserPurchasedProduct(Long userId, Long productId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            long count = em.createQuery(
                "SELECT COUNT(oi) FROM OrderItem oi " +
                "WHERE oi.order.user.id = :userId " +
                "AND oi.product.id = :productId " +
                "AND oi.order.status = 'completed'", Long.class)
                .setParameter("userId", userId)
                .setParameter("productId", productId)
                .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }
    
    public Optional<ReviewLike> findReviewLike(Long reviewId, Long userId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            ReviewLike like = em.createQuery(
                "SELECT rl FROM ReviewLike rl WHERE rl.review.id = :reviewId AND rl.user.id = :userId", 
                ReviewLike.class)
                .setParameter("reviewId", reviewId)
                .setParameter("userId", userId)
                .getSingleResult();
            return Optional.of(like);
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public ReviewLike saveReviewLike(ReviewLike reviewLike) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(reviewLike);
            tx.commit();
            return reviewLike;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
    
    public void deleteReviewLike(Long reviewLikeId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ReviewLike like = em.find(ReviewLike.class, reviewLikeId);
            if (like != null) {
                em.remove(like);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
    
    public int getLikeCount(Long reviewId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(rl) FROM ReviewLike rl WHERE rl.review.id = :reviewId", Long.class)
                .setParameter("reviewId", reviewId)
                .getSingleResult();
            return count.intValue();
        } finally {
            em.close();
        }
    }
    
    public void delete(Long reviewId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Review review = em.find(Review.class, reviewId);
            if (review != null) {
                em.remove(review);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Double getAverageRating(Long productId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return em.createQuery(
                "SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId", Double.class)
                .setParameter("productId", productId)
                .getSingleResult();
        } finally {
            em.close();
        }
    }
}
