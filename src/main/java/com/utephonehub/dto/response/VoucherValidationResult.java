package com.utephonehub.dto.response;

import com.utephonehub.entity.Voucher;

/**
 * DTO for voucher validation result
 */
public class VoucherValidationResult {
    
    private boolean valid;
    private String errorMessage;
    private Voucher voucher;
    
    // Private constructor to enforce factory methods
    private VoucherValidationResult(boolean valid, String errorMessage, Voucher voucher) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.voucher = voucher;
    }
    
    /**
     * Factory method for valid voucher
     */
    public static VoucherValidationResult valid(Voucher voucher) {
        return new VoucherValidationResult(true, null, voucher);
    }
    
    /**
     * Factory method for invalid voucher
     */
    public static VoucherValidationResult invalid(String errorMessage) {
        return new VoucherValidationResult(false, errorMessage, null);
    }
    
    // Getters
    public boolean isValid() {
        return valid;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Voucher getVoucher() {
        return voucher;
    }
}
