package com.utephonehub.controller;

import com.utephonehub.dto.request.ProductRequest;
import com.utephonehub.dto.response.ProductResponse;
import com.utephonehub.entity.Product;
import com.utephonehub.entity.User;
import com.utephonehub.repository.ProductRepository;
import com.utephonehub.repository.CategoryRepository;
import com.utephonehub.repository.BrandRepository;
import com.utephonehub.repository.UserRepository;
import com.utephonehub.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Product Management Controller
 * Quản lý sản phẩm - chỉ dành cho admin
 */
@WebServlet("/api/v1/admin/products/*")
public class AdminProductController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(AdminProductController.class);
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final JsonUtil jsonUtil;
    
    public AdminProductController() {
        this.productRepository = new ProductRepository();
        this.categoryRepository = new CategoryRepository();
        this.brandRepository = new BrandRepository();
        this.userRepository = new UserRepository();
        this.jsonUtil = new JsonUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check admin authentication
        if (!isAdmin(request, response)) {
            return;
        }
        
        String pathInfo = request.getPathInfo();
        logger.info("AdminProductController GET request: {}", pathInfo);
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/v1/admin/products - Lấy danh sách products
                handleGetProducts(request, response);
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/v1/admin/products/{id} - Chi tiết product
                handleGetProductDetail(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in AdminProductController GET", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check admin authentication
        if (!isAdmin(request, response)) {
            return;
        }
        
        logger.info("AdminProductController POST request");
        
        try {
            // POST /api/v1/admin/products - Tạo product mới
            handleCreateProduct(request, response);
        } catch (Exception e) {
            logger.error("Error in AdminProductController POST", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check admin authentication
        if (!isAdmin(request, response)) {
            return;
        }
        
        String pathInfo = request.getPathInfo();
        logger.info("AdminProductController PUT request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // PUT /api/v1/admin/products/{id} - Cập nhật product
                handleUpdateProduct(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Product ID is required");
            }
        } catch (Exception e) {
            logger.error("Error in AdminProductController PUT", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check admin authentication
        if (!isAdmin(request, response)) {
            return;
        }
        
        String pathInfo = request.getPathInfo();
        logger.info("AdminProductController DELETE request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // DELETE /api/v1/admin/products/{id} - Xóa product
                handleDeleteProduct(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Product ID is required");
            }
        } catch (Exception e) {
            logger.error("Error in AdminProductController DELETE", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * Lấy danh sách products với filters
     */
    private void handleGetProducts(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Get query parameters
            String categoryIdStr = request.getParameter("categoryId");
            String brandIdStr = request.getParameter("brandId");
            String keyword = request.getParameter("keyword");
            String statusStr = request.getParameter("status");
            String pageStr = request.getParameter("page");
            String sizeStr = request.getParameter("size");
            
            List<Product> products;
            
            // Apply filters
            if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
                Long categoryId = Long.parseLong(categoryIdStr);
                products = productRepository.findByCategoryId(categoryId);
            } else if (brandIdStr != null && !brandIdStr.isEmpty()) {
                Long brandId = Long.parseLong(brandIdStr);
                products = productRepository.findByBrandId(brandId);
            } else if (keyword != null && !keyword.isEmpty()) {
                products = productRepository.searchByName(keyword);
            } else {
                // Get all products (including inactive for admin)
                products = productRepository.findAll();
            }
            
            // Filter by status if provided
            if (statusStr != null && !statusStr.isEmpty()) {
                boolean status = Boolean.parseBoolean(statusStr);
                products = products.stream()
                    .filter(p -> p.getStatus() == status)
                    .collect(Collectors.toList());
            }
            
            // Apply pagination if provided
            if (pageStr != null && sizeStr != null) {
                int page = Integer.parseInt(pageStr);
                int size = Integer.parseInt(sizeStr);
                int start = page * size;
                int end = Math.min(start + size, products.size());
                products = products.subList(start, end);
            }
            
            // Convert to response DTOs
            List<ProductResponse> productResponses = products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy danh sách sản phẩm thành công");
            responseData.put("data", productResponses);
            responseData.put("total", productResponses.size());
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (Exception e) {
            logger.error("Error getting products", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy danh sách sản phẩm");
        }
    }
    
    /**
     * Lấy chi tiết product
     */
    private void handleGetProductDetail(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long productId = Long.parseLong(pathInfo.substring(1));
            
            Product product = productRepository.findById(productId)
                .orElse(null);
            
            if (product == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy sản phẩm");
                return;
            }
            
            ProductResponse productResponse = convertToProductResponse(product);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy thông tin sản phẩm thành công");
            responseData.put("data", productResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Product ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error getting product detail", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy thông tin sản phẩm");
        }
    }
    
    /**
     * Tạo product mới
     */
    private void handleCreateProduct(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Parse request body
            ProductRequest productRequest = jsonUtil.parseJson(request, ProductRequest.class);
            
            // Validate required fields
            if (productRequest.getName() == null || productRequest.getName().trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Tên sản phẩm không được để trống");
                return;
            }
            
            if (productRequest.getPrice() == null || productRequest.getPrice().doubleValue() <= 0) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Giá sản phẩm không hợp lệ");
                return;
            }
            
            if (productRequest.getCategoryId() == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Danh mục sản phẩm không được để trống");
                return;
            }
            
            if (productRequest.getBrandId() == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Thương hiệu sản phẩm không được để trống");
                return;
            }
            
            // Check category exists
            var category = categoryRepository.findById(productRequest.getCategoryId())
                .orElse(null);
            if (category == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Danh mục không tồn tại");
                return;
            }
            
            // Check brand exists
            var brand = brandRepository.findById(productRequest.getBrandId())
                .orElse(null);
            if (brand == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Thương hiệu không tồn tại");
                return;
            }
            
            // Create new product
            Product product = new Product();
            product.setName(productRequest.getName());
            product.setDescription(productRequest.getDescription());
            product.setPrice(productRequest.getPrice());
            product.setStockQuantity(productRequest.getStockQuantity() != null ? productRequest.getStockQuantity() : 0);
            product.setThumbnailUrl(productRequest.getThumbnailUrl());
            product.setSpecifications(productRequest.getSpecifications());
            product.setStatus(productRequest.getStatus() != null ? productRequest.getStatus() : true);
            product.setCategory(category);
            product.setBrand(brand);
            
            // Save product
            Product savedProduct = productRepository.save(product);
            
            // Re-fetch với relations để tránh LazyInitializationException
            savedProduct = productRepository.findByIdWithRelations(savedProduct.getId())
                .orElseThrow(() -> new RuntimeException("Product not found after save"));
            
            ProductResponse productResponse = convertToProductResponse(savedProduct);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Tạo sản phẩm thành công");
            responseData.put("data", productResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, responseData);
            
        } catch (Exception e) {
            logger.error("Error creating product", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi tạo sản phẩm");
        }
    }
    
    /**
     * Cập nhật product
     */
    private void handleUpdateProduct(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long productId = Long.parseLong(pathInfo.substring(1));
            
            // Check product exists - EAGER fetch category và brand
            Product product = productRepository.findByIdWithRelations(productId)
                .orElse(null);
            
            if (product == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy sản phẩm");
                return;
            }
            
            // Parse request body
            ProductRequest productRequest = jsonUtil.parseJson(request, ProductRequest.class);
            
            // Update fields if provided
            if (productRequest.getName() != null && !productRequest.getName().trim().isEmpty()) {
                product.setName(productRequest.getName());
            }
            
            if (productRequest.getDescription() != null) {
                product.setDescription(productRequest.getDescription());
            }
            
            if (productRequest.getPrice() != null && productRequest.getPrice().doubleValue() > 0) {
                product.setPrice(productRequest.getPrice());
            }
            
            if (productRequest.getStockQuantity() != null) {
                product.setStockQuantity(productRequest.getStockQuantity());
            }
            
            if (productRequest.getThumbnailUrl() != null) {
                product.setThumbnailUrl(productRequest.getThumbnailUrl());
            }
            
            if (productRequest.getSpecifications() != null) {
                product.setSpecifications(productRequest.getSpecifications());
            }
            
            if (productRequest.getStatus() != null) {
                product.setStatus(productRequest.getStatus());
            }
            
            // Update category if provided
            if (productRequest.getCategoryId() != null) {
                var category = categoryRepository.findById(productRequest.getCategoryId())
                    .orElse(null);
                if (category != null) {
                    product.setCategory(category);
                }
            }
            
            // Update brand if provided
            if (productRequest.getBrandId() != null) {
                var brand = brandRepository.findById(productRequest.getBrandId())
                    .orElse(null);
                if (brand != null) {
                    product.setBrand(brand);
                }
            }
            
            // Save updated product
            Product updatedProduct = productRepository.save(product);
            
            // Re-fetch với relations để tránh LazyInitializationException
            updatedProduct = productRepository.findByIdWithRelations(updatedProduct.getId())
                .orElseThrow(() -> new RuntimeException("Product not found after save"));
            
            ProductResponse productResponse = convertToProductResponse(updatedProduct);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Cập nhật sản phẩm thành công");
            responseData.put("data", productResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Product ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error updating product", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi cập nhật sản phẩm");
        }
    }
    
    /**
     * Xóa product (soft delete - set status = false)
     */
    private void handleDeleteProduct(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long productId = Long.parseLong(pathInfo.substring(1));
            
            // Check product exists - EAGER fetch để tránh LazyInitializationException
            Product product = productRepository.findByIdWithRelations(productId)
                .orElse(null);
            
            if (product == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy sản phẩm");
                return;
            }
            
            // Check if product can be deleted (no order_items, cart_items, reviews)
            if (!productRepository.canDeleteProduct(productId)) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, 
                    "Không thể xóa sản phẩm này vì đã được sử dụng trong đơn hàng, giỏ hàng hoặc có đánh giá");
                return;
            }
            
            // Hard delete - xóa thật khỏi database
            boolean deleted = productRepository.deleteById(productId);
            
            if (deleted) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "Xóa sản phẩm thành công");
                sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể xóa sản phẩm");
            }
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Product ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error deleting product", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi xóa sản phẩm");
        }
    }
    
    /**
     * Kiểm tra quyền admin
     */
    private boolean isAdmin(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        // Get userId from request attribute (set by JwtAuthenticationFilter)
        Long userId = (Long) request.getAttribute("currentUserId");
        
        if (userId == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - Missing user ID");
            return false;
        }
        
        try {
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - User not found");
                return false;
            }
            
            if (user.getRole() != User.UserRole.admin) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                    "Forbidden - Admin access required");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking admin role", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            return false;
        }
    }
    
    /**
     * Convert Product entity to ProductResponse DTO
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
        response.setCategoryId(product.getCategory().getId());
        response.setCategoryName(product.getCategory().getName());
        response.setBrandId(product.getBrand().getId());
        response.setBrandName(product.getBrand().getName());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }
    
    /**
     * Gửi JSON response
     */
    private void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) 
            throws IOException {
        
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = jsonUtil.toJson(data);
        response.getWriter().write(jsonResponse);
    }
    
    /**
     * Gửi error response
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) 
            throws IOException {
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("message", message);
        
        sendJsonResponse(response, statusCode, errorData);
    }
}
