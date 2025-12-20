package com.utephonehub.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO for voucher validation
 */
public class ValidateVoucherResponse {
    
    private boolean valid;
    private String message;
    private VoucherInfo voucher;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    
    // Constructors
    public ValidateVoucherResponse() {
    }
    
    public ValidateVoucherResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }
    
    // Factory method for valid voucher
    public static ValidateVoucherResponse success(VoucherInfo voucher, 
                                                   BigDecimal discountAmount, 
                                                   BigDecimal finalAmount) {
        ValidateVoucherResponse response = new ValidateVoucherResponse();
        response.valid = true;
        response.message = "Mã giảm giá hợp lệ";
        response.voucher = voucher;
        response.discountAmount = discountAmount;
        response.finalAmount = finalAmount;
        return response;
    }
    
    // Factory method for invalid voucher
    public static ValidateVoucherResponse error(String message) {
        return new ValidateVoucherResponse(false, message);
    }
    
    // Getters and Setters
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public VoucherInfo getVoucher() {
        return voucher;
    }
    
    public void setVoucher(VoucherInfo voucher) {
        this.voucher = voucher;
    }
    
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }
    
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    public BigDecimal getFinalAmount() {
        return finalAmount;
    }
    
    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }
    
    /**
     * Nested class for voucher information
     */
    public static class VoucherInfo {
        private String code;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal minOrderValue;
        
        public VoucherInfo() {
        }
        
        public VoucherInfo(String code, String discountType, BigDecimal discountValue, BigDecimal minOrderValue) {
            this.code = code;
            this.discountType = discountType;
            this.discountValue = discountValue;
            this.minOrderValue = minOrderValue;
        }
        
        // Getters and Setters
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getDiscountType() {
            return discountType;
        }
        
        public void setDiscountType(String discountType) {
            this.discountType = discountType;
        }
        
        public BigDecimal getDiscountValue() {
            return discountValue;
        }
        
        public void setDiscountValue(BigDecimal discountValue) {
            this.discountValue = discountValue;
        }
        
        public BigDecimal getMinOrderValue() {
            return minOrderValue;
        }
        
        public void setMinOrderValue(BigDecimal minOrderValue) {
            this.minOrderValue = minOrderValue;
        }
    }
}
