package com.utephonehub.controller;

import com.utephonehub.dto.request.CategoryRequest;
import com.utephonehub.dto.response.CategoryResponse;
import com.utephonehub.entity.Category;
import com.utephonehub.entity.User;
import com.utephonehub.repository.CategoryRepository;
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
 * Admin Category Management Controller
 * Quản lý danh mục - chỉ dành cho admin
 */
@WebServlet("/api/v1/admin/categories/*")
public class AdminCategoryController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(AdminCategoryController.class);
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final JsonUtil jsonUtil;
    
    public AdminCategoryController() {
        this.categoryRepository = new CategoryRepository();
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
        logger.info("AdminCategoryController GET request: {}", pathInfo);
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/v1/admin/categories - Lấy danh sách categories
                handleGetCategories(request, response);
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/v1/admin/categories/{id} - Chi tiết category
                handleGetCategoryDetail(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in AdminCategoryController GET", e);
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
        
        logger.info("AdminCategoryController POST request");
        
        try {
            // POST /api/v1/admin/categories - Tạo category mới
            handleCreateCategory(request, response);
        } catch (Exception e) {
            logger.error("Error in AdminCategoryController POST", e);
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
        logger.info("AdminCategoryController PUT request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // PUT /api/v1/admin/categories/{id} - Cập nhật category
                handleUpdateCategory(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Category ID is required");
            }
        } catch (Exception e) {
            logger.error("Error in AdminCategoryController PUT", e);
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
        logger.info("AdminCategoryController DELETE request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // DELETE /api/v1/admin/categories/{id} - Xóa category
                handleDeleteCategory(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Category ID is required");
            }
        } catch (Exception e) {
            logger.error("Error in AdminCategoryController DELETE", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * Lấy danh sách categories
     */
    private void handleGetCategories(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            List<Category> categories = categoryRepository.findAll();
            
            // Convert to response DTOs
            List<CategoryResponse> categoryResponses = categories.stream()
                .map(this::convertToCategoryResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy danh sách danh mục thành công");
            responseData.put("data", categoryResponses);
            responseData.put("total", categoryResponses.size());
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (Exception e) {
            logger.error("Error getting categories", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy danh sách danh mục");
        }
    }
    
    /**
     * Lấy chi tiết category
     */
    private void handleGetCategoryDetail(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long categoryId = Long.parseLong(pathInfo.substring(1));
            
            Category category = categoryRepository.findById(categoryId)
                .orElse(null);
            
            if (category == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy danh mục");
                return;
            }
            
            CategoryResponse categoryResponse = convertToCategoryResponse(category);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy thông tin danh mục thành công");
            responseData.put("data", categoryResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Category ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error getting category detail", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy thông tin danh mục");
        }
    }
    
    /**
     * Tạo category mới
     */
    private void handleCreateCategory(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Parse request body
            CategoryRequest categoryRequest = jsonUtil.parseJson(request, CategoryRequest.class);
            
            // Validate required fields
            if (categoryRequest.getName() == null || categoryRequest.getName().trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Tên danh mục không được để trống");
                return;
            }
            
            // Check name uniqueness
            if (categoryRepository.findByName(categoryRequest.getName()).isPresent()) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Tên danh mục đã tồn tại");
                return;
            }
            
            // Create new category
            Category category = new Category();
            category.setName(categoryRequest.getName());
            category.setDescription(categoryRequest.getDescription());
            
            // Set parent if provided
            if (categoryRequest.getParentId() != null) {
                Category parent = categoryRepository.findById(categoryRequest.getParentId())
                    .orElse(null);
                if (parent == null) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Danh mục cha không tồn tại");
                    return;
                }
                category.setParent(parent);
            }
            
            // Save category
            Category savedCategory = categoryRepository.save(category);
            
            // Re-fetch với parent relation để tránh LazyInitializationException
            savedCategory = categoryRepository.findByIdWithParent(savedCategory.getId())
                .orElseThrow(() -> new RuntimeException("Category not found after save"));
            
            CategoryResponse categoryResponse = convertToCategoryResponse(savedCategory);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Tạo danh mục thành công");
            responseData.put("data", categoryResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, responseData);
            
        } catch (Exception e) {
            logger.error("Error creating category", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi tạo danh mục");
        }
    }
    
    /**
     * Cập nhật category
     */
    private void handleUpdateCategory(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long categoryId = Long.parseLong(pathInfo.substring(1));
            
            // Check category exists
            Category category = categoryRepository.findById(categoryId)
                .orElse(null);
            
            if (category == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy danh mục");
                return;
            }
            
            // Parse request body
            CategoryRequest categoryRequest = jsonUtil.parseJson(request, CategoryRequest.class);
            
            // Update fields if provided
            if (categoryRequest.getName() != null && !categoryRequest.getName().trim().isEmpty()) {
                // Check name uniqueness (except current category)
                var existingCategory = categoryRepository.findByName(categoryRequest.getName());
                if (existingCategory.isPresent() && !existingCategory.get().getId().equals(categoryId)) {
                    sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Tên danh mục đã tồn tại");
                    return;
                }
                category.setName(categoryRequest.getName());
            }
            
            if (categoryRequest.getDescription() != null) {
                category.setDescription(categoryRequest.getDescription());
            }
            
            // Update parent if provided
            if (categoryRequest.getParentId() != null) {
                if (categoryRequest.getParentId().equals(categoryId)) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "Danh mục không thể là cha của chính nó");
                    return;
                }
                
                Category parent = categoryRepository.findById(categoryRequest.getParentId())
                    .orElse(null);
                if (parent == null) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Danh mục cha không tồn tại");
                    return;
                }
                category.setParent(parent);
            }
            
            // Save updated category
            Category updatedCategory = categoryRepository.save(category);
            
            // Re-fetch với parent relation để tránh LazyInitializationException
            updatedCategory = categoryRepository.findByIdWithParent(updatedCategory.getId())
                .orElseThrow(() -> new RuntimeException("Category not found after save"));
            
            CategoryResponse categoryResponse = convertToCategoryResponse(updatedCategory);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Cập nhật danh mục thành công");
            responseData.put("data", categoryResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Category ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error updating category", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi cập nhật danh mục");
        }
    }
    
    /**
     * Xóa category
     */
    private void handleDeleteCategory(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long categoryId = Long.parseLong(pathInfo.substring(1));
            
            // Check category exists
            Category category = categoryRepository.findById(categoryId)
                .orElse(null);
            
            if (category == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy danh mục");
                return;
            }
            
            // Check if category has products - sử dụng count query thay vì lazy load
            long productCount = categoryRepository.countProductsByCategoryId(categoryId);
            if (productCount > 0) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Không thể xóa danh mục đang có " + productCount + " sản phẩm");
                return;
            }
            
            // Check if category has children - sử dụng count query thay vì lazy load
            long childrenCount = categoryRepository.countChildrenByCategoryId(categoryId);
            if (childrenCount > 0) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Không thể xóa danh mục đang có " + childrenCount + " danh mục con");
                return;
            }
            
            // Delete category
            categoryRepository.deleteById(categoryId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Xóa danh mục thành công");
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Category ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error deleting category", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi xóa danh mục");
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
     * Convert Category entity to CategoryResponse DTO
     */
    private CategoryResponse convertToCategoryResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        response.setParentName(category.getParent() != null ? category.getParent().getName() : null);
        // Query counts from database instead of accessing lazy collections
        response.setProductCount((int) categoryRepository.countProductsByCategoryId(category.getId()));
        response.setChildrenCount((int) categoryRepository.countChildrenByCategoryId(category.getId()));
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
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
