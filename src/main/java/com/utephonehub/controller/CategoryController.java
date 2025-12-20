package com.utephonehub.controller;

import com.utephonehub.dto.response.CategoryResponse;
import com.utephonehub.service.CategoryService;
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

/**
 * Controller for Category operations
 * Handles public endpoints for categories
 */
@WebServlet("/api/v1/categories")
public class CategoryController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(CategoryController.class);
    private final CategoryService categoryService;
    private final JsonUtil jsonUtil;
    
    public CategoryController() {
        this.categoryService = new CategoryService();
        this.jsonUtil = new JsonUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        logger.info("CategoryController GET request: {}", pathInfo);
        
        try {
            // GET /api/v1/categories/{id} - Get category by ID
            if (pathInfo != null && !pathInfo.equals("/")) {
                handleGetCategoryById(request, response, pathInfo);
            } 
            // GET /api/v1/categories - Get all categories
            else {
                handleGetAllCategories(request, response);
            }
            
        } catch (Exception e) {
            logger.error("Error in CategoryController", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi xử lý yêu cầu");
        }
    }
    
    /**
     * Handle GET /api/v1/categories - Get all categories
     */
    private void handleGetAllCategories(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        List<CategoryResponse> categories = categoryService.getAllCategories();
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", "Lấy danh sách danh mục thành công.");
        responseData.put("data", categories);
        
        sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
    }
    
    /**
     * Handle GET /api/v1/categories/{id} - Get category by ID
     */
    private void handleGetCategoryById(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            // Extract category ID from path
            String categoryIdStr = pathInfo.substring(1); // Remove leading "/"
            Long categoryId = Long.parseLong(categoryIdStr);
            
            // Get category by ID
            CategoryResponse category = categoryService.getCategoryById(categoryId);
            
            if (category == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                    "Không tìm thấy danh mục với ID: " + categoryId);
                return;
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy thông tin danh mục thành công.");
            responseData.put("data", category);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            logger.error("Invalid category ID format: {}", pathInfo, e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "ID danh mục không hợp lệ");
        }
    }
    
    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpServletResponse response, int statusCode, Map<String, Object> data) 
            throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonUtil.toJson(data));
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) 
            throws IOException {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("message", message);
        sendJsonResponse(response, statusCode, errorData);
    }
}
