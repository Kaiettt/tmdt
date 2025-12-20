package com.utephonehub.service;

import com.utephonehub.entity.Category;
import com.utephonehub.dto.response.CategoryResponse;
import com.utephonehub.repository.CategoryRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Category operations
 */
public class CategoryService {
    
    private static final Logger logger = LogManager.getLogger(CategoryService.class);
    private final CategoryRepository categoryRepository;
    
    public CategoryService() {
        this.categoryRepository = new CategoryRepository();
    }
    
    /**
     * Get all categories
     */
    public List<CategoryResponse> getAllCategories() {
        logger.info("Getting all categories");
        List<Category> categories = categoryRepository.findAll();
        
        return categories.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get category by ID
     */
    public CategoryResponse getCategoryById(Long id) {
        logger.info("Getting category by ID: {}", id);
        
        return categoryRepository.findById(id)
            .map(this::convertToResponse)
            .orElse(null);
    }
    
    /**
     * Convert Category entity to CategoryResponse DTO
     */
    private CategoryResponse convertToResponse(Category category) {
        Long parentId = category.getParent() != null ? category.getParent().getId() : null;
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            parentId
        );
    }
}
