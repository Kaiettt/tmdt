<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Giỏ hàng" scope="request"/>
<c:set var="pageDescription" value="Xem và quản lý giỏ hàng của bạn" scope="request"/>
<c:set var="pageCss" value="pages/cart.css" scope="request"/>
<c:set var="pageJs" value="pages/cart-page.js" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <%@ include file="/WEB-INF/views/common/meta.jspf" %>
    <title><c:out value="${pageTitle}"/> - UTE Phone Hub</title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <!-- Custom CSS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/main.css">
    <c:if test="${not empty pageCss}">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/${pageCss}">
    </c:if>
</head>
<body>
    <%@ include file="/WEB-INF/views/common/header.jspf" %>
    
    <main class="main-content">
        <div class="container">
            <!-- Breadcrumb -->
            <div class="breadcrumb">
                <a href="${pageContext.request.contextPath}/">Trang chủ</a>
                <i class="fas fa-chevron-right"></i>
                <span>Giỏ hàng</span>
            </div>

            <!-- Cart Header -->
            <div class="cart-header">
                <h1>Giỏ hàng của bạn</h1>
                <div class="cart-actions" id="cart-header-actions" style="display: none;">
                    <button class="btn btn-outline-danger" onclick="handleClearCart()">
                        <i class="fas fa-trash"></i>
                        Xóa tất cả
                    </button>
                </div>
            </div>

            <!-- Loading State -->
            <div id="loading-state" class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Đang tải...</span>
                </div>
                <p class="mt-3">Đang tải giỏ hàng...</p>
            </div>

            <!-- Error State -->
            <div id="error-state" class="alert alert-danger d-none" role="alert">
                <h4 class="alert-heading">Lỗi!</h4>
                <p id="error-message">Không thể tải giỏ hàng.</p>
            </div>

            <!-- Empty Cart State -->
            <div id="empty-cart" class="empty-cart d-none">
                <div class="empty-cart-content">
                    <i class="fas fa-shopping-cart"></i>
                    <h2>Giỏ hàng trống</h2>
                    <p>Bạn chưa có sản phẩm nào trong giỏ hàng</p>
                    <a href="${pageContext.request.contextPath}/products" class="btn btn-primary btn-lg">
                        <i class="fas fa-shopping-bag"></i>
                        Mua sắm ngay
                    </a>
                </div>
            </div>

            <!-- Cart Content -->
            <div id="cart-content" class="cart-content d-none">
                <div class="row">
                    <!-- Cart Items -->
                    <div class="col-lg-8">
                        <div class="cart-items-container">
                            <div class="cart-items-header">
                                <div class="row">
                                    <div class="col-5">Sản phẩm</div>
                                    <div class="col-2 text-center">Đơn giá</div>
                                    <div class="col-3 text-center">Số lượng</div>
                                    <div class="col-2 text-end">Thành tiền</div>
                                </div>
                            </div>
                            <div id="cart-items" class="cart-items">
                                <!-- Cart items will be rendered here -->
                            </div>
                        </div>
                    </div>

                    <!-- Cart Summary -->
                    <div class="col-lg-4">
                        <div class="cart-summary sticky-top">
                            <div class="summary-card">
                                <h3>Tóm tắt đơn hàng</h3>

                                <div class="summary-row">
                                    <span>Tạm tính:</span>
                                    <span id="subtotal">0₫</span>
                                </div>

                                <div class="summary-row">
                                    <span>Phí vận chuyển:</span>
                                    <span id="shipping">0₫</span>
                                </div>

                                <div class="summary-divider"></div>

                                <div class="summary-row summary-total">
                                    <span>Tổng cộng:</span>
                                    <span id="total" class="text-danger">0₫</span>
                                </div>

                                <!-- Checkout Button -->
                                <button class="btn btn-danger btn-lg w-100 btn-checkout" onclick="handleCheckout()">
                                    <i class="fas fa-shopping-bag"></i>
                                    Tiến hành thanh toán
                                </button>

                                <!-- Payment Info -->
                                <div class="payment-info">
                                    <div class="info-item">
                                        <i class="fas fa-shield-alt"></i>
                                        <span>Thanh toán an toàn & bảo mật</span>
                                    </div>
                                    <div class="info-item">
                                        <i class="fas fa-truck"></i>
                                        <span>Miễn phí vận chuyển đơn từ 1 triệu</span>
                                    </div>
                                    <div class="info-item">
                                        <i class="fas fa-undo"></i>
                                        <span>Đổi trả trong 7 ngày</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Suggested Products Section -->
            <div id="suggested-products" class="suggested-products d-none">
                <h2 class="section-title">
                    <i class="fas fa-star"></i>
                    Có thể bạn quan tâm
                </h2>
                <div class="product-grid" id="suggested-products-grid">
                    <!-- Suggested products will be rendered here -->
                </div>
            </div>
        </div>
    </main>
    
    <%@ include file="/WEB-INF/views/common/footer.jspf" %>
    
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- Page-specific JS -->
    <script src="${pageContext.request.contextPath}/static/js/pages/cart-page.js"></script>
</body>
</html>
