<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Chi tiết đơn hàng" scope="request"/>
<c:set var="pageCss" value="pages/order-detail.css" scope="request"/>
<c:set var="pageJs" value="pages/order-detail.js" scope="request"/>
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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/main.css">
    <c:if test="${not empty pageCss}">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/${pageCss}">
    </c:if>
</head>
<body>
    <%@ include file="/WEB-INF/views/common/header.jspf" %>
    
    <main class="main-content container my-5">
        <!-- Breadcrumb -->
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/">Trang chủ</a></li>
                <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/orders">Đơn hàng của tôi</a></li>
                <li class="breadcrumb-item active" aria-current="page">Chi tiết đơn hàng</li>
            </ol>
        </nav>

        <!-- Success Alert (if from checkout) -->
        <c:if test="${param.success == 'true'}">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                <i class="fas fa-check-circle me-2"></i>
                <strong>Đặt hàng thành công!</strong> Đơn hàng của bạn đã được tạo. Chúng tôi sẽ liên hệ với bạn sớm nhất.
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:if>

        <!-- Loading State -->
        <div id="loadingState" class="text-center py-5">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Đang tải...</span>
            </div>
            <p class="mt-3 text-muted">Đang tải thông tin đơn hàng...</p>
        </div>

        <!-- Order Content (Loaded by JS) -->
        <div id="orderContent" style="display: none;">
            <!-- Order Header -->
            <div class="order-header card shadow-sm mb-4">
                <div class="card-body">
                    <div class="row align-items-center">
                        <div class="col-md-8">
                            <h4 class="mb-2">
                                <i class="fas fa-receipt text-primary me-2"></i>
                                Đơn hàng <span id="orderCode" class="text-primary"></span>
                            </h4>
                            <p class="text-muted mb-0">
                                <i class="far fa-calendar-alt me-2"></i>
                                Ngày đặt: <span id="orderDate"></span>
                            </p>
                        </div>
                        <div class="col-md-4 text-md-end mt-3 mt-md-0">
                            <span id="orderStatus" class="badge fs-6"></span>
                        </div>
                    </div>
                </div>
            </div>

            <div class="row">
                <!-- Left Column: Order Details -->
                <div class="col-lg-8">
                    <!-- Order Items -->
                    <div class="card shadow-sm mb-4">
                        <div class="card-header bg-white">
                            <h5 class="mb-0">
                                <i class="fas fa-box me-2"></i>
                                Sản phẩm đã đặt
                            </h5>
                        </div>
                        <div class="card-body">
                            <div id="orderItems">
                                <!-- Items will be loaded by JS -->
                            </div>
                        </div>
                    </div>

                    <!-- Shipping Info -->
                    <div class="card shadow-sm mb-4">
                        <div class="card-header bg-white">
                            <h5 class="mb-0">
                                <i class="fas fa-shipping-fast me-2"></i>
                                Thông tin giao hàng
                            </h5>
                        </div>
                        <div class="card-body">
                            <div class="shipping-info">
                                <div class="info-row">
                                    <span class="info-label">Người nhận:</span>
                                    <span class="info-value" id="recipientName"></span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Số điện thoại:</span>
                                    <span class="info-value" id="phoneNumber"></span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Email:</span>
                                    <span class="info-value" id="email"></span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Địa chỉ:</span>
                                    <span class="info-value" id="address"></span>
                                </div>
                                <div class="info-row" id="notesRow" style="display: none;">
                                    <span class="info-label">Ghi chú:</span>
                                    <span class="info-value" id="notes"></span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Right Column: Order Summary -->
                <div class="col-lg-4">
                    <!-- Order Summary -->
                    <div class="card shadow-sm mb-4 sticky-summary">
                        <div class="card-header bg-white">
                            <h5 class="mb-0">
                                <i class="fas fa-file-invoice-dollar me-2"></i>
                                Tóm tắt đơn hàng
                            </h5>
                        </div>
                        <div class="card-body">
                            <div class="summary-row">
                                <span>Phương thức thanh toán:</span>
                                <span id="paymentMethod" class="fw-semibold"></span>
                            </div>
                            <div class="summary-row" id="voucherRow" style="display: none;">
                                <span>Mã giảm giá:</span>
                                <span id="voucherCode" class="text-success fw-semibold"></span>
                            </div>
                            <hr>
                            <div class="summary-row fs-5 fw-bold">
                                <span>Tổng cộng:</span>
                                <span id="totalAmount" class="text-danger"></span>
                            </div>
                        </div>
                    </div>

                    <!-- Actions -->
                    <div class="card shadow-sm" id="orderActions">
                        <div class="card-body">
                            <button class="btn btn-outline-danger w-100 mb-2" id="cancelOrderBtn" style="display: none;">
                                <i class="fas fa-times-circle me-2"></i>
                                Hủy đơn hàng
                            </button>
                            <a href="${pageContext.request.contextPath}/orders" class="btn btn-outline-secondary w-100">
                                <i class="fas fa-list me-2"></i>
                                Xem tất cả đơn hàng
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Error State -->
        <div id="errorState" class="text-center py-5" style="display: none;">
            <i class="fas fa-exclamation-circle fa-4x text-danger mb-3"></i>
            <h4>Không tìm thấy đơn hàng</h4>
            <p class="text-muted">Đơn hàng không tồn tại hoặc bạn không có quyền xem.</p>
            <a href="${pageContext.request.contextPath}/orders" class="btn btn-primary">
                <i class="fas fa-arrow-left me-2"></i>
                Quay lại danh sách đơn hàng
            </a>
        </div>
    </main>
    
    <%@ include file="/WEB-INF/views/common/footer.jspf" %>
    
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- Page-specific JavaScript -->
    <c:if test="${not empty pageJs}">
        <script src="${pageContext.request.contextPath}/static/js/${pageJs}"></script>
    </c:if>
</body>
</html>

