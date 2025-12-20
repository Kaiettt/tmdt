<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt hàng thành công - UTE PhoneHub</title>
    
    <!-- Favicon -->
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/favicon.png">
    
    <!-- CSS -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/main.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/components/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/components/footer.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/pages/order-success.css">
</head>
<body data-context-path="${pageContext.request.contextPath}">
    <%@ include file="../common/header.jspf" %>
    
    <main class="order-success-page">
        <div class="container">
            <!-- Success Message -->
            <div class="success-card">
                <div class="success-icon">
                    <i class="fas fa-check-circle"></i>
                </div>
                
                <h1 class="success-title">Đặt hàng thành công!</h1>
                <p class="success-message">
                    Cảm ơn bạn đã tin tưởng mua hàng tại UTE PhoneHub
                </p>
                
                <div class="order-info">
                    <div class="order-info-item">
                        <span class="label">Mã đơn hàng:</span>
                        <span class="value order-code">
                            <c:out value="${order.orderCode}"/>
                        </span>
                    </div>
                    <div class="order-info-item">
                        <span class="label">Tổng tiền:</span>
                        <span class="value total-amount">
                            <fmt:formatNumber value="${order.totalAmount}" type="currency" currencyCode="VND"/>
                        </span>
                    </div>
                    <div class="order-info-item">
                        <span class="label">Phương thức thanh toán:</span>
                        <span class="value">
                            <c:choose>
                                <c:when test="${order.paymentMethod == 'COD'}">
                                    Thanh toán khi nhận hàng (COD)
                                </c:when>
                                <c:when test="${order.paymentMethod == 'STORE_PICKUP'}">
                                    Nhận và thanh toán tại cửa hàng
                                </c:when>
                                <c:when test="${order.paymentMethod == 'BANK_TRANSFER'}">
                                    Chuyển khoản ngân hàng
                                </c:when>
                                <c:otherwise>
                                    <c:out value="${order.paymentMethod}"/>
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                </div>
                
                <!-- Email Notification -->
                <div class="email-notification">
                    <i class="fas fa-envelope"></i>
                    <p>
                        Chúng tôi đã gửi email xác nhận đơn hàng đến 
                        <strong><c:out value="${order.email}"/></strong>
                    </p>
                </div>
                
                <!-- Next Steps -->
                <div class="next-steps">
                    <h3>Bước tiếp theo</h3>
                    <ul>
                        <c:choose>
                            <c:when test="${order.paymentMethod == 'COD'}">
                                <li><i class="fas fa-check"></i> Chúng tôi sẽ liên hệ xác nhận đơn hàng trong vòng 24h</li>
                                <li><i class="fas fa-check"></i> Đơn hàng sẽ được giao trong 3-5 ngày làm việc</li>
                                <li><i class="fas fa-check"></i> Thanh toán khi nhận hàng</li>
                            </c:when>
                            <c:when test="${order.paymentMethod == 'STORE_PICKUP'}">
                                <li><i class="fas fa-check"></i> Chúng tôi sẽ liên hệ xác nhận trong vòng 24h</li>
                                <li><i class="fas fa-check"></i> Bạn có thể đến cửa hàng để nhận và thanh toán</li>
                                <li><i class="fas fa-check"></i> Mang theo mã đơn hàng khi đến cửa hàng</li>
                            </c:when>
                            <c:when test="${order.paymentMethod == 'BANK_TRANSFER'}">
                                <li><i class="fas fa-check"></i> Vui lòng kiểm tra email để xem thông tin chuyển khoản</li>
                                <li><i class="fas fa-check"></i> Đơn hàng sẽ được xử lý sau khi nhận được thanh toán</li>
                                <li><i class="fas fa-check"></i> Thời gian giao hàng: 3-5 ngày sau khi xác nhận thanh toán</li>
                            </c:when>
                        </c:choose>
                    </ul>
                </div>
                
                <!-- Action Buttons -->
                <div class="action-buttons">
                    <a href="${pageContext.request.contextPath}/orders/${order.id}" class="btn btn-primary">
                        <i class="fas fa-receipt"></i>
                        Xem chi tiết đơn hàng
                    </a>
                    <a href="${pageContext.request.contextPath}/" class="btn btn-secondary">
                        <i class="fas fa-home"></i>
                        Về trang chủ
                    </a>
                </div>
            </div>
        </div>
    </main>
    
    <%@ include file="../common/footer.jspf" %>
</body>
</html>

