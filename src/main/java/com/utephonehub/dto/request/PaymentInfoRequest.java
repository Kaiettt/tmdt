package com.utephonehub.dto.request;

/**
 * Payment Info Request DTO
 * Dùng cho phương thức thanh toán BANK_TRANSFER (giả lập)
 */
public class PaymentInfoRequest {
    
    private String cardNumber;    // Số thẻ (giả lập, không validate thật)
    private String cardHolder;    // Tên chủ thẻ
    private String expiryDate;    // MM/YY
    private String cvv;           // CVV (3 digits)
    
    // Constructors
    public PaymentInfoRequest() {}
    
    public PaymentInfoRequest(String cardNumber, String cardHolder, String expiryDate, String cvv) {
        this.cardNumber = cardNumber;
        this.cardHolder = cardHolder;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
    }
    
    // Getters and Setters
    public String getCardNumber() {
        return cardNumber;
    }
    
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    public String getCardHolder() {
        return cardHolder;
    }
    
    public void setCardHolder(String cardHolder) {
        this.cardHolder = cardHolder;
    }
    
    public String getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public String getCvv() {
        return cvv;
    }
    
    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
    
    /**
     * Validate payment info (basic mock validation)
     */
    public void validate() throws RuntimeException {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            throw new RuntimeException("Số thẻ không được để trống");
        }
        
        if (cardHolder == null || cardHolder.trim().isEmpty()) {
            throw new RuntimeException("Tên chủ thẻ không được để trống");
        }
        
        if (expiryDate == null || !expiryDate.matches("\\d{2}/\\d{2}")) {
            throw new RuntimeException("Ngày hết hạn không hợp lệ (định dạng MM/YY)");
        }
        
        if (cvv == null || !cvv.matches("\\d{3}")) {
            throw new RuntimeException("CVV không hợp lệ (3 chữ số)");
        }
        
        // Basic card number validation (mock - just check length)
        String cardNumberClean = cardNumber.replaceAll("\\s+", "");
        if (cardNumberClean.length() < 13 || cardNumberClean.length() > 19) {
            throw new RuntimeException("Số thẻ không hợp lệ");
        }
    }
    
    @Override
    public String toString() {
        // Mask sensitive data in logs
        return "PaymentInfoRequest{" +
                "cardNumber='" + maskCardNumber(cardNumber) + '\'' +
                ", cardHolder='" + cardHolder + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", cvv='***'" +
                '}';
    }
    
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}

