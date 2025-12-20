package com.utephonehub.repository;

import com.utephonehub.entity.Category;
import com.utephonehub.config.DatabaseConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entity
 */
public class CategoryRepository {
    
    private static final Logger logger = LogManager.getLogger(CategoryRepository.class);
    
    /**
     * Find all categories
     */
    public List<Category> findAll() {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Category> query = em.createQuery(
                "SELECT c FROM Category c ORDER BY c.name ASC", 
                Category.class
            );
            List<Category> categories = query.getResultList();
            // Force initialization of lazy collections before EntityManager closes
            for (Category category : categories) {
                category.getProducts().size(); // Initialize products collection
                category.getChildren().size(); // Initialize children collection
            }
            return categories;
        } catch (Exception e) {
            logger.error("Error finding all categories", e);
            throw new RuntimeException("Error finding all categories", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Find category by ID
     */
    public Optional<Category> findById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Category category = em.find(Category.class, id);
            return Optional.ofNullable(category);
        } catch (Exception e) {
            logger.error("Error finding category by id: {}", id, e);
            throw new RuntimeException("Error finding category", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Find category by ID with parent relation (EAGER fetch)
     */
    public Optional<Category> findByIdWithParent(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Category category = em.createQuery(
                "SELECT c FROM Category c " +
                "LEFT JOIN FETCH c.parent " +
                "WHERE c.id = :id", 
                Category.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);
            return Optional.ofNullable(category);
        } catch (Exception e) {
            logger.error("Error finding category by id with parent: {}", id, e);
            throw new RuntimeException("Error finding category with parent", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Save category
     */
    public Category save(Category category) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            
            if (category.getId() == null) {
                em.persist(category);
            } else {
                category = em.merge(category);
            }
            
            tx.commit();
            return category;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("Error saving category", e);
            throw new RuntimeException("Error saving category", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Delete category
     */
    public void delete(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Category category = em.find(Category.class, id);
            if (category != null) {
                em.remove(category);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("Error deleting category with id: {}", id, e);
            throw new RuntimeException("Error deleting category", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Delete category by ID
     */
    public void deleteById(Long id) {
        delete(id);
    }
    
    /**
     * Find category by name
     */
    public Optional<Category> findByName(String name) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Category> query = em.createQuery(
                "SELECT c FROM Category c WHERE c.name = :name", 
                Category.class
            );
            query.setParameter("name", name);
            List<Category> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            logger.error("Error finding category by name: {}", name, e);
            throw new RuntimeException("Error finding category by name", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Count products by category ID
     */
    public long countProductsByCategoryId(Long categoryId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId", 
                Long.class
            )
            .setParameter("categoryId", categoryId)
            .getSingleResult();
            return count != null ? count : 0L;
        } catch (Exception e) {
            logger.error("Error counting products for category id: {}", categoryId, e);
            throw new RuntimeException("Error counting products", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Count children categories by parent category ID
     */
    public long countChildrenByCategoryId(Long categoryId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(c) FROM Category c WHERE c.parent.id = :categoryId", 
                Long.class
            )
            .setParameter("categoryId", categoryId)
            .getSingleResult();
            return count != null ? count : 0L;
        } catch (Exception e) {
            logger.error("Error counting children for category id: {}", categoryId, e);
            throw new RuntimeException("Error counting children", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
}
