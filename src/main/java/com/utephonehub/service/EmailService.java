package com.utephonehub.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

/**
 * Email Service
 * Handle email sending for password reset, verification, etc.
 */
public class EmailService {
    
    private static final Logger logger = LogManager.getLogger(EmailService.class);
    
    // Static block to log when class is loaded
    static {
        System.out.println("EmailService class loaded!");
        LogManager.getLogger(EmailService.class).info("EmailService class loaded");
    }
    
    private final String mailHost;
    private final int mailPort;
    private final String mailUsername;
    private final String mailPassword;
    private final String mailFrom;
    
    public EmailService() {
        System.out.println("EmailService constructor called!");
        
        this.mailHost = getEnvOrProperty("MAIL_HOST", "smtp.gmail.com");
        this.mailPort = Integer.parseInt(getEnvOrProperty("MAIL_PORT", "587"));
        this.mailUsername = getEnvOrProperty("MAIL_USERNAME", "leekhoa0409@gmail.com");
        // Remove spaces from password (Gmail App Password format has spaces)
        this.mailPassword = getEnvOrProperty(
        "MAIL_PASSWORD",
        "jwli htbe oziw htzw"
).replace(" ", "");

        this.mailFrom = getEnvOrProperty("MAIL_FROM", "noreply@utephonehub.me");
        
        // Debug log with System.out to bypass logger issues
        System.out.println("EmailService initialized:");
        System.out.println("  Host: " + mailHost);
        System.out.println("  Port: " + mailPort);
        System.out.println("  Username: " + mailUsername);
        System.out.println("  HasPassword: " + (mailPassword != null && !mailPassword.isEmpty()));
        System.out.println("  Password length: " + (mailPassword != null ? mailPassword.length() : 0));
        
        // Also log normally
        logger.info("EmailService initialized - Host: {}, Port: {}, Username: {}, HasPassword: {}", 
                mailHost, mailPort, mailUsername, (mailPassword != null && !mailPassword.isEmpty()));
    }
    
    /**
     * Send forgot password email with OTP
     */
    public boolean sendForgotPasswordEmail(String toEmail, String otp) {
        String subject = "Mã xác nhận đặt lại mật khẩu - UTE Phone Hub";
        String htmlContent = buildForgotPasswordEmailTemplate(otp);
        
        return sendEmail(toEmail, subject, htmlContent);
    }
    
    /**
     * Send email verification code
     */
    public boolean sendVerificationEmail(String toEmail, String code) {
        String subject = "Xác nhận địa chỉ email - UTE Phone Hub";
        String htmlContent = buildVerificationEmailTemplate(code);
        
        return sendEmail(toEmail, subject, htmlContent);
    }
    
    /**
     * Send welcome email
     */
    public boolean sendWelcomeEmail(String toEmail, String fullName) {
        String subject = "Chào mừng đến với UTE Phone Hub!";
        String htmlContent = buildWelcomeEmailTemplate(fullName);
        
        return sendEmail(toEmail, subject, htmlContent);
    }
    
