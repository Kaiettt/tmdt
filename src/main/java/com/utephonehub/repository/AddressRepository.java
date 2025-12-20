package com.utephonehub.repository;

import com.utephonehub.config.DatabaseConfig;
import com.utephonehub.entity.Address;
import com.utephonehub.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

public class AddressRepository {
    
    public List<Address> findByUserId(Long userId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Address> q = em.createQuery(
                "SELECT a FROM Address a WHERE a.user.id = :userId ORDER BY a.isDefault DESC, a.createdAt DESC", 
                Address.class);
            q.setParameter("userId", userId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Address> findByUser(User user) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            TypedQuery<Address> q = em.createQuery("SELECT a FROM Address a WHERE a.user = :user", Address.class);
            q.setParameter("user", user);
            return q.getResultList();
        } finally {
            em.close();
        }
    }
    
    public Optional<Address> findById(Long id) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Address.class, id));
        } finally {
            em.close();
        }
    }
    
    public Address save(Address address) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            
            // If this is set as default, unset other defaults
            if (address.getIsDefault() != null && address.getIsDefault()) {
                em.createQuery("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
                    .setParameter("userId", address.getUser().getId())
                    .executeUpdate();
            }
            
            if (address.getId() == null) {
                em.persist(address);
            } else {
                address = em.merge(address);
            }
            
            em.getTransaction().commit();
            em.close(); // Close AFTER commit
            return address;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close(); // Close AFTER rollback
            throw e;
        }
    }
    
    public Address update(Address address) {
        return save(address);
    }
    
    public Optional<Address> findByIdAndUserId(Long addressId, Long userId) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            List<Address> results = em.createQuery(
                "SELECT a FROM Address a WHERE a.id = :addressId AND a.user.id = :userId", 
                Address.class)
                .setParameter("addressId", addressId)
                .setParameter("userId", userId)
                .getResultList();
            
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }
    
    public void delete(Address address) {
        EntityManager em = DatabaseConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            Address managed = em.merge(address);
            em.remove(managed);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
