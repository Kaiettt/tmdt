<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Tra cứu đơn hàng" scope="request"/>
<c:set var="pageCss" value="pages/order-lookup.css" scope="request"/>
<c:set var="pageJs" value="pages/order-lookup.js" scope="request"/>
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
                <li class="breadcrumb-item active" aria-current="page">Tra cứu đơn hàng</li>
            </ol>
        </nav>

        <div class="row justify-content-center">
            <div class="col-lg-6 col-md-8">
                <div class="lookup-card card shadow">
                    <div class="card-body p-4">
                        <div class="text-center mb-4">
                            <div class="icon-wrapper mb-3">
                                <i class="fas fa-search-location fa-3x text-primary"></i>
                            </div>
                            <h2 class="card-title mb-2">Tra cứu đơn hàng</h2>
                            <p class="text-muted">
                                Nhập mã đơn hàng và email để kiểm tra trạng thái đơn hàng của bạn
                            </p>
                        </div>

                        <!-- Lookup Form -->
                        <form id="order-lookup-form" class="needs-validation" novalidate>
                            <div class="mb-3">
                                <label for="orderCode" class="form-label">
                                    <i class="fas fa-barcode me-2"></i>Mã đơn hàng <span class="text-danger">*</span>
                                </label>
                                <input type="text" 
                                       class="form-control form-control-lg" 
                                       id="orderCode" 
                                       placeholder="Ví dụ: UTEHUB-1234567890"
                                       required>
                                <div class="invalid-feedback">
                                    Vui lòng nhập mã đơn hàng
                                </div>
                            </div>

                            <div class="mb-4">
                                <label for="email" class="form-label">
                                    <i class="fas fa-envelope me-2"></i>Email <span class="text-danger">*</span>
                                </label>
                                <input type="email" 
                                       class="form-control form-control-lg" 
                                       id="email" 
                                       placeholder="email@example.com"
                                       required>
                                <div class="invalid-feedback">
                                    Vui lòng nhập địa chỉ email hợp lệ
                                </div>
                                <small class="form-text text-muted">
                                    Email bạn đã sử dụng khi đặt hàng
                                </small>
                            </div>

                            <button type="submit" class="btn btn-primary btn-lg w-100" id="lookup-btn">
                                <i class="fas fa-search me-2"></i>Tra cứu đơn hàng
                            </button>
                        </form>

                        <!-- Result Section (Hidden initially) -->
                        <div id="order-result" class="mt-4" style="display: none;">
                            <hr class="my-4">
                            <div id="order-details">
                                <!-- Order details will be rendered here by JavaScript -->
                            </div>
                        </div>

                        <!-- Error Message -->
                        <div id="error-message" class="alert alert-danger mt-4" style="display: none;">
                            <i class="fas fa-exclamation-circle me-2"></i>
                            <span id="error-text"></span>
                        </div>
                    </div>
                </div>

                <!-- Help Section -->
                <div class="help-section card mt-4">
                    <div class="card-body">
                        <h5 class="card-title">
                            <i class="fas fa-question-circle me-2"></i>Cần trợ giúp?
                        </h5>
                        <ul class="list-unstyled mb-0">
                            <li class="mb-2">
                                <i class="fas fa-check-circle text-success me-2"></i>
                                Mã đơn hàng được gửi qua email sau khi đặt hàng thành công
                            </li>
                            <li class="mb-2">
                                <i class="fas fa-check-circle text-success me-2"></i>
                                Sử dụng đúng địa chỉ email đã đăng ký
                            </li>
                            <li>
                                <i class="fas fa-check-circle text-success me-2"></i>
                                Liên hệ hotline: <strong>1900 xxxx</strong> nếu gặp vấn đề
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
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