    /**
     * Send email
     */
    private boolean sendEmail(String toEmail, String subject, String htmlContent) {
        System.out.println("=== sendEmail called ===");
        System.out.println("To: " + toEmail);
        System.out.println("Subject: " + subject);
        
        logger.info("Attempting to send email to: {} with subject: {}", toEmail, subject);
        
        try {
            // Validate credentials
            if (mailUsername == null || mailUsername.isEmpty()) {
                System.out.println("ERROR: MAIL_USERNAME is empty!");
                logger.error("MAIL_USERNAME is not configured");
                return false;
            }
            if (mailPassword == null || mailPassword.isEmpty()) {
                System.out.println("ERROR: MAIL_PASSWORD is empty!");
                logger.error("MAIL_PASSWORD is not configured");
                return false;
            }
            
            System.out.println("Using SMTP server: " + mailHost + ":" + mailPort + " with username: " + mailUsername);
            logger.info("Using SMTP server: {}:{} with username: {}", mailHost, mailPort, mailUsername);
            
            // Setup mail properties
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", mailHost);
            props.put("mail.smtp.port", mailPort);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            // Create session
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUsername, mailPassword);
                }
            });
            
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailFrom, "UTE Phone Hub"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            // Send message
            System.out.println("Sending email message via Transport.send()...");
            logger.info("Sending email message...");
            Transport.send(message);
            
            System.out.println("Email sent successfully!");
            logger.info("Email sent successfully to: {}", toEmail);
            return true;
            
        } catch (MessagingException e) {
            System.out.println("MessagingException: " + e.getMessage());
            e.printStackTrace();
            logger.error("MessagingException while sending email to: {}", toEmail, e);
            logger.error("Error message: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            logger.error("Failed to send email to: {}", toEmail, e);
            return false;
        }
    }
    
    /**
     * Generate random OTP (6 digits)
     */
    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
    
    /**
     * Build forgot password email template
     */
    private String buildForgotPasswordEmailTemplate(String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                ".otp-box { background: white; border: 2px dashed #667eea; padding: 20px; margin: 20px 0; text-align: center; border-radius: 8px; }" +
                ".otp-code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }" +
                ".warning { color: #e74c3c; font-size: 14px; margin-top: 20px; }" +
                ".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🔐 Đặt lại mật khẩu</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Xin chào,</p>" +
                "<p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản UTE Phone Hub của mình.</p>" +
                "<p>Vui lòng sử dụng mã xác nhận dưới đây để đặt lại mật khẩu:</p>" +
                "<div class='otp-box'>" +
                "<div class='otp-code'>" + otp + "</div>" +
                "</div>" +
                "<p>Mã xác nhận này có hiệu lực trong <strong>5 phút</strong>.</p>" +
                "<p class='warning'>⚠️ Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2025 UTE Phone Hub. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Build email verification template
     */
    private String buildVerificationEmailTemplate(String code) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                ".code-box { background: white; border: 2px solid #667eea; padding: 20px; margin: 20px 0; text-align: center; border-radius: 8px; }" +
                ".code { font-size: 28px; font-weight: bold; color: #667eea; letter-spacing: 3px; }" +
                ".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>✉️ Xác nhận Email</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Xin chào,</p>" +
                "<p>Cảm ơn bạn đã đăng ký tài khoản tại UTE Phone Hub!</p>" +
                "<p>Vui lòng sử dụng mã xác nhận dưới đây để hoàn tất đăng ký:</p>" +
                "<div class='code-box'>" +
                "<div class='code'>" + code + "</div>" +
                "</div>" +
                "<p>Mã xác nhận này có hiệu lực trong <strong>15 phút</strong>.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2025 UTE Phone Hub. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Build welcome email template
     */
    private String buildWelcomeEmailTemplate(String fullName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                ".button { background: #667eea; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 20px 0; }" +
                ".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🎉 Chào mừng đến với UTE Phone Hub!</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Xin chào <strong>" + fullName + "</strong>,</p>" +
                "<p>Chào mừng bạn đến với UTE Phone Hub - nơi cung cấp điện thoại chất lượng cao với giá tốt nhất!</p>" +
                "<p>Tài khoản của bạn đã được tạo thành công. Bạn có thể bắt đầu khám phá các sản phẩm của chúng tôi ngay bây giờ.</p>" +
                "<center>" +
                "<a href='https://utephonehub.me' class='button'>Khám phá ngay</a>" +
                "</center>" +
                "<p>Nếu bạn có bất kỳ câu hỏi nào, đừng ngần ngại liên hệ với chúng tôi.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2025 UTE Phone Hub. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Get environment variable or system property
     */
    private String getEnvOrProperty(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null) {
            value = System.getProperty(key);
        }
        return value != null ? value : defaultValue;
    }
    
    /**
     * Send order confirmation email to customer
     */
    public void sendOrderConfirmationToCustomer(com.utephonehub.entity.Order order) {
        try {
            String subject = "Xác nhận đơn hàng #" + order.getOrderCode() + " - UTE Phone Hub";
            String htmlContent = buildOrderConfirmationTemplate(order);
            
            sendEmail(order.getEmail(), subject, htmlContent);
            logger.info("Order confirmation email sent to: {}", order.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send order confirmation email to: " + order.getEmail(), e);
            // Don't throw exception, just log
        }
    }
    
    /**
     * Send new order notification to admin
     */
    public void sendNewOrderNotificationToAdmin(com.utephonehub.entity.Order order) {
        try {
            String adminEmail = getEnvOrProperty("ADMIN_EMAIL", "01699963463tu@gmail.com");
            String subject = "Đơn hàng mới #" + order.getOrderCode() + " - UTE Phone Hub";
            String htmlContent = buildAdminOrderNotificationTemplate(order);
            
            sendEmail(adminEmail, subject, htmlContent);
            logger.info("Admin order notification sent to: {}", adminEmail);
        } catch (Exception e) {
            logger.error("Failed to send admin order notification", e);
            // Don't throw exception, just log
        }
    }
    
    /**
     * Build order confirmation email template for customer
     */
    private String buildOrderConfirmationTemplate(com.utephonehub.entity.Order order) {
        StringBuilder items = new StringBuilder();
        java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
        
        for (com.utephonehub.entity.OrderItem item : order.getItems()) {
            java.math.BigDecimal lineTotal = item.getPrice().multiply(new java.math.BigDecimal(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            
            items.append(String.format(
                "<tr>" +
                "<td style='padding: 10px; border-bottom: 1px solid #eee;'>%s</td>" +
                "<td style='padding: 10px; border-bottom: 1px solid #eee; text-align: center;'>%d</td>" +
                "<td style='padding: 10px; border-bottom: 1px solid #eee; text-align: right;'>%,.0f ₫</td>" +
                "<td style='padding: 10px; border-bottom: 1px solid #eee; text-align: right;'><strong>%,.0f ₫</strong></td>" +
                "</tr>",
                item.getProduct().getName(),
                item.getQuantity(),
                item.getPrice(),
                lineTotal
            ));
        }
        
       String paymentMethodText = switch (order.getPaymentMethod()) {
    case COD -> "Thanh toán khi nhận hàng (COD)";
    case STORE_PICKUP -> "Thanh toán tại cửa hàng";
    case BANK_TRANSFER -> "Chuyển khoản ngân hàng";
    case VNPAY -> "Thanh toán qua VNPay";
};

        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #ff6b35 0%, #ee4d2d 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 30px; }" +
                ".order-info { background: white; padding: 20px; margin: 20px 0; border-radius: 8px; }" +
                ".info-row { padding: 10px 0; border-bottom: 1px solid #eee; }" +
                ".info-label { font-weight: bold; color: #666; }" +
                "table { width: 100%; border-collapse: collapse; background: white; margin: 20px 0; }" +
                "th { background: #f5f5f5; padding: 12px; text-align: left; font-weight: 600; }" +
                ".total-row { font-size: 18px; color: #ff6b35; }" +
                ".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; border-radius: 0 0 10px 10px; background: #f9f9f9; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>✅ Đặt hàng thành công!</h1>" +
                "<p>Mã đơn hàng: <strong>#" + order.getOrderCode() + "</strong></p>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Xin chào <strong>" + order.getRecipientName() + "</strong>,</p>" +
                "<p>Cảm ơn bạn đã đặt hàng tại UTE Phone Hub! Đơn hàng của bạn đã được tiếp nhận và đang được xử lý.</p>" +
                
                "<div class='order-info'>" +
                "<h3>Thông tin đơn hàng</h3>" +
                "<div class='info-row'><span class='info-label'>Người nhận:</span> " + order.getRecipientName() + "</div>" +
                "<div class='info-row'><span class='info-label'>Số điện thoại:</span> " + order.getPhoneNumber() + "</div>" +
                "<div class='info-row'><span class='info-label'>Địa chỉ:</span> " + order.getStreetAddress() + ", " + order.getCity() + "</div>" +
                "<div class='info-row'><span class='info-label'>Phương thức thanh toán:</span> " + paymentMethodText + "</div>" +
                "</div>" +
                
                "<h3>Chi tiết đơn hàng</h3>" +
                "<table>" +
                "<thead>" +
                "<tr>" +
                "<th>Sản phẩm</th>" +
                "<th style='text-align: center; width: 80px;'>Số lượng</th>" +
                "<th style='text-align: right; width: 120px;'>Đơn giá</th>" +
                "<th style='text-align: right; width: 120px;'>Thành tiền</th>" +
                "</tr>" +
                "</thead>" +
                "<tbody>" +
                items.toString() +
                "<tr class='total-row'>" +
                "<td colspan='3' style='padding: 15px; text-align: right; border-top: 2px solid #ddd;'><strong>Tổng cộng:</strong></td>" +
                "<td style='padding: 15px; text-align: right; border-top: 2px solid #ddd;'><strong>" + String.format("%,.0f ₫", order.getTotalAmount()) + "</strong></td>" +
                "</tr>" +
                "</tbody>" +
                "</table>" +

                "<p>Bạn có thể tra cứu đơn hàng của mình bất kỳ lúc nào tại <a href='https://utephonehub.me/orders'>trang đơn hàng</a>.</p>" +
                "<p>Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi qua email hoặc hotline.</p>" +
                "<p>Trân trọng,<br><strong>UTE Phone Hub Team</strong></p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2025 UTE Phone Hub. All rights reserved.</p>" +
                "<p>Email: support@utephonehub.me | Hotline: 1800 1234</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Build admin order notification template
     */
    private String buildAdminOrderNotificationTemplate(com.utephonehub.entity.Order order) {
        StringBuilder items = new StringBuilder();
        
        for (com.utephonehub.entity.OrderItem item : order.getItems()) {
            items.append(String.format("- %s x%d - %,.0f ₫\n",
                item.getProduct().getName(),
                item.getQuantity(),
                item.getPrice().multiply(new java.math.BigDecimal(item.getQuantity()))
            ));
        }
        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #ff6b35;'>🔔 Đơn hàng mới #" + order.getOrderCode() + "</h2>" +
                "<p><strong>Thời gian:</strong> " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</p>" +
                "<p><strong>Khách hàng:</strong> " + order.getRecipientName() + "</p>" +
                "<p><strong>Email:</strong> " + order.getEmail() + "</p>" +
                "<p><strong>Số điện thoại:</strong> " + order.getPhoneNumber() + "</p>" +
                "<p><strong>Địa chỉ:</strong> " + order.getStreetAddress() + ", " + order.getCity() + "</p>" +
                "<p><strong>Phương thức thanh toán:</strong> " + order.getPaymentMethod() + "</p>" +
                "<h3>Sản phẩm:</h3>" +
                "<pre>" + items.toString() + "</pre>" +
                "<h3 style='color: #ff6b35;'>Tổng: " + String.format("%,.0f ₫", order.getTotalAmount()) + "</h3>" +
                "<p><a href='https://utephonehub.me/admin/orders' style='background: #ff6b35; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>Xem chi tiết</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
