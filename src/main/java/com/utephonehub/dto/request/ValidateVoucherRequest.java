package com.utephonehub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for validating voucher code
 */
public class ValidateVoucherRequest {
    
    @NotBlank(message = "Mã voucher không được để trống")
    private String code;
    
    @NotNull(message = "Tổng giá trị đơn hàng không được để trống")
    @Positive(message = "Tổng giá trị đơn hàng phải lớn hơn 0")
    private BigDecimal orderTotal;
    
    // userId is optional (can be null for guest checkout)
    private Long userId;
    
    // Constructors
    public ValidateVoucherRequest() {
    }
    
    public ValidateVoucherRequest(String code, BigDecimal orderTotal, Long userId) {
        this.code = code;
        this.orderTotal = orderTotal;
        this.userId = userId;
    }
    
    // Getters and Setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public BigDecimal getOrderTotal() {
        return orderTotal;
    }
    
    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
