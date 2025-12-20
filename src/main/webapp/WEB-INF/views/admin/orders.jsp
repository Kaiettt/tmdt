<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Quản lý Đơn hàng - UTE Admin</title>

  <!-- Favicon -->
  <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/favicon.png">

  <!-- Font Awesome -->
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

  <!-- Google Fonts -->
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;600;700&display=swap" rel="stylesheet">

  <!-- Admin CSS -->
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/pages/admin.css">
  
  <style>
    /* Additional styles for orders page */
    .order-detail-modal .modal-dialog {
      max-width: 800px;
    }
    
    .order-items-list {
      margin: 1rem 0;
    }
    
    .order-item {
      display: flex;
      gap: 1rem;
      padding: 1rem;
      background: var(--admin-bg);
      border-radius: 8px;
      margin-bottom: 0.75rem;
    }
    
    .order-item-image {
      width: 80px;
      height: 80px;
      object-fit: cover;
      border-radius: 8px;
    }
    
    .order-item-info {
      flex: 1;
    }
    
    .order-item-name {
      font-weight: 600;
      margin-bottom: 0.25rem;
    }
    
    .order-item-price {
      color: var(--admin-text-light);
      font-size: 0.875rem;
    }
    
    .order-summary {
      background: var(--admin-bg);
      padding: 1rem;
      border-radius: 8px;
      margin-top: 1rem;
    }
    
    .summary-row {
      display: flex;
      justify-content: space-between;
      padding: 0.5rem 0;
      border-bottom: 1px solid var(--admin-border);
    }
    
    .summary-row:last-child {
      border-bottom: none;
      font-weight: 700;
      font-size: 1.125rem;
      color: var(--admin-primary);
    }
    
    .status-actions {
      display: flex;
      gap: 0.5rem;
      margin-top: 1rem;
    }
    
    /* Modal styles - giống products.jsp */
    .modal {
      display: none;
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.5);
      z-index: 10000;
      align-items: center;
      justify-content: center;
    }
    
    .modal.show {
      display: flex;
    }
    
    .modal-dialog {
      background: white;
      border-radius: 12px;
      max-width: 600px;
      width: 90%;
      max-height: 90vh;
      overflow-y: auto;
    }
    
    .modal-header {
      padding: 1.5rem;
      border-bottom: 1px solid var(--admin-border);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .modal-title {
      font-size: 1.25rem;
      font-weight: 700;
    }
    
    .btn-close {
      background: none;
      border: none;
      font-size: 1.5rem;
      cursor: pointer;
      color: var(--admin-text-light);
    }
    
    .modal-body {
      padding: 1.5rem;
    }
    
    .form-group {
      margin-bottom: 1.5rem;
    }
    
    .form-label {
      display: block;
      margin-bottom: 0.5rem;
      font-weight: 600;
      font-size: 0.875rem;
    }
    
    .form-control {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid var(--admin-border);
      border-radius: 8px;
      font-size: 0.875rem;
    }
    
    .form-control:focus {
      outline: none;
      border-color: var(--admin-primary);
    }
    
    textarea.form-control {
      resize: vertical;
      min-height: 100px;
    }
    
    .modal-footer {
      padding: 1.5rem;
      border-top: 1px solid var(--admin-border);
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
    }
  </style>
</head>
<body class="admin-body">
  <!-- Admin Layout -->
  <div class="admin-layout">
    <!-- Sidebar -->
    <%@ include file="/WEB-INF/views/common/admin-sidebar.jspf" %>

    <!-- Main Content -->
    <main class="admin-main">
      <!-- Top Bar -->
      <div class="admin-topbar">
        <div class="topbar-left">
          <button class="btn-toggle-sidebar" id="toggleSidebar">
            <i class="fas fa-bars"></i>
          </button>
          <h1 class="page-title">Quản lý Đơn hàng</h1>
        </div>
        <div class="topbar-right">
          <div class="topbar-date">
            <i class="far fa-calendar"></i>
            <span id="currentDate"></span>
          </div>
        </div>
      </div>

      <!-- Orders Content -->
      <div class="admin-content">
        <!-- Filters & Search -->
        <div class="dashboard-card" style="margin-bottom: 1.5rem;">
          <div class="card-body">
            <div style="display: flex; gap: 1rem; flex-wrap: wrap;">
              <div style="flex: 1; min-width: 250px;">
                <input type="text" id="searchInput" class="form-control" placeholder="Tìm theo mã đơn, tên khách hàng...">
              </div>
              <div style="min-width: 200px;">
                <select id="statusFilter" class="form-control">
                  <option value="">Tất cả trạng thái</option>
                  <option value="PENDING">Chờ xác nhận</option>
                  <option value="PROCESSING">Đang xử lý</option>
                  <option value="SHIPPED">Đang giao</option>
                  <option value="DELIVERED">Đã giao</option>
                  <option value="CANCELLED">Đã hủy</option>
                </select>
              </div>
              <div style="min-width: 200px;">
                <select id="sortBy" class="form-control">
                  <option value="createdAt:desc">Mới nhất</option>
                  <option value="createdAt:asc">Cũ nhất</option>
                  <option value="totalAmount:desc">Giá trị cao</option>
                  <option value="totalAmount:asc">Giá trị thấp</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <!-- Orders Table -->
        <div class="dashboard-card">
          <div class="card-body">
            <div class="table-responsive">
              <table class="admin-table">
                <thead>
                  <tr>
                    <th style="width: 100px;">Mã đơn</th>
                    <th>Khách hàng</th>
                    <th>Địa chỉ giao hàng</th>
                    <th>Tổng tiền</th>
                    <th>Trạng thái</th>
                    <th>Ngày đặt</th>
                    <th style="width: 150px;">Thao tác</th>
                  </tr>
                </thead>
                <tbody id="ordersTableBody">
                  <tr>
                    <td colspan="7" class="text-center">
                      <div class="loading-spinner">
                        <i class="fas fa-spinner fa-spin"></i>
                        <span>Đang tải...</span>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- Pagination -->
            <div id="pagination" style="margin-top: 1.5rem; display: flex; justify-content: center; gap: 0.5rem;">
              <!-- Pagination will be rendered here -->
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>

  <!-- Order Detail Modal -->
  <div id="orderDetailModal" class="modal">
    <div class="modal-dialog order-detail-modal">
      <div class="modal-header">
        <h3 class="modal-title">Chi tiết đơn hàng <span id="modalOrderCode"></span></h3>
        <button class="btn-close" id="btnCloseModal">&times;</button>
      </div>
      <div class="modal-body">
        <!-- Customer Info -->
        <div style="margin-bottom: 1.5rem;">
          <h4 style="font-size: 1rem; font-weight: 600; margin-bottom: 0.75rem;">Thông tin khách hàng</h4>
          <div style="background: var(--admin-bg); padding: 1rem; border-radius: 8px;">
            <p><strong>Họ tên:</strong> <span id="customerName"></span></p>
            <p><strong>Email:</strong> <span id="customerEmail"></span></p>
            <p><strong>Số điện thoại:</strong> <span id="customerPhone"></span></p>
          </div>
        </div>

        <!-- Shipping Address -->
        <div style="margin-bottom: 1.5rem;">
          <h4 style="font-size: 1rem; font-weight: 600; margin-bottom: 0.75rem;">Địa chỉ giao hàng</h4>
          <div style="background: var(--admin-bg); padding: 1rem; border-radius: 8px;">
            <p id="shippingAddress"></p>
          </div>
        </div>

        <!-- Order Items -->
        <div style="margin-bottom: 1.5rem;">
          <h4 style="font-size: 1rem; font-weight: 600; margin-bottom: 0.75rem;">Sản phẩm</h4>
          <div class="order-items-list" id="orderItemsList">
            <!-- Order items will be rendered here -->
          </div>
        </div>

        <!-- Order Summary -->
        <div class="order-summary">
          <div class="summary-row">
            <span>Tạm tính:</span>
            <span id="subtotal">0 ₫</span>
          </div>
          <div class="summary-row">
            <span>Phí vận chuyển:</span>
            <span id="shippingFee">0 ₫</span>
          </div>
          <div class="summary-row">
            <span>Giảm giá:</span>
            <span id="discount">0 ₫</span>
          </div>
          <div class="summary-row">
            <span>Tổng cộng:</span>
            <span id="totalAmount">0 ₫</span>
          </div>
        </div>

        <!-- Status Actions -->
        <div class="status-actions">
          <button class="btn-action btn-primary" id="btnProcessOrder" style="display: none;">
            <i class="fas fa-check"></i> Xác nhận đơn
          </button>
          <button class="btn-action btn-primary" id="btnShipOrder" style="display: none;">
            <i class="fas fa-shipping-fast"></i> Giao hàng
          </button>
          <button class="btn-action btn-success" id="btnDeliverOrder" style="display: none;">
            <i class="fas fa-check-circle"></i> Đã giao
          </button>
          <button class="btn-action btn-danger" id="btnCancelOrder" style="display: none;">
            <i class="fas fa-times"></i> Hủy đơn
          </button>
        </div>
      </div>
    </div>
  </div>

  <%@ include file="/WEB-INF/views/common/admin-footer.jspf" %>
  
  <!-- Page-specific JavaScript -->
  <script src="${pageContext.request.contextPath}/static/js/pages/admin-orders.js"></script>

  <script>
    // Toggle sidebar on mobile
    document.getElementById('toggleSidebar')?.addEventListener('click', () => {
      document.querySelector('.admin-sidebar')?.classList.toggle('show');
    });

    // Display current date
    const dateEl = document.getElementById('currentDate');
    if (dateEl) {
      const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
      dateEl.textContent = new Date().toLocaleDateString('vi-VN', options);
    }
  </script>
</body>
</html>

