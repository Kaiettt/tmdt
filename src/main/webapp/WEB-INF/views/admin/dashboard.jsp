<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Dashboard - UTE Admin</title>

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
          <h1 class="page-title">Dashboard</h1>
        </div>
        <div class="topbar-right">
          <div class="topbar-date">
            <i class="far fa-calendar"></i>
            <span id="currentDate"></span>
          </div>
        </div>
      </div>

      <!-- Dashboard Content -->
      <div class="admin-content">
        <!-- Statistics Cards -->
        <div class="stats-grid">
          <!-- Total Revenue -->
          <div class="stat-card stat-revenue">
            <div class="stat-icon">
              <i class="fas fa-dollar-sign"></i>
            </div>
            <div class="stat-details">
              <h3 class="stat-value" id="totalRevenue">0 ₫</h3>
              <p class="stat-label">Tổng doanh thu</p>
              <span class="stat-change positive" id="revenueChange">+0%</span>
            </div>
          </div>

          <!-- Total Orders -->
          <div class="stat-card stat-orders">
            <div class="stat-icon">
              <i class="fas fa-shopping-cart"></i>
            </div>
            <div class="stat-details">
              <h3 class="stat-value" id="totalOrders">0</h3>
              <p class="stat-label">Tổng đơn hàng</p>
              <span class="stat-change positive" id="ordersChange">+0%</span>
            </div>
          </div>

          <!-- Total Products -->
          <div class="stat-card stat-products">
            <div class="stat-icon">
              <i class="fas fa-box"></i>
            </div>
            <div class="stat-details">
              <h3 class="stat-value" id="totalProducts">0</h3>
              <p class="stat-label">Tổng sản phẩm</p>
              <span class="stat-change" id="productsChange">0</span>
            </div>
          </div>

          <!-- Total Users -->
          <div class="stat-card stat-users">
            <div class="stat-icon">
              <i class="fas fa-users"></i>
            </div>
            <div class="stat-details">
              <h3 class="stat-value" id="totalUsers">0</h3>
              <p class="stat-label">Tổng người dùng</p>
              <span class="stat-change positive" id="usersChange">+0%</span>
            </div>
          </div>
        </div>

        <!-- Charts & Recent Orders -->
        <div class="dashboard-grid">
          <!-- Order Status Distribution -->
          <div class="dashboard-card">
            <div class="card-header">
              <h3 class="card-title">
                <i class="fas fa-chart-pie"></i>
                Trạng thái đơn hàng
              </h3>
            </div>
            <div class="card-body">
              <div class="status-list" id="orderStatusList">
                <div class="status-item">
                  <span class="status-label">Chờ xác nhận</span>
                  <span class="status-count" id="pendingCount">0</span>
                </div>
                <div class="status-item">
                  <span class="status-label">Đang xử lý</span>
                  <span class="status-count" id="processingCount">0</span>
                </div>
                <div class="status-item">
                  <span class="status-label">Đang giao</span>
                  <span class="status-count" id="shippedCount">0</span>
                </div>
                <div class="status-item">
                  <span class="status-label">Đã giao</span>
                  <span class="status-count" id="deliveredCount">0</span>
                </div>
                <div class="status-item">
                  <span class="status-label">Đã hủy</span>
                  <span class="status-count" id="cancelledCount">0</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Orders Table -->
        <div class="dashboard-card">
          <div class="card-header">
            <h3 class="card-title">
              <i class="fas fa-clock"></i>
              Đơn hàng gần đây
            </h3>
            <a href="${pageContext.request.contextPath}/admin/orders" class="btn-view-all">
              Xem tất cả
              <i class="fas fa-arrow-right"></i>
            </a>
          </div>
          <div class="card-body">
            <div class="table-responsive">
              <table class="admin-table">
                <thead>
                  <tr>
                    <th>Mã đơn</th>
                    <th>Khách hàng</th>
                    <th>Tổng tiền</th>
                    <th>Trạng thái</th>
                    <th>Ngày đặt</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody id="recentOrdersTable">
                  <tr>
                    <td colspan="6" class="text-center">
                      <div class="loading-spinner">
                        <i class="fas fa-spinner fa-spin"></i>
                        <span>Đang tải...</span>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Low Stock Products -->
        <div class="dashboard-card">
          <div class="card-header">
            <h3 class="card-title">
              <i class="fas fa-exclamation-triangle"></i>
              Sản phẩm sắp hết hàng
            </h3>
            <a href="${pageContext.request.contextPath}/admin/products" class="btn-view-all">
              Xem tất cả
              <i class="fas fa-arrow-right"></i>
            </a>
          </div>
          <div class="card-body">
            <div class="table-responsive">
              <table class="admin-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th>Danh mục</th>
                    <th>Tồn kho</th>
                    <th>Trạng thái</th>
                  </tr>
                </thead>
                <tbody id="lowStockTable">
                  <tr>
                    <td colspan="4" class="text-center">
                      <div class="loading-spinner">
                        <i class="fas fa-spinner fa-spin"></i>
                        <span>Đang tải...</span>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>

  <%@ include file="/WEB-INF/views/common/admin-footer.jspf" %>
  
  <!-- Page-specific JavaScript -->
  <script src="${pageContext.request.contextPath}/static/js/pages/admin-dashboard.js"></script>

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

