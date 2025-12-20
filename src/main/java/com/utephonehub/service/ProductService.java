package com.utephonehub.service;

import com.utephonehub.dto.response.ProductResponse;
import com.utephonehub.entity.Product;
import com.utephonehub.repository.ProductRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Product Service
 * Business logic cho Product management
 */
public class ProductService {
    
    private static final Logger logger = LogManager.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    
    public ProductService() {
        this.productRepository = new ProductRepository();
    }
    
    /**
     * Lấy danh sách sản phẩm với phân trang, filter và sort
     * 
     * @param page Số trang (bắt đầu từ 1)
     * @param limit Số lượng sản phẩm mỗi trang
     * @param categoryId Filter theo category ID
     * @param brandId Filter theo brand ID
     * @param minPrice Filter giá tối thiểu
     * @param maxPrice Filter giá tối đa
     * @param keyword Từ khóa tìm kiếm
     * @param sortBy Sắp xếp theo field (vd: "price:asc", "createdAt:desc")
     * @return Map chứa danh sách products và metadata
     */
    public Map<String, Object> getProducts(
            Integer page,
            Integer limit,
            Long categoryId,
            Long brandId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String keyword,
            String sortBy
    ) {
        try {
            // Default values
            int currentPage = (page != null && page > 0) ? page : 1;
            int pageSize = (limit != null && limit > 0) ? limit : 12;
            String sortField = "createdAt";
            String sortDirection = "desc";
            
            // Parse sortBy parameter
            if (sortBy != null && sortBy.contains(":")) {
                String[] parts = sortBy.split(":");
                sortField = parts[0];
                if (parts.length > 1) {
                    sortDirection = parts[1].equalsIgnoreCase("asc") ? "asc" : "desc";
                }
            }
            
            // Calculate offset
            int offset = (currentPage - 1) * pageSize;
            
            // Get products with filters
            List<Product> products = productRepository.findWithFilters(
                categoryId, brandId, minPrice, maxPrice, keyword,
                sortField, sortDirection, pageSize, offset
            );
            
            // Get total count for pagination
            long totalItems = productRepository.countWithFilters(
                categoryId, brandId, minPrice, maxPrice, keyword
            );
            
            int totalPages = (int) Math.ceil((double) totalItems / pageSize);
            
            // Convert entities to DTOs
            List<ProductResponse> productResponses = products.stream()
                    .map(this::convertToProductResponse)
                    .collect(Collectors.toList());
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("products", productResponses);
            
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", currentPage);
            pagination.put("limit", pageSize);
            pagination.put("totalItems", totalItems);
            pagination.put("totalPages", totalPages);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pagination", pagination);
            response.put("metadata", metadata);
            
            logger.info("Retrieved {} products (page {}/{})", products.size(), currentPage, totalPages);
            return response;
            
        } catch (Exception e) {
            logger.error("Error getting products", e);
            throw new RuntimeException("Failed to retrieve products", e);
        }
    }
    
    /**
     * Lấy chi tiết sản phẩm theo ID
     * 
     * @param productId Product ID
     * @return Optional<Product>
     */
    public Optional<Product> getProductById(Long productId) {
        try {
            return productRepository.findById(productId);
        } catch (Exception e) {
            logger.error("Error getting product by ID: {}", productId, e);
            throw new RuntimeException("Failed to retrieve product", e);
        }
    }
    
    /**
     * Lấy product theo ID với EAGER fetch images (for product detail page)
     * Tránh LazyInitializationException khi access images collection
     * 
     * @param productId Product ID
     * @return Optional<Product>
     */
    public Optional<Product> getProductByIdWithImages(Long productId) {
        try {
            return productRepository.findByIdWithAllRelations(productId);
        } catch (Exception e) {
            logger.error("Error getting product by ID with images: {}", productId, e);
            throw new RuntimeException("Failed to retrieve product with images", e);
        }
    }
    
    /**
     * Lấy các sản phẩm mới nhất
     * 
     * @param limit Số lượng sản phẩm
     * @return List<Product>
     */
    public List<Product> getLatestProducts(int limit) {
        try {
            return productRepository.findLatest(limit);
        } catch (Exception e) {
            logger.error("Error getting latest products", e);
            throw new RuntimeException("Failed to retrieve latest products", e);
        }
    }
    
    /**
     * Lấy các sản phẩm bán chạy
     * 
     * @param limit Số lượng sản phẩm
     * @return List<Product>
     */
    public List<Product> getBestSellingProducts(int limit) {
        try {
            return productRepository.findBestSelling(limit);
        } catch (Exception e) {
            logger.error("Error getting best selling products", e);
            throw new RuntimeException("Failed to retrieve best selling products", e);
        }
    }
    
    /**
     * Kiểm tra sản phẩm còn hàng
     * 
     * @param productId Product ID
     * @param quantity Số lượng cần kiểm tra
     * @return true nếu đủ hàng
     */
    public boolean isInStock(Long productId, int quantity) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return false;
        }
        Product product = productOpt.get();
        return product.getStatus() && product.getStockQuantity() >= quantity;
    }
    
    /**
     * Giảm số lượng tồn kho
     * 
     * @param productId Product ID
     * @param quantity Số lượng cần giảm
     * @return true nếu thành công
     */
    public boolean decreaseStock(Long productId, int quantity) {
        try {
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                logger.warn("Product not found: {}", productId);
                return false;
            }
            
            Product product = productOpt.get();
            if (product.getStockQuantity() < quantity) {
                logger.warn("Insufficient stock for product {}: {} < {}", 
                    productId, product.getStockQuantity(), quantity);
                return false;
            }
            
            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);
            
            logger.info("Decreased stock for product {}: -{}", productId, quantity);
            return true;
            
        } catch (Exception e) {
            logger.error("Error decreasing stock for product: {}", productId, e);
            return false;
        }
    }
    
    /**
     * Convert Product entity to ProductResponse DTO
     * 
     * @param product Product entity
     * @return ProductResponse DTO
     */
    private ProductResponse convertToProductResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());
        response.setThumbnailUrl(product.getThumbnailUrl());
        response.setSpecifications(product.getSpecifications());
        response.setStatus(product.getStatus());
        
        // Category info
        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getId());
            response.setCategoryName(product.getCategory().getName());
        }
        
        // Brand info
        if (product.getBrand() != null) {
            response.setBrandId(product.getBrand().getId());
            response.setBrandName(product.getBrand().getName());
        }
        
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        
        return response;
    }
}
