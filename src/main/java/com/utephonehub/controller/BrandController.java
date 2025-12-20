package com.utephonehub.controller;

import com.utephonehub.dto.response.BrandResponse;
import com.utephonehub.service.BrandService;
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
 * Controller for Brand operations
 * Handles public endpoints for brands
 */
@WebServlet("/api/v1/brands")
public class BrandController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(BrandController.class);
    private final BrandService brandService;
    private final JsonUtil jsonUtil;
    
    public BrandController() {
        this.brandService = new BrandService();
        this.jsonUtil = new JsonUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        logger.info("BrandController GET request: {}", pathInfo);
        
        try {
            // GET /api/v1/brands/{id} - Get brand by ID
            if (pathInfo != null && !pathInfo.equals("/")) {
                handleGetBrandById(request, response, pathInfo);
            } 
            // GET /api/v1/brands - Get all brands
            else {
                handleGetAllBrands(request, response);
            }
            
        } catch (Exception e) {
            logger.error("Error in BrandController", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi xử lý yêu cầu");
        }
    }
    
    /**
     * Handle GET /api/v1/brands - Get all brands
     */
    private void handleGetAllBrands(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        List<BrandResponse> brands = brandService.getAllBrands();
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", "Lấy danh sách thương hiệu thành công.");
        responseData.put("data", brands);
        
        sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
    }
    
    /**
     * Handle GET /api/v1/brands/{id} - Get brand by ID
     */
    private void handleGetBrandById(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            // Extract brand ID from path
            String brandIdStr = pathInfo.substring(1); // Remove leading "/"
            Long brandId = Long.parseLong(brandIdStr);
            
            // Get brand by ID
            BrandResponse brand = brandService.getBrandById(brandId);
            
            if (brand == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                    "Không tìm thấy thương hiệu với ID: " + brandId);
                return;
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy thông tin thương hiệu thành công.");
            responseData.put("data", brand);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            logger.error("Invalid brand ID format: {}", pathInfo, e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "ID thương hiệu không hợp lệ");
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
