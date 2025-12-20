package com.utephonehub.service;

import com.utephonehub.dto.response.VoucherValidationResult;
import com.utephonehub.entity.Voucher;
import com.utephonehub.entity.Voucher.DiscountType;
import com.utephonehub.entity.Voucher.VoucherStatus;
import com.utephonehub.repository.VoucherRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service layer for voucher business logic
 * Handles voucher validation, discount calculation, and usage tracking
 */
public class VoucherService {
    
    private static final Logger logger = LogManager.getLogger(VoucherService.class);
    private final VoucherRepository voucherRepository;
    
    public VoucherService() {
        this.voucherRepository = new VoucherRepository();
    }
    
    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }
    
    /**
     * Validate voucher code before applying to order
     * 
     * @param code Voucher code to validate
     * @param orderTotal Total order amount before discount
     * @param userId User ID (can be null for guest checkout)
     * @return VoucherValidationResult with validation status and error message if invalid
     */
    public VoucherValidationResult validateVoucher(String code, BigDecimal orderTotal, Long userId) {
        logger.debug("Validating voucher code: {} for order total: {} and userId: {}", code, orderTotal, userId);
        
        // Check 1: Voucher exists
        Optional<Voucher> voucherOpt = voucherRepository.findByCode(code);
        if (voucherOpt.isEmpty()) {
            logger.warn("Voucher not found: {}", code);
            return VoucherValidationResult.invalid("Mã giảm giá không tồn tại");
        }
        
        Voucher voucher = voucherOpt.get();
        
        // Check 2: Voucher is active
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            logger.warn("Voucher is not active: {}", code);
            return VoucherValidationResult.invalid("Mã giảm giá không còn hiệu lực");
        }
        
        // Check 3: Not expired
        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("Voucher expired: {} (expiry date: {})", code, voucher.getExpiryDate());
            return VoucherValidationResult.invalid("Mã giảm giá đã hết hạn");
        }
        
        // Check 4: Usage limit not exceeded
        if (voucher.getMaxUsage() != null) {
            long currentUsage = voucherRepository.countVoucherUsage(voucher.getId());
            if (currentUsage >= voucher.getMaxUsage()) {
                logger.warn("Voucher usage limit exceeded: {} (current: {}, max: {})", 
                    code, currentUsage, voucher.getMaxUsage());
                return VoucherValidationResult.invalid("Mã giảm giá đã hết lượt sử dụng");
            }
        }
        
        // Check 5: Minimum order value
        if (voucher.getMinOrderValue() != null && 
            orderTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            logger.warn("Order total {} is below minimum required: {}", orderTotal, voucher.getMinOrderValue());
            return VoucherValidationResult.invalid(
                String.format("Đơn hàng tối thiểu %,.0f VNĐ để sử dụng mã này", voucher.getMinOrderValue())
            );
        }
        
        // Check 6: Per user limit (if user is logged in)
        if (userId != null) {
            long userUsageCount = voucherRepository.countUserUsage(voucher.getId(), userId);
            // Note: usageLimitPerUser is not in current Voucher entity, skip this check for now
            // Can be added later if needed
            logger.debug("User {} has used voucher {} times", userId, userUsageCount);
        }
        
        logger.info("Voucher validation successful: {}", code);
        return VoucherValidationResult.valid(voucher);
    }
    
    /**
     * Calculate discount amount based on voucher type and value
     * 
     * @param voucher Voucher to calculate discount from
     * @param orderTotal Total order amount before discount
     * @return Discount amount
     */
    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal) {
        logger.debug("Calculating discount for voucher: {} with order total: {}", 
            voucher.getCode(), orderTotal);
        
        BigDecimal discount;
        
        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            // Calculate percentage discount
            discount = orderTotal
                .multiply(voucher.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            logger.debug("Percentage discount calculated: {} ({}%)", discount, voucher.getDiscountValue());
            
            // Apply max discount cap if exists (not in current entity, but good to have)
            // Can be added later if needed
            
        } else {
            // Fixed amount discount
            discount = voucher.getDiscountValue();
            logger.debug("Fixed amount discount: {}", discount);
        }
        
        // Ensure discount doesn't exceed order total
        if (discount.compareTo(orderTotal) > 0) {
            logger.warn("Discount {} exceeds order total {}, capping to order total", discount, orderTotal);
            discount = orderTotal;
        }
        
        return discount.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Apply voucher to order and track usage
     * Should be called after order is successfully created
     * 
     * @param voucherId Voucher ID to apply
     */
    public void applyVoucher(Long voucherId) {
        logger.info("Applying voucher: {}", voucherId);
        
        try {
            voucherRepository.incrementUsedCount(voucherId);
            logger.info("Voucher usage incremented successfully: {}", voucherId);
        } catch (Exception e) {
            logger.error("Failed to increment voucher usage for voucher: {}", voucherId, e);
            // Don't throw exception, order is already created
            // Just log the error for investigation
        }
    }
    
    /**
     * Get voucher by code
     */
    public Optional<Voucher> getVoucherByCode(String code) {
        return voucherRepository.findByCode(code);
    }
    
    /**
     * Get voucher by ID
     */
    public Optional<Voucher> getVoucherById(Long id) {
        return voucherRepository.findById(id);
    }
    
    /**
     * Count how many times a voucher has been used
     * @param voucherId Voucher ID
     * @return Usage count
     */
    public long countVoucherUsage(Long voucherId) {
        return voucherRepository.countVoucherUsage(voucherId);
    }
    
    /**
     * Get all vouchers with pagination and optional status filter
     * 
     * @param page Page number (starting from 1)
     * @param limit Items per page
     * @param status Optional status filter (ACTIVE, INACTIVE, EXPIRED)
     * @return Map with vouchers list and metadata
     */
    public Map<String, Object> getAllVouchers(int page, int limit, String status) {
        logger.debug("Getting vouchers - page: {}, limit: {}, status: {}", page, limit, status);
        
        // Get vouchers from repository
        List<Voucher> allVouchers = voucherRepository.findAll();
        
        // Filter by status if provided
        if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
            if ("ACTIVE".equalsIgnoreCase(status)) {
                allVouchers = allVouchers.stream()
                    .filter(v -> v.getStatus() == VoucherStatus.ACTIVE 
                        && (v.getExpiryDate() == null || v.getExpiryDate().isAfter(LocalDateTime.now()))
                        && (v.getMaxUsage() == null || v.getOrders().size() < v.getMaxUsage()))
                    .toList();
            } else if ("EXPIRED".equalsIgnoreCase(status)) {
                allVouchers = allVouchers.stream()
                    .filter(v -> v.getStatus() == VoucherStatus.EXPIRED 
                        || (v.getExpiryDate() != null && v.getExpiryDate().isBefore(LocalDateTime.now()))
                        || (v.getMaxUsage() != null && v.getOrders().size() >= v.getMaxUsage()))
                    .toList();
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                allVouchers = allVouchers.stream()
                    .filter(v -> v.getStatus() == VoucherStatus.INACTIVE)
                    .toList();
            }
        }
        
        // Calculate pagination
        int totalItems = allVouchers.size();
        int totalPages = (int) Math.ceil((double) totalItems / limit);
        int offset = (page - 1) * limit;
        
        // Get vouchers for current page
        List<Voucher> pagedVouchers = allVouchers.stream()
            .skip(offset)
            .limit(limit)
            .toList();
        
        // Build metadata
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("totalItems", totalItems);
        pagination.put("totalPages", totalPages);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pagination", pagination);
        
        // Build result
        Map<String, Object> result = new HashMap<>();
        result.put("vouchers", pagedVouchers);
        result.put("metadata", metadata);
        
        return result;
    }
}
