package com.utephonehub.controller;

import com.google.gson.Gson;
import com.utephonehub.dto.ApiResponse;
import com.utephonehub.service.ProvinceWardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Province and Ward API Controller
 * Endpoints:
 * - GET /api/v1/location/provinces - Get all provinces
 * - GET /api/v1/location/provinces/{code} - Get province by code
 * - GET /api/v1/location/wards - Get all wards
 * - GET /api/v1/location/wards/{code} - Get ward by code
 * - GET /api/v1/location/provinces/{code}/wards - Get wards by province
 */
@WebServlet("/api/v1/location/*")
public class LocationController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(LocationController.class);
    private final Gson gson = new Gson();
    private final ProvinceWardService locationService = new ProvinceWardService();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            
            if (pathParts.length < 2) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid endpoint");
                return;
            }
            
            String resource = pathParts[1]; // provinces or wards
            
            switch (resource) {
                case "provinces":
                    handleProvinceRequests(pathParts, response);
                    break;
                case "wards":
                    handleWardRequests(pathParts, response);
                    break;
                default:
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Resource not found");
            }
            
        } catch (Exception e) {
            logger.error("Error in location API", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Có lỗi xảy ra, vui lòng thử lại sau");
        }
    }
    
    /**
     * Handle province requests
     * - /provinces - Get all provinces
     * - /provinces/{code} - Get province by code
     * - /provinces/{code}/wards - Get wards by province
     */
    private void handleProvinceRequests(String[] pathParts, HttpServletResponse response)
            throws IOException {
        
        if (pathParts.length == 2) {
            // GET /provinces - Get all provinces
            List<ProvinceWardService.ProvinceDto> provinces = locationService.getAllProvinces();
            sendSuccessResponse(response, "Danh sách tỉnh/thành phố", provinces);
            
        } else if (pathParts.length == 3) {
            // GET /provinces/{code} - Get province by code
            String code = pathParts[2];
            ProvinceWardService.ProvinceDto province = locationService.getProvinceByCode(code);
            
            if (province == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tỉnh/thành phố");
                return;
            }
            
            sendSuccessResponse(response, "Thông tin tỉnh/thành phố", province);
            
        } else if (pathParts.length == 4 && "wards".equals(pathParts[3])) {
            // GET /provinces/{code}/wards - Get wards by province
            String provinceCode = pathParts[2];
            List<ProvinceWardService.WardDto> wards = locationService.getWardsByProvinceCode(provinceCode);
            sendSuccessResponse(response, "Danh sách xã/phường", wards);
            
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid province endpoint");
        }
    }
    
    /**
     * Handle ward requests
     * - /wards - Get all wards
     * - /wards/{code} - Get ward by code
     */
    private void handleWardRequests(String[] pathParts, HttpServletResponse response)
            throws IOException {
        
        if (pathParts.length == 2) {
            // GET /wards - Get all wards
            List<ProvinceWardService.WardDto> wards = locationService.getAllWards();
            sendSuccessResponse(response, "Danh sách xã/phường", wards);
            
        } else if (pathParts.length == 3) {
            // GET /wards/{code} - Get ward by code
            String code = pathParts[2];
            ProvinceWardService.WardDto ward = locationService.getWardByCode(code);
            
            if (ward == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy xã/phường");
                return;
            }
            
            sendSuccessResponse(response, "Thông tin xã/phường", ward);
            
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid ward endpoint");
        }
    }
    
    /**
     * Send success response
     */
    private void sendSuccessResponse(HttpServletResponse response, String message, Object data)
            throws IOException {
        ApiResponse apiResponse = new ApiResponse(true, message, data);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(apiResponse));
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message)
            throws IOException {
        ApiResponse apiResponse = new ApiResponse(false, message, null);
        response.setStatus(statusCode);
        response.getWriter().write(gson.toJson(apiResponse));
    }
}