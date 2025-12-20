package com.utephonehub.config;

public class VNPayConfig {
    // Merchant info
    public static final String vnp_TmnCode = "1K86M9UK";
    public static final String vnp_HashSecret = "2A30WFU1J0WK8UADYTLLU9IEP6X7M55I";

    // URLs
    public static final String vnp_PayUrl =
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    public static final String vnp_ReturnUrl =
            "http://localhost:8080/payment/return";

    public static final String vnp_IpnUrl =
            "http://localhost:8080/payment/return";

    // VNPay constants
    public static final String vnp_version = "2.1.0";
    public static final String payment_success_code = "00";

}
