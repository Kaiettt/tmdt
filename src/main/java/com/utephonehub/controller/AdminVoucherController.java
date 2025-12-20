package com.utephonehub.controller;

import com.utephonehub.dto.request.VoucherRequest;
import com.utephonehub.dto.response.VoucherResponse;
import com.utephonehub.entity.User;
import com.utephonehub.entity.Voucher;
import com.utephonehub.repository.UserRepository;
import com.utephonehub.repository.VoucherRepository;
import com.utephonehub.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Voucher Management Controller
 * Quản lý vouchers - chỉ dành cho admin
 */
@WebServlet("/api/v1/admin/vouchers/*")
public class AdminVoucherController extends HttpServlet {
    
    private static final Logger logger = LogManager.getLogger(AdminVoucherController.class);
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final JsonUtil jsonUtil;
    
    public AdminVoucherController() {
        this.voucherRepository = new VoucherRepository();
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
        logger.info("AdminVoucherController GET request: {}", pathInfo);
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/v1/admin/vouchers - Lấy danh sách vouchers
                handleGetVouchers(request, response);
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/v1/admin/vouchers/{id} - Chi tiết voucher
                handleGetVoucherDetail(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            }
        } catch (Exception e) {
            logger.error("Error in AdminVoucherController GET", e);
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
        
        logger.info("AdminVoucherController POST request");
        
        try {
            // POST /api/v1/admin/vouchers - Tạo voucher mới
            handleCreateVoucher(request, response);
        } catch (Exception e) {
            logger.error("Error in AdminVoucherController POST", e);
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
        logger.info("AdminVoucherController PUT request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // PUT /api/v1/admin/vouchers/{id} - Cập nhật voucher
                handleUpdateVoucher(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Voucher ID is required");
            }
        } catch (Exception e) {
            logger.error("Error in AdminVoucherController PUT", e);
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
        logger.info("AdminVoucherController DELETE request: {}", pathInfo);
        
        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // DELETE /api/v1/admin/vouchers/{id} - Xóa voucher
                handleDeleteVoucher(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Voucher ID is required");
            }
        } catch (Exception e) {
            logger.error("Error in AdminVoucherController DELETE", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * Lấy danh sách vouchers với filters
     */
    private void handleGetVouchers(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Get query parameters
            String statusStr = request.getParameter("status");
            String discountTypeStr = request.getParameter("discountType");
            
            List<Voucher> vouchers = voucherRepository.findAll();
            
            // Filter by status if provided
            if (statusStr != null && !statusStr.isEmpty()) {
                try {
                    Voucher.VoucherStatus status = Voucher.VoucherStatus.valueOf(statusStr.toUpperCase());
                    vouchers = vouchers.stream()
                        .filter(v -> v.getStatus() == status)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid voucher status: {}", statusStr);
                }
            }
            
            // Filter by discount type if provided
            if (discountTypeStr != null && !discountTypeStr.isEmpty()) {
                try {
                    Voucher.DiscountType discountType = Voucher.DiscountType.valueOf(discountTypeStr.toUpperCase());
                    vouchers = vouchers.stream()
                        .filter(v -> v.getDiscountType() == discountType)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid discount type: {}", discountTypeStr);
                }
            }
            
            // Convert to response DTOs
            List<VoucherResponse> voucherResponses = vouchers.stream()
                .map(this::convertToVoucherResponse)
                .collect(Collectors.toList());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy danh sách voucher thành công");
            responseData.put("data", voucherResponses);
            responseData.put("total", voucherResponses.size());
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (Exception e) {
            logger.error("Error getting vouchers", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy danh sách voucher");
        }
    }
    
    /**
     * Lấy chi tiết voucher
     */
    private void handleGetVoucherDetail(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long voucherId = Long.parseLong(pathInfo.substring(1));
            
            Voucher voucher = voucherRepository.findById(voucherId)
                .orElse(null);
            
            if (voucher == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy voucher");
                return;
            }
            
            VoucherResponse voucherResponse = convertToVoucherResponse(voucher);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Lấy thông tin voucher thành công");
            responseData.put("data", voucherResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Voucher ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error getting voucher detail", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy thông tin voucher");
        }
    }
    
    /**
     * Tạo voucher mới
     */
    private void handleCreateVoucher(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        try {
            // Parse request body
            VoucherRequest voucherRequest = jsonUtil.parseJson(request, VoucherRequest.class);
            
            // Validate required fields
            if (voucherRequest.getCode() == null || voucherRequest.getCode().trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Mã voucher không được để trống");
                return;
            }
            
            if (voucherRequest.getDiscountType() == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Loại giảm giá không được để trống");
                return;
            }
            
            if (voucherRequest.getDiscountValue() == null || voucherRequest.getDiscountValue().doubleValue() <= 0) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Giá trị giảm giá không hợp lệ");
                return;
            }
            
            // Validate discount type and value
            if (voucherRequest.getDiscountType() == Voucher.DiscountType.PERCENTAGE) {
                if (voucherRequest.getDiscountValue().doubleValue() > 100) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "Phần trăm giảm giá không được vượt quá 100%");
                    return;
                }
            }
            
            // Check voucher code uniqueness
            if (voucherRepository.findByCode(voucherRequest.getCode()).isPresent()) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Mã voucher đã tồn tại");
                return;
            }
            
            // Create new voucher
            Voucher voucher = new Voucher();
            voucher.setCode(voucherRequest.getCode().toUpperCase());
            voucher.setDiscountType(voucherRequest.getDiscountType());
            voucher.setDiscountValue(voucherRequest.getDiscountValue());
            
            // Map usageLimit → maxUsage
            voucher.setMaxUsage(voucherRequest.getUsageLimit());
            
            voucher.setMinOrderValue(voucherRequest.getMinOrderValue());
            
            // Map endDate → expiryDate
            voucher.setExpiryDate(voucherRequest.getEndDate());
            
            // Map isActive → status
            if (voucherRequest.getIsActive() != null) {
                voucher.setStatus(voucherRequest.getIsActive() 
                    ? Voucher.VoucherStatus.ACTIVE 
                    : Voucher.VoucherStatus.INACTIVE);
            } else {
                voucher.setStatus(Voucher.VoucherStatus.ACTIVE); // Default
            }
            
            // Save voucher
            Voucher savedVoucher = voucherRepository.save(voucher);
            VoucherResponse voucherResponse = convertToVoucherResponse(savedVoucher);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Tạo voucher thành công");
            responseData.put("data", voucherResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_CREATED, responseData);
            
        } catch (Exception e) {
            logger.error("Error creating voucher", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi tạo voucher");
        }
    }
    
    /**
     * Cập nhật voucher
     */
    private void handleUpdateVoucher(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long voucherId = Long.parseLong(pathInfo.substring(1));
            
            // Check voucher exists
            Voucher voucher = voucherRepository.findById(voucherId)
                .orElse(null);
            
            if (voucher == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy voucher");
                return;
            }
            
            // Parse request body
            VoucherRequest voucherRequest = jsonUtil.parseJson(request, VoucherRequest.class);
            
            // Update fields if provided
            if (voucherRequest.getCode() != null && !voucherRequest.getCode().trim().isEmpty()) {
                // Check code uniqueness (except current voucher)
                var existingVoucher = voucherRepository.findByCode(voucherRequest.getCode().toUpperCase());
                if (existingVoucher.isPresent() && !existingVoucher.get().getId().equals(voucherId)) {
                    sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, "Mã voucher đã tồn tại");
                    return;
                }
                voucher.setCode(voucherRequest.getCode().toUpperCase());
            }
            
            if (voucherRequest.getDiscountType() != null) {
                voucher.setDiscountType(voucherRequest.getDiscountType());
            }
            
            if (voucherRequest.getDiscountValue() != null && voucherRequest.getDiscountValue().doubleValue() > 0) {
                // Validate percentage
                if (voucher.getDiscountType() == Voucher.DiscountType.PERCENTAGE 
                    && voucherRequest.getDiscountValue().doubleValue() > 100) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "Phần trăm giảm giá không được vượt quá 100%");
                    return;
                }
                voucher.setDiscountValue(voucherRequest.getDiscountValue());
            }
            
            if (voucherRequest.getUsageLimit() != null) {
                voucher.setMaxUsage(voucherRequest.getUsageLimit());
            }
            
            if (voucherRequest.getMinOrderValue() != null) {
                voucher.setMinOrderValue(voucherRequest.getMinOrderValue());
            }
            
            if (voucherRequest.getEndDate() != null) {
                voucher.setExpiryDate(voucherRequest.getEndDate());
            }
            
            // Map isActive → status
            if (voucherRequest.getIsActive() != null) {
                voucher.setStatus(voucherRequest.getIsActive() 
                    ? Voucher.VoucherStatus.ACTIVE 
                    : Voucher.VoucherStatus.INACTIVE);
            }
            
            // Save updated voucher
            Voucher updatedVoucher = voucherRepository.save(voucher);
            VoucherResponse voucherResponse = convertToVoucherResponse(updatedVoucher);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Cập nhật voucher thành công");
            responseData.put("data", voucherResponse);
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Voucher ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error updating voucher", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi cập nhật voucher");
        }
    }
    
    /**
     * Xóa voucher
     */
    private void handleDeleteVoucher(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            Long voucherId = Long.parseLong(pathInfo.substring(1));
            
            // Check voucher exists
            Voucher voucher = voucherRepository.findById(voucherId)
                .orElse(null);
            
            if (voucher == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy voucher");
                return;
            }
            
            // Soft delete - set status to INACTIVE
            voucher.setStatus(Voucher.VoucherStatus.INACTIVE);
            voucherRepository.save(voucher);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Xóa voucher thành công");
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Voucher ID không hợp lệ");
        } catch (Exception e) {
            logger.error("Error deleting voucher", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi xóa voucher");
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
     * Convert Voucher entity to VoucherResponse DTO
     * Maps entity fields to frontend-expected field names
     */
    private VoucherResponse convertToVoucherResponse(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setDescription("Voucher giảm giá đặc biệt"); // Default description - not in entity
        response.setDiscountType(voucher.getDiscountType().toString());
        response.setDiscountValue(voucher.getDiscountValue());
        
        // Map maxUsage → usageLimit (frontend expects this name)
        response.setUsageLimit(voucher.getMaxUsage());
        
        // Query usage count from database instead of accessing lazy collection
        // Map to usedCount (frontend expects this name)
        response.setUsedCount((int) voucherRepository.countVoucherUsage(voucher.getId()));
        
        response.setMinOrderValue(voucher.getMinOrderValue());
        response.setMaxDiscountAmount(null); // Not available in entity - future feature
        
        // Map createdAt → startDate (frontend expects this)
        response.setStartDate(voucher.getCreatedAt());
        
        // Map expiryDate → endDate (frontend expects this)
        response.setEndDate(voucher.getExpiryDate());
        
        // Map status ENUM → isActive boolean (frontend expects this)
        response.setIsActive(voucher.getStatus() == Voucher.VoucherStatus.ACTIVE);
        
        // Keep status for reference
        response.setStatus(voucher.getStatus().toString());
        
        response.setCreatedAt(voucher.getCreatedAt());
        response.setUpdatedAt(voucher.getUpdatedAt());
        
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
