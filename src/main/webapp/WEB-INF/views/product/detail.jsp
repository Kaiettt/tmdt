<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Chi tiết sản phẩm" scope="request"/>
<c:set var="pageDescription" value="Xem thông tin chi tiết sản phẩm, thông số kỹ thuật và đánh giá" scope="request"/>
<c:set var="pageCss" value="pages/product-detail.css" scope="request"/>
<c:set var="pageJs" value="pages/product-detail.js" scope="request"/>
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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/components/product.css">
    <c:if test="${not empty pageCss}">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/${pageCss}">
    </c:if>
</head>
<body>
    <%@ include file="/WEB-INF/views/common/header.jspf" %>
    
    <main class="main-content">
        <!-- Breadcrumb -->
        <div class="container">
            <div class="breadcrumb" id="breadcrumb">
                <a href="${pageContext.request.contextPath}/">Trang chủ</a>
                <i class="fas fa-chevron-right"></i>
                <a href="${pageContext.request.contextPath}/products">Sản phẩm</a>
                <i class="fas fa-chevron-right"></i>
                <span id="breadcrumb-category">...</span>
                <i class="fas fa-chevron-right"></i>
                <span id="breadcrumb-product">...</span>
            </div>
        </div>

        <!-- Product Detail Section -->
        <div class="container mt-4" id="product-detail-container">
            <!-- Loading state -->
            <div id="loading-state" class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Đang tải...</span>
                </div>
                <p class="mt-3">Đang tải thông tin sản phẩm...</p>
            </div>

            <!-- Error state -->
            <div id="error-state" class="alert alert-danger d-none" role="alert">
                <h4 class="alert-heading">Lỗi!</h4>
                <p id="error-message">Không thể tải thông tin sản phẩm.</p>
                <hr>
                <a href="${pageContext.request.contextPath}/products" class="btn btn-primary">
                    <i class="fas fa-arrow-left"></i> Quay lại danh sách sản phẩm
                </a>
            </div>

            <!-- Product content (will be populated by JavaScript) -->
            <div id="product-content" class="d-none">
                <div class="row">
                    <!-- Left: Product Images -->
                    <div class="col-lg-5">
                        <div class="product-gallery">
                            <div class="main-image-container">
                                <img id="main-product-image" 
                                     src="" 
                                     alt="Product Image" 
                                     class="main-product-image">
                                <div class="product-badges" id="product-badges">
                                    <!-- Badges will be added by JS -->
                                </div>
                            </div>
                            <div class="thumbnail-images" id="thumbnail-images">
                                <!-- Thumbnails will be added by JS -->
                            </div>
                        </div>
                    </div>

                    <!-- Right: Product Info -->
                    <div class="col-lg-7">
                        <div class="product-info-section">
                            <!-- Product Name -->
                            <h1 class="product-name" id="product-name">Loading...</h1>

                            <!-- Rating -->
                            <div class="product-rating-summary" id="rating-summary">
                                <div class="rating-stars" id="rating-stars">
                                    <!-- Stars will be added by JS -->
                                </div>
                                <span class="rating-text" id="rating-text">(0 đánh giá)</span>
                                <span class="separator">|</span>
                                <span class="sold-count" id="sold-count">Đã bán: 0</span>
                            </div>

                            <!-- Price -->
                            <div class="product-price-section">
                                <div class="price-box">
                                    <span class="price-current" id="price-current">0₫</span>
                                    <span class="price-original d-none" id="price-original">0₫</span>
                                    <span class="discount-badge d-none" id="discount-badge">-0%</span>
                                </div>
                            </div>

                            <!-- Key Specs -->
                            <div class="key-specs" id="key-specs">
                                <h3>Thông số nổi bật</h3>
                                <div class="specs-grid" id="specs-grid">
                                    <!-- Specs will be added by JS -->
                                </div>
                            </div>

                            <!-- Stock Status -->
                            <div class="stock-status" id="stock-status">
                                <i class="fas fa-check-circle"></i>
                                <span id="stock-text">Còn hàng</span>
                            </div>

                            <!-- Quantity Selector -->
                            <div class="quantity-section">
                                <label>Số lượng:</label>
                                <div class="quantity-controls">
                                    <button type="button" class="btn-quantity-decrease" id="btn-decrease">
                                        <i class="fas fa-minus"></i>
                                    </button>
                                    <input type="number" 
                                           id="quantity-input" 
                                           class="quantity-input" 
                                           value="1" 
                                           min="1" 
                                           max="10">
                                    <button type="button" class="btn-quantity-increase" id="btn-increase">
                                        <i class="fas fa-plus"></i>
                                    </button>
                                </div>
                                <span class="stock-available" id="stock-available">Còn 0 sản phẩm</span>
                            </div>

                            <!-- Action Buttons -->
                            <div class="product-actions">
                                <button class="btn btn-add-cart" id="btn-add-cart">
                                    <i class="fas fa-shopping-cart"></i>
                                    Thêm vào giỏ hàng
                                </button>
                                <button class="btn btn-buy-now" id="btn-buy-now">
                                    <i class="fas fa-bolt"></i>
                                    Mua ngay
                                </button>
                                <button class="btn btn-wishlist" id="btn-wishlist">
                                    <i class="far fa-heart"></i>
                                </button>
                            </div>

                            <!-- Services -->
                            <div class="product-services">
                                <div class="service-item">
                                    <i class="fas fa-shield-alt"></i>
                                    <span>Bảo hành chính hãng 12 tháng</span>
                                </div>
                                <div class="service-item">
                                    <i class="fas fa-truck"></i>
                                    <span>Giao hàng miễn phí toàn quốc</span>
                                </div>
                                <div class="service-item">
                                    <i class="fas fa-undo"></i>
                                    <span>Đổi trả trong 7 ngày</span>
                                </div>
                                <div class="service-item">
                                    <i class="fas fa-headset"></i>
                                    <span>Hỗ trợ 24/7</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Product Details Tabs -->
                <div class="row mt-5">
                    <div class="col-12">
                        <ul class="nav nav-tabs product-tabs" id="product-tabs" role="tablist">
                            <li class="nav-item" role="presentation">
                                <button class="nav-link active" 
                                        id="description-tab" 
                                        data-bs-toggle="tab" 
                                        data-bs-target="#description" 
                                        type="button" 
                                        role="tab">
                                    Mô tả sản phẩm
                                </button>
                            </li>
                            <li class="nav-item" role="presentation">
                                <button class="nav-link" 
                                        id="specs-tab" 
                                        data-bs-toggle="tab" 
                                        data-bs-target="#specs" 
                                        type="button" 
                                        role="tab">
                                    Thông số kỹ thuật
                                </button>
                            </li>
                            <li class="nav-item" role="presentation">
                                <button class="nav-link" 
                                        id="reviews-tab" 
                                        data-bs-toggle="tab" 
                                        data-bs-target="#reviews" 
                                        type="button" 
                                        role="tab">
                                    Đánh giá (<span id="review-count-tab">0</span>)
                                </button>
                            </li>
                        </ul>
                        <div class="tab-content product-tab-content" id="product-tab-content">
                            <!-- Description Tab -->
                            <div class="tab-pane fade show active" 
                                 id="description" 
                                 role="tabpanel">
                                <div class="description-content" id="description-content">
                                    <!-- Description will be added by JS -->
                                </div>
                            </div>

                            <!-- Specs Tab -->
                            <div class="tab-pane fade" 
                                 id="specs" 
                                 role="tabpanel">
                                <div class="specs-table-container">
                                    <table class="table specs-table" id="specs-table">
                                        <tbody>
                                            <!-- Specs will be added by JS -->
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <!-- Reviews Tab -->
                            <div class="tab-pane fade" 
                                 id="reviews" 
                                 role="tabpanel">
                                <div class="reviews-section">
                                    <!-- Reviews Summary & Write Review Button -->
                                    <div class="reviews-header mb-4">
                                        <div class="reviews-summary" id="reviews-summary">
                                            <div class="rating-overview">
                                                <div class="rating-score">
                                                    <span class="score" id="avg-rating">0.0</span>
                                                    <div class="stars" id="summary-stars">
                                                        <!-- Stars -->
                                                    </div>
                                                    <span class="total-reviews" id="total-reviews">0 đánh giá</span>
                                                </div>
                                            </div>
                                        </div>
                                        
                                        <!-- Write Review Button (for logged-in users) -->
                                        <div class="write-review-action d-none" id="write-review-action">
                                            <button type="button" class="btn btn-primary" id="open-review-modal-btn">
                                                <i class="fas fa-star"></i>
                                                Đánh giá ngay
                                            </button>
                                        </div>
                                    </div>

                                    <!-- Reviews List -->
                                    <div class="reviews-list" id="reviews-list">
                                        <!-- Reviews will be added by JS -->
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Related Products -->
                <div class="row mt-5">
                    <div class="col-12">
                        <h2 class="section-title">Sản phẩm tương tự</h2>
                        <div class="product-grid" id="related-products">
                            <!-- Related products will be added by JS -->
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>
    
    <!-- Review Modal (giống form thêm sản phẩm admin) -->
    <div class="modal fade" id="reviewModal" tabindex="-1" aria-labelledby="reviewModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="reviewModalLabel">
                        <i class="fas fa-star text-warning"></i>
                        Đánh giá sản phẩm
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form id="review-modal-form">
                        <!-- Rating Stars -->
                        <div class="mb-4">
                            <label class="form-label fw-bold">
                                Đánh giá của bạn <span class="text-danger">*</span>
                            </label>
                            <div class="rating-input-modal d-flex gap-2 fs-3" id="rating-input-modal">
                                <i class="far fa-star text-warning" data-rating="1" style="cursor: pointer;"></i>
                                <i class="far fa-star text-warning" data-rating="2" style="cursor: pointer;"></i>
                                <i class="far fa-star text-warning" data-rating="3" style="cursor: pointer;"></i>
                                <i class="far fa-star text-warning" data-rating="4" style="cursor: pointer;"></i>
                                <i class="far fa-star text-warning" data-rating="5" style="cursor: pointer;"></i>
                            </div>
                            <input type="hidden" id="rating-value-modal" name="rating" value="0">
                            <div class="invalid-feedback d-block" id="rating-error" style="display: none !important;">
                                Vui lòng chọn số sao đánh giá
                            </div>
                        </div>

                        <!-- Comment Textarea -->
                        <div class="mb-3">
                            <label for="review-comment-modal" class="form-label fw-bold">
                                Nhận xét của bạn <span class="text-danger">*</span>
                            </label>
                            <textarea class="form-control" 
                                      id="review-comment-modal" 
                                      name="comment" 
                                      rows="5" 
                                      placeholder="Chia sẻ trải nghiệm của bạn về sản phẩm này..."
                                      maxlength="1000"></textarea>
                            <div class="form-text">Tối đa 1000 ký tự</div>
                            <div class="invalid-feedback" id="comment-error">
                                Vui lòng nhập nhận xét
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                        <i class="fas fa-times"></i>
                        Hủy
                    </button>
                    <button type="button" class="btn btn-primary" id="submit-review-btn">
                        <i class="fas fa-paper-plane"></i>
                        Gửi đánh giá
                    </button>
                </div>
            </div>
        </div>
    </div>
    
    <%@ include file="/WEB-INF/views/common/footer.jspf" %>
    
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- Utility JS -->
    <script src="${pageContext.request.contextPath}/static/js/utils.js"></script>
    <!-- API JS -->
    <script src="${pageContext.request.contextPath}/static/js/api.js"></script>
    <!-- Page-specific JS -->
    <script src="${pageContext.request.contextPath}/static/js/pages/product-detail.js"></script>
</body>
</html>

