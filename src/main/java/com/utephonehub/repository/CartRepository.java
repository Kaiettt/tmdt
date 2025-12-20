package com.utephonehub.repository;

import com.utephonehub.config.DatabaseConfig;
import com.utephonehub.entity.Cart;
import com.utephonehub.entity.CartItem;
import com.utephonehub.entity.Product;
import com.utephonehub.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;

import java.util.Optional;

public class CartRepository {
    
    public Optional<Cart> findByUserId(Long userId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Cart cart = em.createQuery(
                "SELECT DISTINCT c FROM Cart c " +
                "LEFT JOIN FETCH c.items i " +
                "LEFT JOIN FETCH i.product p " +
                "LEFT JOIN FETCH p.category " +
                "LEFT JOIN FETCH p.brand " +
                "WHERE c.user.id = :userId", Cart.class)
                .setParameter("userId", userId)
                .getSingleResult();
            return Optional.of(cart);
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public Cart save(Cart cart) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Cart saved;
            if (cart.getId() == null) {
                em.persist(cart);
                saved = cart;
            } else {
                saved = em.merge(cart);
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
    
    public Optional<CartItem> findCartItem(Long cartId, Long productId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            CartItem item = em.createQuery(
                "SELECT ci FROM CartItem ci " +
                "JOIN FETCH ci.product " +
                "WHERE ci.cart.id = :cartId AND ci.product.id = :productId", 
                CartItem.class)
                .setParameter("cartId", cartId)
                .setParameter("productId", productId)
                .getSingleResult();
            return Optional.of(item);
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
    
    public CartItem saveCartItem(CartItem item) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CartItem saved;
            if (item.getId() == null) {
                em.persist(item);
                saved = item;
            } else {
                saved = em.merge(item);
            }
            tx.commit();
            return saved;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
    
    public void deleteCartItem(Long cartItemId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CartItem item = em.find(CartItem.class, cartItemId);
            if (item != null) {
                em.remove(item);
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
    
    public Cart createCartForUser(User user) {
        Cart cart = new Cart(user);
        return save(cart);
    }
}
