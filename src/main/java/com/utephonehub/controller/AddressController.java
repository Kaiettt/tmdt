package com.utephonehub.controller;

import com.utephonehub.util.JsonUtil;

import com.google.gson.JsonObject;
import com.utephonehub.service.AddressService;
import com.utephonehub.util.RequestUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

@WebServlet("/api/v1/user/addresses/*")
public class AddressController extends HttpServlet {
    
    private final AddressService addressService;
    private final JsonUtil jsonUtil;
    
    public AddressController() {
        this.addressService = new AddressService();
        this.jsonUtil = new JsonUtil();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để xem địa chỉ");
                return;
            }
            
            List<Map<String, Object>> addresses = addressService.getUserAddresses(userId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy danh sách địa chỉ thành công.");
            responseData.put("data", addresses);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy danh sách địa chỉ: " + e.getMessage());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để thêm địa chỉ");
                return;
            }
            
            // Read request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject jsonRequest = jsonUtil.fromJson(sb.toString(), JsonObject.class);
            
            // Validate required fields
            if (!jsonRequest.has("recipientName") || !jsonRequest.has("phoneNumber") ||
                !jsonRequest.has("streetAddress")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Thiếu thông tin bắt buộc");
                return;
            }
            
            String recipientName = jsonRequest.get("recipientName").getAsString();
            String phoneNumber = jsonRequest.get("phoneNumber").getAsString();
            String streetAddress = jsonRequest.get("streetAddress").getAsString();
            
            // Get location fields (optional for backward compatibility)
            String province = jsonRequest.has("province") ? jsonRequest.get("province").getAsString() : null;
            String provinceCode = jsonRequest.has("provinceCode") ? jsonRequest.get("provinceCode").getAsString() : null;
            String ward = jsonRequest.has("ward") ? jsonRequest.get("ward").getAsString() : null;
            String wardCode = jsonRequest.has("wardCode") ? jsonRequest.get("wardCode").getAsString() : null;
            
            // Backward compatibility: use 'city' if 'province' not provided
            if (province == null && jsonRequest.has("city")) {
                province = jsonRequest.get("city").getAsString();
            }
            
            Boolean isDefault = jsonRequest.has("isDefault") ? 
                jsonRequest.get("isDefault").getAsBoolean() : false;
            
            Map<String, Object> addressData = addressService.createAddress(
                userId, recipientName, phoneNumber, streetAddress, 
                province, provinceCode, ward, wardCode, isDefault);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Thêm địa chỉ thành công.");
            responseData.put("data", addressData);
            
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi thêm địa chỉ: " + e.getMessage());
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        
        try {
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để cập nhật địa chỉ");
                return;
            }
            
            // Extract addressId from path
            if (pathInfo == null || pathInfo.equals("/")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Thiếu ID địa chỉ");
                return;
            }
            
            String addressIdStr = pathInfo.substring(1); // Remove leading /
            Long addressId = Long.parseLong(addressIdStr);
            
            // Read request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject jsonRequest = jsonUtil.fromJson(sb.toString(), JsonObject.class);
            
            String recipientName = jsonRequest.has("recipientName") ? 
                jsonRequest.get("recipientName").getAsString() : null;
            String phoneNumber = jsonRequest.has("phoneNumber") ? 
                jsonRequest.get("phoneNumber").getAsString() : null;
            String streetAddress = jsonRequest.has("streetAddress") ? 
                jsonRequest.get("streetAddress").getAsString() : null;
            
            // Get location fields
            String province = jsonRequest.has("province") ? 
                jsonRequest.get("province").getAsString() : null;
            String provinceCode = jsonRequest.has("provinceCode") ? 
                jsonRequest.get("provinceCode").getAsString() : null;
            String ward = jsonRequest.has("ward") ? 
                jsonRequest.get("ward").getAsString() : null;
            String wardCode = jsonRequest.has("wardCode") ? 
                jsonRequest.get("wardCode").getAsString() : null;
            
            // Backward compatibility: use 'city' if 'province' not provided
            if (province == null && jsonRequest.has("city")) {
                province = jsonRequest.get("city").getAsString();
            }
            
            Boolean isDefault = jsonRequest.has("isDefault") ? 
                jsonRequest.get("isDefault").getAsBoolean() : null;
            
            Map<String, Object> addressData = addressService.updateAddress(
                userId, addressId, recipientName, phoneNumber, streetAddress, 
                province, provinceCode, ward, wardCode, isDefault);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Cập nhật địa chỉ thành công.");
            responseData.put("data", addressData);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "ID địa chỉ không hợp lệ");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else {
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi cập nhật địa chỉ: " + e.getMessage());
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        
        try {
            Long userId = getUserIdFromRequest(request);
            
            if (userId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Vui lòng đăng nhập để xóa địa chỉ");
                return;
            }
            
            // Extract addressId from path
            if (pathInfo == null || pathInfo.equals("/")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Thiếu ID địa chỉ");
                return;
            }
            
            String addressIdStr = pathInfo.substring(1);
            Long addressId = Long.parseLong(addressIdStr);
            
            addressService.deleteAddress(userId, addressId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Xóa địa chỉ thành công.");
            responseData.put("data", null);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonUtil.toJson(responseData));
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "ID địa chỉ không hợp lệ");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Không tìm thấy")) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("mặc định")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else {
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi xóa địa chỉ: " + e.getMessage());
        }
    }
    
    /**
     * Get user ID from JWT token (via request attribute)
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        return RequestUtil.getCurrentUserId(request);
    }
    
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) 
            throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        
        response.setStatus(statusCode);
        response.getWriter().write(jsonUtil.toJson(errorResponse));
    }
}

