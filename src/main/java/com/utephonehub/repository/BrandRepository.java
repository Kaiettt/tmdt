package com.utephonehub.repository;

import com.utephonehub.entity.Brand;
import com.utephonehub.config.DatabaseConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Brand entity
 */
public class BrandRepository {
    
    private static final Logger logger = LogManager.getLogger(BrandRepository.class);
    
    /**
     * Find all brands
     */
    public List<Brand> findAll() {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Brand> query = em.createQuery(
                "SELECT b FROM Brand b ORDER BY b.name ASC", 
                Brand.class
            );
            List<Brand> brands = query.getResultList();
            // Force initialization of lazy collections before EntityManager closes
            for (Brand brand : brands) {
                brand.getProducts().size(); // Initialize products collection
            }
            return brands;
        } catch (Exception e) {
            logger.error("Error finding all brands", e);
            throw new RuntimeException("Error finding all brands", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Find brand by ID
     */
    public Optional<Brand> findById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Brand brand = em.find(Brand.class, id);
            return Optional.ofNullable(brand);
        } catch (Exception e) {
            logger.error("Error finding brand by id: {}", id, e);
            throw new RuntimeException("Error finding brand", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Save brand
     */
    public Brand save(Brand brand) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            
            if (brand.getId() == null) {
                em.persist(brand);
            } else {
                brand = em.merge(brand);
            }
            
            tx.commit();
            return brand;
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("Error saving brand", e);
            throw new RuntimeException("Error saving brand", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Delete brand
     */
    public void delete(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Brand brand = em.find(Brand.class, id);
            if (brand != null) {
                em.remove(brand);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("Error deleting brand with id: {}", id, e);
            throw new RuntimeException("Error deleting brand", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Delete brand by ID
     */
    public void deleteById(Long id) {
        delete(id);
    }
    
    /**
     * Find brand by name
     */
    public Optional<Brand> findByName(String name) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Brand> query = em.createQuery(
                "SELECT b FROM Brand b WHERE b.name = :name", 
                Brand.class
            );
            query.setParameter("name", name);
            List<Brand> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            logger.error("Error finding brand by name: {}", name, e);
            throw new RuntimeException("Error finding brand by name", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
    
    /**
     * Count products by brand ID
     */
    public long countProductsByBrandId(Long brandId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(p) FROM Product p WHERE p.brand.id = :brandId", 
                Long.class
            )
            .setParameter("brandId", brandId)
            .getSingleResult();
            return count != null ? count : 0L;
        } catch (Exception e) {
            logger.error("Error counting products for brand id: {}", brandId, e);
            throw new RuntimeException("Error counting products", e);
        } finally {
            DatabaseConfig.closeEntityManager();
        }
    }
}
