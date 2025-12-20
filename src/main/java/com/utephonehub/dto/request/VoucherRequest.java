package com.utephonehub.dto.request;

import com.utephonehub.entity.Voucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Voucher Request DTO
 * Dùng cho create và update voucher
 * Fields match frontend naming conventions
 */
public class VoucherRequest {
    
    private String code;
    private String description;
    
    private Voucher.DiscountType discountType;
    private BigDecimal discountValue;
    
    // Frontend sends: usageLimit
    private Integer usageLimit;
    
    private Integer usageLimitPerUser; // Future feature
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount; // Future feature
    
    // Frontend sends: startDate, endDate
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // Frontend sends: isActive (boolean)
    private Boolean isActive;
    
    private Voucher.VoucherStatus status;
    
    // Constructors
    public VoucherRequest() {
    }
    
    // Getters and Setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Voucher.DiscountType getDiscountType() {
        return discountType;
    }
    
    public void setDiscountType(Voucher.DiscountType discountType) {
        this.discountType = discountType;
    }
    
    public BigDecimal getDiscountValue() {
        return discountValue;
    }
    
    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }
    
    public Integer getUsageLimit() {
        return usageLimit;
    }
    
    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }
    
    public Integer getUsageLimitPerUser() {
        return usageLimitPerUser;
    }
    
    public void setUsageLimitPerUser(Integer usageLimitPerUser) {
        this.usageLimitPerUser = usageLimitPerUser;
    }
    
    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }
    
    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }
    
    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }
    
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Voucher.VoucherStatus getStatus() {
        return status;
    }
    
    public void setStatus(Voucher.VoucherStatus status) {
        this.status = status;
    }
}
