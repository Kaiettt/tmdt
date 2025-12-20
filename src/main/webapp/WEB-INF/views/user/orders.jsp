<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%> <%@ taglib prefix="c"
uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Đơn hàng của tôi - UTE Phone Hub</title>

    <!-- Favicon -->
    <link
      rel="icon"
      type="image/png"
      href="${pageContext.request.contextPath}/static/favicon.png"
    />
    <link
      rel="shortcut icon"
      type="image/png"
      href="${pageContext.request.contextPath}/static/favicon.png"
    />

    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/static/css/pages/profile-modern.css"
    />
    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/static/css/pages/user-orders.css"
    />

    <!-- Google Fonts - Roboto -->
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link
      href="https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,100;0,300;0,400;0,500;0,700;0,900;1,100;1,300;1,400;1,500;1,700;1,900&display=swap"
      rel="stylesheet"
    />

    <link
      href="https://unpkg.com/boxicons@2.1.4/css/boxicons.min.css"
      rel="stylesheet"
    />
    <link
      rel="stylesheet"
      href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css"
    />
  </head>
  <body>
    <!-- Profile Header (reuse from profile page) -->
    <div class="profile-header">
      <div class="profile-header-content">
        <!-- Logo -->
        <a href="${pageContext.request.contextPath}/" class="profile-logo">
          <img src="${pageContext.request.contextPath}/static/images/logo.png" alt="UTE Phone Hub" style="height: 45px; width: auto;">
          <span class="logo-text">UTE Phone Hub</span>
        </a>

        <!-- Page Title -->
        <div class="profile-title">
          <i class="fas fa-shopping-bag"></i>
          <h1>Đơn hàng của tôi</h1>
        </div>

        <!-- Actions -->
        <div class="profile-header-actions">
          <a href="${pageContext.request.contextPath}/profile" class="btn-back">
            <i class="fas fa-user"></i>
            <span>Tài khoản</span>
          </a>
          <a href="${pageContext.request.contextPath}/" class="btn-back">
            <i class="fas fa-arrow-left"></i>
            <span>Quay lại</span>
          </a>
          <button class="btn-logout" id="logoutBtn">
            <i class="fas fa-sign-out-alt"></i>
            <span>Đăng xuất</span>
          </button>
        </div>
      </div>
    </div>

    <div class="profile-container">
      <!-- Sidebar Navigation -->
      <div class="sidebar">
        <div class="user-info">
          <div class="avatar">
            <i class="bx bxs-user-circle"></i>
          </div>
          <h3 id="userName">Loading...</h3>
          <p id="userEmail">Loading...</p>
        </div>
        <nav class="sidebar-nav">
          <a href="${pageContext.request.contextPath}/profile" class="nav-item">
            <i class="bx bxs-user"></i>
            <span>Thông tin cá nhân</span>
          </a>
          <a href="${pageContext.request.contextPath}/orders" class="nav-item active">
            <i class="bx bxs-shopping-bag"></i>
            <span>Đơn hàng của tôi</span>
          </a>
        </nav>
      </div>

      <!-- Main Content -->
      <div class="content">
        <!-- Order Status Filter -->
        <div class="order-filter">
          <button class="filter-btn active" data-status="ALL">
            <i class="bx bx-list-ul"></i>
            Tất cả đơn
          </button>
          <button class="filter-btn" data-status="PENDING">
            <i class="bx bx-time"></i>
            Chờ xác nhận
          </button>
          <button class="filter-btn" data-status="CONFIRMED">
            <i class="bx bx-check-circle"></i>
            Đã xác nhận
          </button>
          <button class="filter-btn" data-status="SHIPPING">
            <i class="bx bx-package"></i>
            Đang giao
          </button>
          <button class="filter-btn" data-status="DELIVERED">
            <i class="bx bx-check-double"></i>
            Đã giao
          </button>
          <button class="filter-btn" data-status="CANCELLED">
            <i class="bx bx-x-circle"></i>
            Đã hủy
          </button>
        </div>

        <!-- Loading State -->
        <div id="loadingState" class="loading-state">
          <div class="spinner"></div>
          <p>Đang tải đơn hàng...</p>
        </div>

        <!-- Error State -->
        <div id="errorState" class="error-state" style="display: none;">
          <i class="bx bx-error-circle"></i>
          <h3>Không thể tải đơn hàng</h3>
          <p id="errorMessage">Đã xảy ra lỗi. Vui lòng thử lại sau.</p>
          <button class="btn btn-primary" onclick="location.reload()">
            <i class="bx bx-refresh"></i> Thử lại
          </button>
        </div>

        <!-- Empty State -->
        <div id="emptyState" class="empty-state" style="display: none;">
          <i class="bx bx-cart"></i>
          <h3>Chưa có đơn hàng nào</h3>
          <p>Bạn chưa có đơn hàng nào. Hãy bắt đầu mua sắm ngay!</p>
          <a href="${pageContext.request.contextPath}/products" class="btn btn-primary">
            <i class="bx bx-shopping-bag"></i> Mua sắm ngay
          </a>
        </div>

        <!-- Orders List -->
        <div id="ordersList" class="orders-list">
          <!-- Orders will be loaded dynamically -->
        </div>

        <!-- Pagination -->
        <div id="pagination" class="pagination" style="display: none;">
          <!-- Pagination will be loaded dynamically -->
        </div>
      </div>
    </div>

    <!-- Order Detail Modal -->
    <div id="orderDetailModal" class="modal">
      <div class="modal-content modal-large">
        <span class="close">&times;</span>
        <div class="modal-header">
          <h2>Chi tiết đơn hàng</h2>
          <div id="orderStatus" class="order-status-badge"></div>
        </div>
        <div class="modal-body">
          <!-- Order Info -->
          <div class="order-info-section">
            <div class="info-row">
              <span class="label">Mã đơn hàng:</span>
              <span class="value" id="modalOrderCode"></span>
            </div>
            <div class="info-row">
              <span class="label">Ngày đặt:</span>
              <span class="value" id="modalOrderDate"></span>
            </div>
            <div class="info-row">
              <span class="label">Trạng thái:</span>
              <span class="value" id="modalOrderStatus"></span>
            </div>
            <div class="info-row">
              <span class="label">Hình thức thanh toán:</span>
              <span class="value" id="modalPaymentMethod"></span>
            </div>
          </div>

          <!-- Shipping Address -->
          <div class="shipping-address-section">
            <h3><i class="bx bx-map"></i> Địa chỉ giao hàng</h3>
            <div class="address-card">
              <p class="recipient-name" id="modalRecipientName"></p>
              <p class="recipient-phone" id="modalRecipientPhone"></p>
              <p class="recipient-address" id="modalRecipientAddress"></p>
            </div>
          </div>

          <!-- Order Items -->
          <div class="order-items-section">
            <h3><i class="bx bx-package"></i> Sản phẩm</h3>
            <div id="modalOrderItems" class="modal-order-items">
              <!-- Items will be loaded dynamically -->
            </div>
          </div>

          <!-- Order Summary -->
          <div class="order-summary-section">
            <div class="summary-row">
              <span class="label">Tổng tiền hàng:</span>
              <span class="value" id="modalSubtotal"></span>
            </div>
            <div class="summary-row">
              <span class="label">Phí vận chuyển:</span>
              <span class="value" id="modalShippingFee"></span>
            </div>
            <div class="summary-row discount" id="modalDiscountRow" style="display: none;">
              <span class="label">Giảm giá:</span>
              <span class="value" id="modalDiscount"></span>
            </div>
            <div class="summary-row total">
              <span class="label">Tổng thanh toán:</span>
              <span class="value" id="modalTotal"></span>
            </div>
          </div>

          <!-- Order Actions -->
          <div class="order-actions-section" id="modalOrderActions">
            <!-- Actions will be loaded dynamically based on order status -->
          </div>
        </div>
      </div>
    </div>

    <!-- Confirm Cancel Modal -->
    <div id="confirmCancelModal" class="modal">
      <div class="modal-content modal-small">
        <span class="close">&times;</span>
        <div class="modal-header">
          <h3>Xác nhận hủy đơn hàng</h3>
        </div>
        <div class="modal-body">
          <p>Bạn có chắc chắn muốn hủy đơn hàng này không?</p>
          <div class="form-group">
            <label for="cancelReason">Lý do hủy:</label>
            <textarea id="cancelReason" rows="3" placeholder="Nhập lý do hủy đơn (không bắt buộc)"></textarea>
          </div>
          <div class="modal-actions">
            <button class="btn btn-secondary" id="btnCancelNo">Không</button>
            <button class="btn btn-danger" id="btnCancelYes">
              <i class="bx bx-x"></i> Hủy đơn hàng
            </button>
          </div>
        </div>
      </div>
    </div>

    <%@ include file="/WEB-INF/views/common/footer.jspf" %>

    <!-- Page-specific JavaScript -->
    <script src="${pageContext.request.contextPath}/static/js/pages/user-orders.js"></script>
    
    <!-- Logout Handler -->
    <script>
      document
        .getElementById("logoutBtn")
        .addEventListener("click", function () {
          // Use global logout function from auth.js
          if (typeof logout === "function") {
            logout();
          } else {
            // Fallback: clear storage and redirect
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
            localStorage.removeItem("user");
            window.location.href = "${pageContext.request.contextPath}/";
          }
        });
    </script>
  </body>
</html>

