package com.utephonehub.service;

import com.utephonehub.entity.Brand;
import com.utephonehub.dto.response.BrandResponse;
import com.utephonehub.repository.BrandRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Brand operations
 */
public class BrandService {
    
    private static final Logger logger = LogManager.getLogger(BrandService.class);
    private final BrandRepository brandRepository;
    
    public BrandService() {
        this.brandRepository = new BrandRepository();
    }
    
    /**
     * Get all brands
     */
    public List<BrandResponse> getAllBrands() {
        logger.info("Getting all brands");
        List<Brand> brands = brandRepository.findAll();
        
        return brands.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get brand by ID
     */
    public BrandResponse getBrandById(Long id) {
        logger.info("Getting brand by ID: {}", id);
        
        return brandRepository.findById(id)
            .map(this::convertToResponse)
            .orElse(null);
    }
    
    /**
     * Convert Brand entity to BrandResponse DTO
     */
    private BrandResponse convertToResponse(Brand brand) {
        return new BrandResponse(
            brand.getId(),
            brand.getName()
        );
    }
}
