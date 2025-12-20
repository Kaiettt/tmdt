<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Quản lý Người dùng - UTE Admin</title>

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
    /* Additional styles for users page */
    .user-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: 600;
      font-size: 1rem;
    }
    
    .user-info-cell {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    
    .user-details h4 {
      font-size: 0.875rem;
      font-weight: 600;
      margin-bottom: 0.25rem;
    }
    
    .user-details p {
      font-size: 0.75rem;
      color: var(--admin-text-light);
      margin: 0;
    }
    
    .role-badge {
      padding: 0.375rem 0.75rem;
      border-radius: 6px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
    }
    
    .role-badge.admin {
      background: rgba(156, 39, 176, 0.1);
      color: #9c27b0;
    }
    
    .role-badge.customer {
      background: rgba(33, 150, 243, 0.1);
      color: #2196f3;
    }
    
    .toggle-switch {
      position: relative;
      display: inline-block;
      width: 50px;
      height: 24px;
    }
    
    .toggle-switch input {
      opacity: 0;
      width: 0;
      height: 0;
    }
    
    .toggle-slider {
      position: absolute;
      cursor: pointer;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: #ccc;
      transition: 0.3s;
      border-radius: 24px;
    }
    
    .toggle-slider:before {
      position: absolute;
      content: "";
      height: 18px;
      width: 18px;
      left: 3px;
      bottom: 3px;
      background-color: white;
      transition: 0.3s;
      border-radius: 50%;
    }
    
    input:checked + .toggle-slider {
      background-color: var(--admin-success);
    }
    
    input:checked + .toggle-slider:before {
      transform: translateX(26px);
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
          <h1 class="page-title">Quản lý Người dùng</h1>
        </div>
        <div class="topbar-right">
          <div class="topbar-date">
            <i class="far fa-calendar"></i>
            <span id="currentDate"></span>
          </div>
        </div>
      </div>

      <!-- Users Content -->
      <div class="admin-content">
        <!-- Statistics Cards -->
        <div class="stats-grid" style="margin-bottom: 2rem; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
              <i class="fas fa-users"></i>
            </div>
            <div class="stat-details">
              <h3 class="stat-value" id="totalUsers">0</h3>
              <p class="stat-label">Tổng người dùng</p>
            </div>
          </div>
          
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
              <i class="fas fa-user-check"></i>
            </div>
            <div class="stat-details">
              <h3 class="stat-value" id="activeUsers">0</h3>
              <p class="stat-label">Đang hoạt động</p>
            </div>
          </div>
          
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
              <i class="fas fa-user-plus"></i>
            </div>
            <div class="stat-details">
              <h3 class="stat-value" id="newUsers">0</h3>
              <p class="stat-label">Mới tháng này</p>
            </div>
          </div>
        </div>

        <!-- Filters & Search -->
        <div class="dashboard-card" style="margin-bottom: 1.5rem;">
          <div class="card-body">
            <div style="display: flex; gap: 1rem; flex-wrap: wrap; align-items: center;">
              <div style="flex: 1; min-width: 250px;">
                <input type="text" id="searchInput" class="form-control" placeholder="Tìm theo tên, email, username...">
              </div>
              <div style="min-width: 180px;">
                <select id="roleFilter" class="form-control">
                  <option value="">Tất cả vai trò</option>
                  <option value="customer">Khách hàng</option>
                  <option value="admin">Quản trị viên</option>
                </select>
              </div>
              <div style="min-width: 180px;">
                <select id="statusFilter" class="form-control">
                  <option value="">Tất cả trạng thái</option>
                  <option value="active">Đang hoạt động</option>
                  <option value="locked">Đã khóa</option>
                </select>
              </div>
              <button id="btnAddUser" class="btn-add" style="white-space: nowrap;">
                <i class="fas fa-plus"></i> Thêm người dùng
              </button>
            </div>
          </div>
        </div>

        <!-- Users Table -->
        <div class="dashboard-card">
          <div class="card-body">
            <div class="table-responsive">
              <table class="admin-table">
                <thead>
                  <tr>
                    <th style="width: 50px;">ID</th>
                    <th>Người dùng</th>
                    <th>Số điện thoại</th>
                    <th>Vai trò</th>
                    <th>Trạng thái</th>
                    <th>Ngày tạo</th>
                    <th style="width: 100px;">Thao tác</th>
                  </tr>
                </thead>
                <tbody id="usersTableBody">
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

  <!-- User Modal -->
  <div id="userModal" class="modal">
    <div class="modal-dialog">
      <div class="modal-header">
        <h3 class="modal-title" id="modalTitle">Thêm người dùng</h3>
        <button class="btn-close" id="btnCloseModal">&times;</button>
      </div>
      <div class="modal-body">
        <form id="userForm">
          <input type="hidden" id="userId">
          
          <div class="form-group">
            <label for="userEmail">Email <span style="color: red;">*</span></label>
            <input type="email" id="userEmail" class="form-control" required>
          </div>
          
          <div class="form-group">
            <label for="userUsername">Username</label>
            <input type="text" id="userUsername" class="form-control" placeholder="Tự động tạo từ email nếu để trống">
            <small style="color: #666; font-size: 0.875rem;">Để trống để tự động tạo từ email</small>
          </div>
          
          <div class="form-group">
            <label for="userFullName">Họ và tên <span style="color: red;">*</span></label>
            <input type="text" id="userFullName" class="form-control" required>
          </div>
          
          <div class="form-group">
            <label for="userPassword">Mật khẩu <span style="color: red;">*</span></label>
            <input type="password" id="userPassword" class="form-control" required>
            <small style="color: #666; font-size: 0.875rem;">Tối thiểu 6 ký tự</small>
          </div>
          
          <div class="form-group">
            <label for="userPhoneNumber">Số điện thoại</label>
            <input type="tel" id="userPhoneNumber" class="form-control" placeholder="0123456789">
          </div>
          
          <div class="form-group">
            <label for="userRole">Vai trò <span style="color: red;">*</span></label>
            <select id="userRole" class="form-control" required>
              <option value="customer">Khách hàng</option>
              <option value="admin">Quản trị viên</option>
            </select>
          </div>
          
          <div class="form-group">
            <label for="userStatus">Trạng thái <span style="color: red;">*</span></label>
            <select id="userStatus" class="form-control" required>
              <option value="active">Đang hoạt động</option>
              <option value="locked">Đã khóa</option>
              <option value="pending">Chờ xác thực</option>
            </select>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button class="btn-action btn-secondary" id="btnCancelModal">Hủy</button>
        <button class="btn-action btn-primary" id="btnSaveUser">Lưu</button>
      </div>
    </div>
  </div>

  <%@ include file="/WEB-INF/views/common/admin-footer.jspf" %>
  
  <!-- Page-specific JavaScript -->
  <script src="${pageContext.request.contextPath}/static/js/pages/admin-users.js"></script>

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

