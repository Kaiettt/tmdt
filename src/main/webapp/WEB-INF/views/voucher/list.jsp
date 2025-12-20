<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Vouchers & Khuyến mãi" scope="request"/>
<c:set var="pageDescription" value="Các mã giảm giá và chương trình khuyến mãi đặc biệt" scope="request"/>
<c:set var="pageCss" value="pages/vouchers.css" scope="request"/>
<c:set var="pageJs" value="pages/vouchers.js" scope="request"/>
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
                <li class="breadcrumb-item active" aria-current="page">Vouchers</li>
            </ol>
        </nav>

        <!-- Page Header -->
        <div class="vouchers-header text-center mb-5">
            <h1 class="page-title">
                <i class="fas fa-gift text-danger me-2"></i>
                Vouchers & Khuyến mãi
            </h1>
            <p class="page-subtitle text-muted">
                Tận hưởng các ưu đãi đặc biệt dành riêng cho bạn
            </p>
        </div>

        <!-- Filter Tabs -->
        <div class="voucher-filters mb-4">
            <ul class="nav nav-pills justify-content-center" id="voucherTabs" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active" id="all-tab" data-filter="all" type="button">
                        <i class="fas fa-list me-2"></i>
                        Tất cả
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="available-tab" data-filter="available" type="button">
                        <i class="fas fa-check-circle me-2"></i>
                        Có thể sử dụng
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="saved-tab" data-filter="saved" type="button">
                        <i class="fas fa-bookmark me-2"></i>
                        Đã lưu
                    </button>
                </li>
            </ul>
        </div>

        <!-- Loading State -->
        <div id="loadingState" class="text-center py-5">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Đang tải...</span>
            </div>
            <p class="mt-3 text-muted">Đang tải vouchers...</p>
        </div>

        <!-- Vouchers Grid -->
        <div id="vouchersGrid" class="vouchers-grid" style="display: none;">
            <!-- Vouchers will be loaded by JS -->
        </div>

        <!-- Empty State -->
        <div id="emptyState" class="empty-state text-center py-5" style="display: none;">
            <i class="fas fa-ticket-alt fa-4x text-muted mb-3"></i>
            <h4>Không có vouchers nào</h4>
            <p class="text-muted">Hiện tại không có vouchers phù hợp.</p>
        </div>

        <!-- Voucher Info Box -->
        <div class="voucher-info-box mt-5">
            <div class="card shadow-sm">
                <div class="card-body">
                    <h5 class="card-title">
                        <i class="fas fa-info-circle text-primary me-2"></i>
                        Hướng dẫn sử dụng voucher
                    </h5>
                    <ul class="voucher-instructions">
                        <li><i class="fas fa-check text-success me-2"></i>Nhấn "Lưu voucher" để lưu mã giảm giá vào tài khoản của bạn</li>
                        <li><i class="fas fa-check text-success me-2"></i>Áp dụng voucher khi thanh toán để nhận ưu đãi</li>
                        <li><i class="fas fa-check text-success me-2"></i>Mỗi voucher chỉ có thể sử dụng một lần</li>
                        <li><i class="fas fa-check text-success me-2"></i>Kiểm tra điều kiện áp dụng trước khi sử dụng</li>
                        <li><i class="fas fa-check text-success me-2"></i>Voucher không thể hoàn lại sau khi đã sử dụng</li>
                    </ul>
                </div>
            </div>
        </div>
    </main>
    
    <%@ include file="/WEB-INF/views/common/footer.jspf" %>
    
    <!-- Voucher Detail Modal -->
    <div class="modal fade" id="voucherDetailModal" tabindex="-1" aria-labelledby="voucherDetailModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="voucherDetailModalLabel">Chi tiết Voucher</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div class="voucher-detail-content">
                        <div class="detail-code mb-3">
                            <label class="fw-bold">Mã voucher:</label>
                            <div class="code-display" id="modalVoucherCode">
                                <!-- Code will be loaded by JS -->
                            </div>
                        </div>
                        <div class="detail-description mb-3">
                            <label class="fw-bold">Mô tả:</label>
                            <p id="modalVoucherDescription"><!-- Description will be loaded by JS --></p>
                        </div>
                        <div class="detail-discount mb-3">
                            <label class="fw-bold">Giảm giá:</label>
                            <p id="modalVoucherDiscount" class="text-danger fs-5 fw-bold"><!-- Discount will be loaded by JS --></p>
                        </div>
                        <div class="detail-conditions mb-3">
                            <label class="fw-bold">Điều kiện:</label>
                            <ul id="modalVoucherConditions">
                                <!-- Conditions will be loaded by JS -->
                            </ul>
                        </div>
                        <div class="detail-expiry mb-3">
                            <label class="fw-bold">Hiệu lực:</label>
                            <p id="modalVoucherExpiry"><!-- Expiry will be loaded by JS --></p>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                    <button type="button" class="btn btn-primary" id="saveVoucherBtn">
                        <i class="fas fa-bookmark me-2"></i>
                        Lưu voucher
                    </button>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- Custom JS -->
    <!-- Page-specific JS -->
    <script src="${pageContext.request.contextPath}/static/js/pages/vouchers.js"></script>
</body>
</html>

