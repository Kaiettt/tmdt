package com.utephonehub.repository;

import com.utephonehub.entity.User;
import com.utephonehub.config.DatabaseConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * User Repository
 * Quản lý các thao tác database cho User entity
 */
public class UserRepository {
    
    private static final Logger logger = LogManager.getLogger(UserRepository.class);
    
    /**
     * Lưu user mới
     * @param user User entity
     * @return User đã được lưu
     */
    public User save(User user) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        User savedUser = null;
        try {
            tx.begin();
            if (user.getId() == null) {
                em.persist(user);
                em.flush();
                logger.info("Created new user: {}", user.getEmail());
                savedUser = user;
            } else {
                savedUser = em.merge(user);
                em.flush(); // Force sync to database immediately
                logger.info("Updated user: {}", user.getEmail());
            }
            
            // Get ID before commit
            Long userId = savedUser.getId();
            
            tx.commit();
            
            // Clear cache sau commit
            em.clear();
            
            // Query lại để get fresh data (dùng new transaction/em để safety)
            User freshUser = em.find(User.class, userId);
            if (freshUser != null) {
                logger.debug("Refreshed user from DB: {}", freshUser.getEmail());
                return freshUser;
            }
            
            logger.warn("Could not refresh user, returning merged instance");
            return savedUser;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("Error saving user: {}", user != null ? user.getEmail() : "null", e);
            throw new RuntimeException("Failed to save user", e);
        }
    }
    
    /**
     * Tìm user theo ID
     * @param id User ID
     * @return Optional<User>
     */
    public Optional<User> findById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            em.clear(); // Clear cache trước khi query
            User user = em.find(User.class, id);
            if (user != null) {
                em.refresh(user); // Force refresh từ DB
            }
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.error("Error finding user by ID: {}", id, e);
            throw new RuntimeException("Failed to find user by ID", e);
        }
    }
    
    /**
     * Tìm user theo email
     * @param email User email
     * @return Optional<User>
     */
    public Optional<User> findByEmail(String email) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            em.clear(); // Clear cache trước khi query
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.email = :email", User.class);
            query.setParameter("email", email);
            query.setHint("jakarta.persistence.cache.retrieveMode", "BYPASS"); // Bypass cache
            query.setHint("jakarta.persistence.cache.storeMode", "BYPASS"); // Don't store in cache
            User user = query.getSingleResult();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.debug("User not found with email: {}", email);
            return Optional.empty();
        }
    }
    
    /**
     * Tìm user theo username
     * @param username Username cần tìm
     * @return Optional<User>
     */
    public Optional<User> findByUsername(String username) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            User user = query.getSingleResult();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.debug("User not found with username: {}", username);
            return Optional.empty();
        }
    }
    
    /**
     * Kiểm tra email đã tồn tại chưa
     * @param email Email cần kiểm tra
     * @return true nếu email đã tồn tại
     */
    public boolean existsByEmail(String email) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class);
            query.setParameter("email", email);
            Long count = query.getSingleResult();
            return count > 0;
        } catch (Exception e) {
            logger.error("Error checking email existence: {}", email, e);
            throw new RuntimeException("Failed to check email existence", e);
        }
    }
    
    /**
     * Kiểm tra username đã tồn tại chưa
     * @param username Username cần kiểm tra
     * @return true nếu username đã tồn tại
     */
    public boolean existsByUsername(String username) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class);
            query.setParameter("username", username);
            Long count = query.getSingleResult();
            return count > 0;
        } catch (Exception e) {
            logger.error("Error checking username existence: {}", username, e);
            throw new RuntimeException("Failed to check username existence", e);
        }
    }
    
    /**
     * Lấy tất cả users
     * @return List<User>
     */
    public List<User> findAll() {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u ORDER BY u.createdAt DESC", User.class);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Error finding all users", e);
            throw new RuntimeException("Failed to find all users", e);
        }
    }
    
    /**
     * Lấy users với pagination
     * @param page Số trang (bắt đầu từ 0)
     * @param size Kích thước trang
     * @return List<User>
     */
    public List<User> findAll(int page, int size) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u ORDER BY u.createdAt DESC", User.class);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Error finding users with pagination", e);
            throw new RuntimeException("Failed to find users with pagination", e);
        }
    }
    
    /**
     * Đếm tổng số users
     * @return Tổng số users
     */
    public long count() {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(u) FROM User u", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            logger.error("Error counting users", e);
            throw new RuntimeException("Failed to count users", e);
        }
    }
    
    /**
     * Xóa user
     * @param id User ID
     * @return true nếu xóa thành công
     */
    public boolean deleteById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User user = em.find(User.class, id);
            if (user != null) {
                em.remove(user);
                tx.commit();
                logger.info("Deleted user: {}", user.getEmail());
                return true;
            }
            tx.rollback();
            return false;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("Error deleting user by ID: {}", id, e);
            throw new RuntimeException("Failed to delete user", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Cập nhật trạng thái user
     * @param id User ID
     * @param status Trạng thái mới
     * @return true nếu cập nhật thành công
     */
    public boolean updateStatus(Long id, User.UserStatus status) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User user = em.find(User.class, id);
            if (user != null) {
                user.setStatus(status);
                em.merge(user);
                tx.commit();
                logger.info("Updated user status: {} -> {}", user.getEmail(), status);
                return true;
            }
            tx.rollback();
            return false;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("Error updating user status: {}", id, e);
            throw new RuntimeException("Failed to update user status", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Count active users (for Dashboard)
     */
    public long countActiveUsers() {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.status = :status", Long.class)
                .setParameter("status", User.UserStatus.active)
                .getSingleResult();
        } catch (Exception e) {
            logger.error("Error counting active users", e);
            throw new RuntimeException("Failed to count active users", e);
        }
    }
    
    /**
     * Count new users by date range (for Dashboard)
     */
    public long countNewUsers(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            java.time.LocalDateTime startDateTime = startDate.atStartOfDay();
            java.time.LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            return em.createQuery(
                "SELECT COUNT(u) FROM User u " +
                "WHERE u.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("startDate", startDateTime)
                .setParameter("endDate", endDateTime)
                .getSingleResult();
        } catch (Exception e) {
            logger.error("Error counting new users", e);
            throw new RuntimeException("Failed to count new users", e);
        }
    }
}
