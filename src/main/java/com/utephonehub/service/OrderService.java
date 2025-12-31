package com.utephonehub.service;

import com.utephonehub.dto.request.PaymentInfoRequest;
import com.utephonehub.dto.response.VoucherValidationResult;
import com.utephonehub.entity.*;
import com.utephonehub.repository.*;
import com.utephonehub.util.JsonUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import com.utephonehub.config.VNPayConfig;
import com.utephonehub.util.VNPayUtil;

public class OrderService {
    
    private static final Logger logger = LogManager.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final VoucherService voucherService;
    private final RedisService redisService;
    private final EmailService emailService;
    private final JsonUtil jsonUtil;
    
    public OrderService() {
        this.orderRepository = new OrderRepository();
        this.cartRepository = new CartRepository();
        this.productRepository = new ProductRepository();
        this.addressRepository = new AddressRepository();
        this.userRepository = new UserRepository();
        this.voucherService = new VoucherService();
        this.redisService = new RedisService();
        this.emailService = new EmailService();
        this.jsonUtil = new JsonUtil();
    }
    
    /**
     * Checkout với payment info (Redis-based, không lưu vào DB)
     * @param userId User ID (null cho guest checkout)
     * @param shippingInfo Thông tin giao hàng
     * @param voucherCode Mã voucher (optional)
     * @param paymentMethod Phương thức thanh toán (COD, STORE_PICKUP, BANK_TRANSFER)
     * @param paymentInfo Thông tin thanh toán (chỉ cần cho BANK_TRANSFER)
     * @return Order data
     */
    public Map<String, Object> checkout(Long userId, Map<String, Object> shippingInfo, 
                                       String voucherCode, String paymentMethod,
                                       PaymentInfoRequest paymentInfo) {
        
        logger.info("Processing checkout for userId: {}, paymentMethod: {}", userId, paymentMethod);
        
        // Validate payment method
        Order.PaymentMethod method;
        try {
            method = Order.PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ: " + paymentMethod);
        }
        
        // Validate payment info for BANK_TRANSFER
        if (method == Order.PaymentMethod.BANK_TRANSFER) {
            if (paymentInfo == null) {
                throw new RuntimeException("Vui lòng cung cấp thông tin thanh toán");
            }
            paymentInfo.validate(); // Will throw exception if invalid
            logger.info("Payment info validated: {}", paymentInfo);
        }
        
        // Get user's cart
        Optional<Cart> cartOpt = userId != null ? cartRepository.findByUserId(userId) : Optional.empty();
        
        if (cartOpt.isEmpty() || cartOpt.get().getItems().isEmpty()) {
            logger.warn("Cart is empty for userId: {}", userId);
            throw new RuntimeException("Giỏ hàng trống");
        }
        
        Cart cart = cartOpt.get();
        
        // Calculate total (NO shipping fee as per requirement)
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal lineTotal = item.getProduct().getPrice()
                .multiply(new BigDecimal(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }
        
        logger.debug("Order total before discount: {}", totalAmount);
        
        // Apply voucher if provided
        Voucher voucher = null;
        BigDecimal finalAmount = totalAmount;
        
        if (voucherCode != null && !voucherCode.isEmpty()) {
            VoucherValidationResult validation = voucherService.validateVoucher(
                voucherCode, totalAmount, userId
            );
            
            if (!validation.isValid()) {
                logger.warn("Voucher validation failed: {}", validation.getErrorMessage());
                throw new RuntimeException(validation.getErrorMessage());
            }
            
            voucher = validation.getVoucher();
            BigDecimal discount = voucherService.calculateDiscount(voucher, totalAmount);
            finalAmount = totalAmount.subtract(discount);
            
            logger.info("Voucher applied: {} - Discount: {} - Final amount: {}", 
                voucherCode, discount, finalAmount);
        }
        
        // Create order
        Order order = new Order();
        order.setOrderCode(orderRepository.generateOrderCode());
        
        // Set user if logged in
        if (userId != null) {
            User user = new User();
            user.setId(userId);
            order.setUser(user);
        }
        
        // Set shipping info - handle both addressId and direct fields
        if (shippingInfo.containsKey("addressId") && shippingInfo.get("addressId") != null) {
            // Load address from database
            Long addressId = ((Number) shippingInfo.get("addressId")).longValue();
            Optional<Address> addressOpt = addressRepository.findById(addressId);
            
            if (addressOpt.isEmpty()) {
                throw new RuntimeException("Địa chỉ không tồn tại");
            }
            
            Address address = addressOpt.get();
            
            // Populate order with address data
            // Email: get from user repository to avoid lazy loading proxy issue
            String email = null;
            if (userId != null) {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    email = userOpt.get().getEmail();
                }
            }
            order.setEmail(email);
            
            order.setRecipientName(address.getRecipientName());
            order.setPhoneNumber(address.getPhoneNumber());
            order.setStreetAddress(address.getStreetAddress());
            order.setCity(address.getProvince()); // Address uses 'province' field
            
            logger.info("Using saved address: {} for user: {}", addressId, userId);
        } else {
            // Use direct fields from request
            order.setEmail((String) shippingInfo.get("email"));
            order.setRecipientName((String) shippingInfo.get("recipientName"));
            order.setPhoneNumber((String) shippingInfo.get("phoneNumber"));
            order.setStreetAddress((String) shippingInfo.get("streetAddress"));
            order.setCity((String) shippingInfo.get("city"));
            
            logger.info("Using new address from form");
        }
        
        // Set payment method
        order.setPaymentMethod(method);
        
        order.setTotalAmount(finalAmount);
        order.setVoucher(voucher);
        order.setStatus(Order.OrderStatus.PENDING);
        
        // Save order first to get ID
        Order savedOrder = orderRepository.save(order);
        logger.info("Order created with code: {}", savedOrder.getOrderCode());
        
        // Store payment info in Redis if BANK_TRANSFER (TTL 24h)
        if (method == Order.PaymentMethod.BANK_TRANSFER && paymentInfo != null) {
            try {
                String key = "payment_info:" + savedOrder.getId();
                String value = jsonUtil.toJson(paymentInfo);
                redisService.set(key, value, 86400); // 24 hours TTL
                logger.info("Payment info stored in Redis for order: {}", savedOrder.getId());
            } catch (Exception e) {
                logger.error("Failed to store payment info in Redis", e);
                // Don't fail checkout, just log the error
            }
        }
        
        // Create order items from cart
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getProduct().getPrice());
            
