package com.utephonehub.controller;

import com.google.gson.JsonObject;
import com.utephonehub.entity.Product;
import com.utephonehub.service.ProductService;
import com.utephonehub.service.ReviewService;
import com.utephonehub.util.JsonUtil;
import com.utephonehub.util.RequestUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Product Controller
 * REST API endpoints cho Product management
 * Base path: /api/v1/products
 */
@WebServlet(name = "ProductController", urlPatterns = {"/api/v1/products", "/api/v1/products/*"})
public class ProductController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(ProductController.class);
    private final JsonUtil jsonUtil = new JsonUtil();
    private ProductService productService;
    private ReviewService reviewService;
    
    @Override
    public void init() throws ServletException {
        super.init();
        this.productService = new ProductService();
        this.reviewService = new ReviewService();
        logger.info("ProductController initialized");
    }
    
    /**
     * GET /api/v1/products - Lấy danh sách sản phẩm
     * GET /api/v1/products/{id} - Lấy chi tiết sản phẩm
     * GET /api/v1/products/{id}/reviews - Lấy danh sách đánh giá
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        
        try {
            // GET /products/{id}/reviews - Get product reviews
            if (pathInfo != null && pathInfo.matches("/\\d+/reviews")) {
                handleGetProductReviews(request, response, pathInfo);
                return;
            }
            
            // GET /products/{id} - Get product by ID
            if (pathInfo != null && !pathInfo.equals("/")) {
                handleGetProductById(request, response, pathInfo);
                return;
            }
            
            // GET /products - Get products list with filters
            handleGetProducts(request, response);
            
        } catch (Exception e) {
            logger.error("Error in ProductController.doGet", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Lỗi hệ thống khi xử lý yêu cầu");
        }
    }
    
    /**
     * Handle GET /products - Lấy danh sách sản phẩm với filters
     */
    private void handleGetProducts(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        try {
            // Parse query parameters
            String pageParam = request.getParameter("page");
            String limitParam = request.getParameter("limit");
            String categoryIdParam = request.getParameter("categoryId");
            String brandIdParam = request.getParameter("brandId");
            String minPriceParam = request.getParameter("minPrice");
            String maxPriceParam = request.getParameter("maxPrice");
            String keyword = request.getParameter("keyword");
            String sortBy = request.getParameter("sortBy");
            
            // Convert parameters
            Integer page = parseInteger(pageParam, 1);
            Integer limit = parseInteger(limitParam, 12);
            Long categoryId = parseLong(categoryIdParam);
            Long brandId = parseLong(brandIdParam);
            BigDecimal minPrice = parseBigDecimal(minPriceParam);
            BigDecimal maxPrice = parseBigDecimal(maxPriceParam);
            
            // Get products from service
            Map<String, Object> result = productService.getProducts(
                    page, limit, categoryId, brandId, minPrice, maxPrice, keyword, sortBy
            );
            
            // Send success response
            sendSuccessResponse(response, "Lấy danh sách sản phẩm thành công.", 
                    result.get("products"), result.get("metadata"));
            
        } catch (Exception e) {
            logger.error("Error getting products list", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể lấy danh sách sản phẩm");
        }
    }
    
    /**
     * Handle GET /products/{id} - Lấy chi tiết sản phẩm
     */
    private void handleGetProductById(HttpServletRequest request, HttpServletResponse response,
                                      String pathInfo) throws IOException {
        
        try {
            // Extract product ID from path
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        "ID sản phẩm không hợp lệ");
                return;
            }
            
            Long productId = Long.parseLong(pathParts[1]);
            
            // Get product from service with EAGER fetch images
            Optional<Product> productOpt = productService.getProductByIdWithImages(productId);
            
            if (productOpt.isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
                        "Không tìm thấy sản phẩm với ID: " + productId);
                return;
            }
            
            Product product = productOpt.get();
            
            // Build response with full product details
            Map<String, Object> productData = buildProductDetailResponse(product);
            
            sendSuccessResponse(response, "Lấy chi tiết sản phẩm thành công.", productData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "ID sản phẩm không hợp lệ");
        } catch (Exception e) {
            logger.error("Error getting product by ID", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể lấy thông tin sản phẩm");
        }
    }
    
    /**
     * Build product detail response with all related data
     */
    private Map<String, Object> buildProductDetailResponse(Product product) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", product.getId());
        data.put("name", product.getName());
        data.put("description", product.getDescription());
        data.put("price", product.getPrice());
        data.put("stockQuantity", product.getStockQuantity());
        data.put("thumbnailUrl", product.getThumbnailUrl());
        data.put("status", product.getStatus() ? "active" : "inactive");
        data.put("specifications", product.getSpecifications());
        
        // Category info
        if (product.getCategory() != null) {
            Map<String, Object> category = new HashMap<>();
            category.put("id", product.getCategory().getId());
            category.put("name", product.getCategory().getName());
            data.put("category", category);
        }
        
        // Brand info
        if (product.getBrand() != null) {
            Map<String, Object> brand = new HashMap<>();
            brand.put("id", product.getBrand().getId());
            brand.put("name", product.getBrand().getName());
            data.put("brand", brand);
        }
        
        // Images
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            data.put("images", product.getImages());
        }
        
        // Review statistics
        Map<String, Object> reviewStats = reviewService.getProductReviewStats(product.getId());
        data.put("averageRating", reviewStats.get("averageRating"));
        data.put("reviewCount", reviewStats.get("reviewCount"));
        
        data.put("createdAt", product.getCreatedAt());
        data.put("updatedAt", product.getUpdatedAt());
        
        return data;
    }
    
    /**
     * POST /api/v1/products/{id}/reviews - Tạo đánh giá mới
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        
        try {
            // POST /products/{id}/reviews - Create review
            if (pathInfo != null && pathInfo.matches("/\\d+/reviews")) {
                handleCreateReview(request, response, pathInfo);
                return;
            }
            
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint không tồn tại");
            
        } catch (Exception e) {
            logger.error("Error in ProductController.doPost", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Lỗi hệ thống khi xử lý yêu cầu");
        }
    }
    
    /**
     * Handle GET /products/{id}/reviews - Lấy danh sách đánh giá của sản phẩm
     */
    private void handleGetProductReviews(HttpServletRequest request, HttpServletResponse response,
                                         String pathInfo) throws IOException {
        try {
            // Extract product ID from path
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID sản phẩm không hợp lệ");
                return;
            }
            
            Long productId = Long.parseLong(pathParts[1]);
            
            // Parse pagination parameters
            int page = parseInteger(request.getParameter("page"), 1);
            int limit = parseInteger(request.getParameter("limit"), 5);
            
            Map<String, Object> result = reviewService.getProductReviews(productId, page, limit);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy danh sách đánh giá thành công.");
            responseData.put("data", result.get("reviews"));
            responseData.put("metadata", result.get("metadata"));
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID sản phẩm không hợp lệ");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy sản phẩm")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    /**
     * Handle POST /products/{id}/reviews - Tạo đánh giá mới
     */
    private void handleCreateReview(HttpServletRequest request, HttpServletResponse response,
                                    String pathInfo) throws IOException {
        try {
            // Check authentication
            Long userId = RequestUtil.getCurrentUserId(request);
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Vui lòng đăng nhập để thực hiện chức năng này");
                return;
            }
            
            // Extract product ID from path
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID sản phẩm không hợp lệ");
                return;
            }
            
            Long productId = Long.parseLong(pathParts[1]);
            
            // Read request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject jsonRequest = jsonUtil.fromJson(sb.toString(), JsonObject.class);
            
            if (!jsonRequest.has("rating")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Thiếu thông tin rating");
                return;
            }
            
            Integer rating = jsonRequest.get("rating").getAsInt();
            String comment = jsonRequest.has("comment") ? jsonRequest.get("comment").getAsString() : null;
            
            Map<String, Object> reviewData = reviewService.createReview(userId, productId, rating, comment);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Gửi đánh giá thành công.");
            responseData.put("data", reviewData);
            
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID sản phẩm không hợp lệ");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy sản phẩm")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("đã đánh giá")) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
            } else if (e.getMessage().contains("phải từ 1 đến 5")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    /**
     * Send success response
     */
    private void sendSuccessResponse(HttpServletResponse response, String message,
                                     Object data) throws IOException {
        sendSuccessResponse(response, message, data, null);
    }
    
    private void sendSuccessResponse(HttpServletResponse response, String message,
                                     Object data, Object metadata) throws IOException {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", message);
        responseBody.put("data", data);
        
        if (metadata != null) {
            responseBody.put("metadata", metadata);
        }
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(jsonUtil.toJson(responseBody));
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse response, int status,
                                   String message) throws IOException {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("message", message);
        
        response.setStatus(status);
        response.getWriter().write(jsonUtil.toJson(responseBody));
    }
    
    // Helper methods for parsing parameters
    private Integer parseInteger(String value, Integer defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
