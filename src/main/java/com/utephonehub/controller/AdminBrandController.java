package com.utephonehub.controller;

import com.utephonehub.dto.request.BrandRequest;
import com.utephonehub.dto.response.BrandResponse;
import com.utephonehub.entity.Brand;
import com.utephonehub.entity.User;
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
 * Admin Brand Management Controller
 * Quản lý thương hiệu - chỉ dành cho admin
 */
@WebServlet("/api/v1/admin/brands/*")
public class AdminBrandController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(AdminBrandController.class);
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final JsonUtil jsonUtil;
    
    public AdminBrandController() {
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
        logger.info("AdminBrandController GET request: {}", pathInfo);
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/v1/admin/brands - Lấy danh sách brands
                handleGetBrands(request, response);
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/v1/admin/brands/{id} - Chi tiết brand
                handleGetBrandDetail(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in AdminBrandController GET", e);
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
        
        logger.info("AdminBrandController POST request");
        
        try {
            // POST /api/v1/admin/brands - Tạo brand mới
            handleCreateBrand(request, response);
        } catch (Exception e) {
            logger.error("Error in AdminBrandController POST", e);
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
        logger.info("AdminBrandController PUT request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // PUT /api/v1/admin/brands/{id} - Cập nhật brand
                handleUpdateBrand(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Brand ID is required");
            }
        } catch (Exception e) {
            logger.error("Error in AdminBrandController PUT", e);
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
        logger.info("AdminBrandController DELETE request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // DELETE /api/v1/admin/brands/{id} - Xóa brand
                handleDeleteBrand(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Brand ID is required");
            }
        } catch (Exception e) {
            logger.error("Error in AdminBrandController DELETE", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * Lấy danh sách brands
     */
    private void handleGetBrands(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            List<Brand> brands = brandRepository.findAll();
            
            // Convert to response DTOs
            List<BrandResponse> brandResponses = brands.stream()
                .map(this::convertToBrandResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy danh sách thương hiệu thành công");
            responseData.put("data", brandResponses);
            responseData.put("total", brandResponses.size());
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (Exception e) {
            logger.error("Error getting brands", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy danh sách thương hiệu");
        }
    }
    
    /**
     * Lấy chi tiết brand
     */
    private void handleGetBrandDetail(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long brandId = Long.parseLong(pathInfo.substring(1));
            
            Brand brand = brandRepository.findById(brandId)
                .orElse(null);
            
            if (brand == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thương hiệu");
                return;
            }
            
            BrandResponse brandResponse = convertToBrandResponse(brand);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy thông tin thương hiệu thành công");
            responseData.put("data", brandResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Brand ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error getting brand detail", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy thông tin thương hiệu");
        }
    }
    
    /**
     * Tạo brand mới
     */
    private void handleCreateBrand(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Parse request body
            BrandRequest brandRequest = jsonUtil.parseJson(request, BrandRequest.class);
            
            // Validate required fields
            if (brandRequest.getName() == null || brandRequest.getName().trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Tên thương hiệu không được để trống");
                return;
            }
            
            // Check name uniqueness
            if (brandRepository.findByName(brandRequest.getName()).isPresent()) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Tên thương hiệu đã tồn tại");
                return;
            }
            
            // Create new brand
            Brand brand = new Brand();
            brand.setName(brandRequest.getName());
            brand.setDescription(brandRequest.getDescription());
            brand.setLogoUrl(brandRequest.getLogoUrl());
            
            // Save brand
            Brand savedBrand = brandRepository.save(brand);
            BrandResponse brandResponse = convertToBrandResponse(savedBrand);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Tạo thương hiệu thành công");
            responseData.put("data", brandResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, responseData);
            
        } catch (Exception e) {
            logger.error("Error creating brand", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi tạo thương hiệu");
        }
    }
    
    /**
     * Cập nhật brand
     */
    private void handleUpdateBrand(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long brandId = Long.parseLong(pathInfo.substring(1));
            
            // Check brand exists
            Brand brand = brandRepository.findById(brandId)
                .orElse(null);
            
            if (brand == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thương hiệu");
                return;
            }
            
            // Parse request body
            BrandRequest brandRequest = jsonUtil.parseJson(request, BrandRequest.class);
            
            // Update fields if provided
            if (brandRequest.getName() != null && !brandRequest.getName().trim().isEmpty()) {
                // Check name uniqueness (except current brand)
                var existingBrand = brandRepository.findByName(brandRequest.getName());
                if (existingBrand.isPresent() && !existingBrand.get().getId().equals(brandId)) {
                    sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Tên thương hiệu đã tồn tại");
                    return;
                }
                brand.setName(brandRequest.getName());
            }
            
            if (brandRequest.getDescription() != null) {
                brand.setDescription(brandRequest.getDescription());
            }
            
            if (brandRequest.getLogoUrl() != null) {
                brand.setLogoUrl(brandRequest.getLogoUrl());
            }
            
            // Save updated brand
            Brand updatedBrand = brandRepository.save(brand);
            BrandResponse brandResponse = convertToBrandResponse(updatedBrand);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Cập nhật thương hiệu thành công");
            responseData.put("data", brandResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Brand ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error updating brand", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi cập nhật thương hiệu");
        }
    }
    
    /**
     * Xóa brand
     */
    private void handleDeleteBrand(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long brandId = Long.parseLong(pathInfo.substring(1));
            
            // Check brand exists
            Brand brand = brandRepository.findById(brandId)
                .orElse(null);
            
            if (brand == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thương hiệu");
                return;
            }
            
            // Check if brand has products - query database instead of accessing lazy collection
            long productCount = brandRepository.countProductsByBrandId(brandId);
            if (productCount > 0) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Không thể xóa thương hiệu đang có sản phẩm");
                return;
            }
            
            // Delete brand
            brandRepository.deleteById(brandId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Xóa thương hiệu thành công");
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Brand ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error deleting brand", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi xóa thương hiệu");
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
     * Convert Brand entity to BrandResponse DTO
     */
    private BrandResponse convertToBrandResponse(Brand brand) {
        BrandResponse response = new BrandResponse();
        response.setId(brand.getId());
        response.setName(brand.getName());
        response.setDescription(brand.getDescription());
        response.setLogoUrl(brand.getLogoUrl());
        // Query count from database instead of accessing lazy collection
        response.setProductCount((int) brandRepository.countProductsByBrandId(brand.getId()));
        response.setCreatedAt(brand.getCreatedAt());
        response.setUpdatedAt(brand.getUpdatedAt());
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