            savedOrder.getItems().add(orderItem);
            
            // Decrease stock
            Product product = cartItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }
        
        // Save order with items
        savedOrder = orderRepository.save(savedOrder);
        
        // Apply voucher usage tracking (increment count)
        if (voucher != null) {
            try {
                voucherService.applyVoucher(voucher.getId());
            } catch (Exception e) {
                logger.error("Failed to track voucher usage, but order is created", e);
                // Don't fail the checkout, just log the error
            }
        }
        
        // Clear cart
        for (CartItem item : new ArrayList<>(cart.getItems())) {
            cartRepository.deleteCartItem(item.getId());
        }
        
        logger.info("Checkout completed successfully for order: {}", savedOrder.getOrderCode());
        
        // Re-fetch order with EAGER loaded items to avoid LazyInitializationException in EmailService
        final Long orderId = savedOrder.getId();
        Order orderWithItems = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found after save: " + orderId));
        
        // Send email notifications (async, don't fail checkout if email fails)
        try {
            emailService.sendOrderConfirmationToCustomer(orderWithItems);
            emailService.sendNewOrderNotificationToAdmin(orderWithItems);
            logger.info("Order notification emails sent for order: {}", savedOrder.getOrderCode());
        } catch (Exception e) {
            logger.error("Failed to send order notification emails, but checkout succeeded", e);
            // Don't throw exception, checkout is already successful
        }
        
        String payment_url = "";
        if (method == Order.PaymentMethod.VNPAY) {
            String ipAddr = "172.16.30.240"; // or get from request

            Map<String, String> vnpParams = new HashMap<>();

            // Use orderId directly
            String txnRef = String.valueOf(savedOrder.getId());

            vnpParams.put("vnp_TxnRef", txnRef);

            // Amount in VND (createPayment() will *100)
            vnpParams.put(
                "amount",
                savedOrder.getTotalAmount().toBigInteger().toString()
            );

            vnpParams.put(
                "vnp_OrderInfo",
                "Thanh toan don hang " + savedOrder.getOrderCode()
            );

            vnpParams.put("ordertype", "other");
            vnpParams.put("language", "vn");

            payment_url = createPayment(ipAddr, vnpParams);
            }


        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", savedOrder.getId());
        response.put("orderCode", savedOrder.getOrderCode());
        response.put("totalAmount", savedOrder.getTotalAmount());
        response.put("paymentMethod", savedOrder.getPaymentMethod().name());
        response.put("payment_url",payment_url);
        
        return response;
    }

                public String createPayment(String ipAddr, Map<String, String> params) {
                String vnp_Version = VNPayConfig.vnp_version;
                    String vnp_Command = "pay";
                    // String txnRef = "BOOKING-" + booking.getId() + "-" + System.currentTimeMillis();
                    String vnp_TxnRef = params.get("vnp_TxnRef");
                    String vnp_IpAddr = ipAddr;
                    int amount = Integer.parseInt(params.get("amount")) * 100;

                    Map<String, String> vnp_Params = new HashMap<>();
                    vnp_Params.put("vnp_Version", vnp_Version);
                    vnp_Params.put("vnp_Command", vnp_Command);
                    vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
                    vnp_Params.put("vnp_Amount", String.valueOf(amount));
                    vnp_Params.put("vnp_CurrCode", "VND");
                    vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
                    vnp_Params.put("vnp_OrderInfo", params.get("vnp_OrderInfo"));
                    vnp_Params.put("vnp_OrderType", params.get("ordertype"));
                    vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
                    vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
                    vnp_Params.put("vnp_Locale", params.getOrDefault("language", "vn"));

                    // ==========================
                    // Thời gian VN (UTC+7)
                    // ==========================
                    Calendar cld = Calendar.getInstance();
                    cld.add(Calendar.HOUR_OF_DAY, 7);
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

                    String vnp_CreateDate = formatter.format(cld.getTime());
                    vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

                    cld.add(Calendar.MINUTE, 120);
                    String vnp_ExpireDate = formatter.format(cld.getTime());
                    vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

                    // ==========================
                    // Sinh query & hash
                    // ==========================
                    List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
                    Collections.sort(fieldNames);

                    StringBuilder hashData = new StringBuilder();
                    StringBuilder query = new StringBuilder();

                try {
                for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
                    String fieldName = itr.next();
                    String fieldValue = vnp_Params.get(fieldName);
                    if (fieldValue != null && !fieldValue.isEmpty()) {
                        String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());
                        hashData.append(fieldName).append('=').append(encodedValue);
                        query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                            .append('=').append(encodedValue);

                        if (itr.hasNext()) {
                            hashData.append('&');
                            query.append('&');
                        }
                    }   
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding not supported", e);
            }

                    String vnp_SecureHash = VNPayUtil.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
                    String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + query + "&vnp_SecureHash=" + vnp_SecureHash;

                    return paymentUrl;
                }
    public List<Map<String, Object>> getUserOrders(Long userId, int page, int limit) {
        List<Order> orders = orderRepository.findByUserId(userId, page, limit);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("id", order.getId());
            orderData.put("orderCode", order.getOrderCode());
            orderData.put("status", order.getStatus().toString());
            orderData.put("totalAmount", order.getTotalAmount());
            orderData.put("createdAt", order.getCreatedAt());
            
            result.add(orderData);
        }
        
        return result;
    }
    
    public Map<String, Object> getOrderDetail(Long orderId, Long userId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId);
        }
        
        Order order = orderOpt.get();
        order.getItems().size();
        for(OrderItem item : order.getItems()){
            item.getProduct();
            String img = item.getProduct().getThumbnailUrl();
        }
        // Check if order belongs to user
        if (userId != null && (order.getUser() == null || !order.getUser().getId().equals(userId))) {
            throw new RuntimeException("Bạn không có quyền xem đơn hàng này");
        }
        
        return buildOrderDetailResponse(order);
    }
    
    public Map<String, Object> lookupOrder(String orderCode, String email) {
        Optional<Order> orderOpt = orderRepository.findByOrderCodeAndEmail(orderCode, email);
        
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy đơn hàng với thông tin đã cung cấp");
        }
        
        return buildOrderDetailResponse(orderOpt.get());
    }
    
    private Map<String, Object> buildOrderDetailResponse(Order order) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", order.getId());
        response.put("orderCode", order.getOrderCode());
        response.put("status", order.getStatus().toString());
        response.put("paymentMethod", order.getPaymentMethod().toString());
        response.put("totalAmount", order.getTotalAmount());
        
        // Shipping info
        Map<String, Object> shippingInfo = new HashMap<>();
        shippingInfo.put("recipientName", order.getRecipientName());
        shippingInfo.put("phoneNumber", order.getPhoneNumber());
        shippingInfo.put("email", order.getEmail());
        shippingInfo.put("streetAddress", order.getStreetAddress());
        shippingInfo.put("city", order.getCity());
        response.put("shippingInfo", shippingInfo);
        
        // Items
        List<Map<String, Object>> items = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("productId", item.getProduct().getId());
                itemData.put("thumbnailUrl",item.getProduct().getThumbnailUrl());
                itemData.put("productName", item.getProduct().getName());
                itemData.put("quantity", item.getQuantity());
                itemData.put("price", item.getPrice());
                itemData.put("lineTotal", item.getPrice().multiply(new BigDecimal(item.getQuantity())));
                
                items.add(itemData);
            }
        }
        response.put("items", items);
        
        // Voucher
        if (order.getVoucher() != null) {
            Map<String, Object> voucherData = new HashMap<>();
            voucherData.put("code", order.getVoucher().getCode());
            response.put("voucher", voucherData);
        }
        
        response.put("createdAt", order.getCreatedAt());
        
        return response;
    }
}
