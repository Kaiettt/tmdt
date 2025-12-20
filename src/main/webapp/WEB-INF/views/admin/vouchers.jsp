<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Quản lý Voucher - UTE Admin</title>

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
    .voucher-code {
      font-family: 'Courier New', monospace;
      font-weight: 700;
      color: var(--admin-primary);
      background: rgba(67, 97, 238, 0.1);
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
    }
    
    .discount-badge {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 0.375rem 0.75rem;
      border-radius: 6px;
      font-weight: 600;
      font-size: 0.875rem;
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
  <div class="admin-layout">
    <%@ include file="/WEB-INF/views/common/admin-sidebar.jspf" %>

    <main class="admin-main">
      <div class="admin-topbar">
        <div class="topbar-left">
          <button class="btn-toggle-sidebar" id="toggleSidebar">
            <i class="fas fa-bars"></i>
          </button>
          <h1 class="page-title">Quản lý Voucher</h1>
        </div>
        <div class="topbar-right">
          <button class="btn-action btn-primary" id="btnAddVoucher">
            <i class="fas fa-plus"></i> Thêm voucher
          </button>
        </div>
      </div>

      <div class="admin-content">
        <!-- Filters -->
        <div class="dashboard-card" style="margin-bottom: 1.5rem;">
          <div class="card-body">
            <div style="display: flex; gap: 1rem; flex-wrap: wrap;">
              <div style="flex: 1; min-width: 250px;">
                <input type="text" id="searchInput" class="form-control" placeholder="Tìm theo mã voucher...">
              </div>
              <div style="min-width: 180px;">
                <select id="typeFilter" class="form-control">
                  <option value="">Tất cả loại</option>
                  <option value="PERCENTAGE">Giảm %</option>
                  <option value="FIXED_AMOUNT">Giảm cố định</option>
                </select>
              </div>
              <div style="min-width: 180px;">
                <select id="statusFilter" class="form-control">
                  <option value="">Tất cả trạng thái</option>
                  <option value="active">Đang hoạt động</option>
                  <option value="inactive">Không hoạt động</option>
                  <option value="expired">Hết hạn</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <!-- Vouchers Table -->
        <div class="dashboard-card">
          <div class="card-body">
            <div class="table-responsive">
              <table class="admin-table">
                <thead>
                  <tr>
                    <th style="width: 80px;">ID</th>
                    <th>Mã voucher</th>
                    <th>Giảm giá</th>
                    <th>Giá trị tối thiểu</th>
                    <th>Số lượng</th>
                    <th>Hạn sử dụng</th>
                    <th>Trạng thái</th>
                    <th style="width: 150px;">Thao tác</th>
                  </tr>
                </thead>
                <tbody id="vouchersTableBody">
                  <tr>
                    <td colspan="8" class="text-center">
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
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>

  <!-- Voucher Modal -->
  <div id="voucherModal" class="modal">
    <div class="modal-dialog" style="max-width: 600px;">
      <div class="modal-header">
        <h3 class="modal-title" id="modalTitle">Thêm voucher</h3>
        <button class="btn-close" id="btnCloseModal">&times;</button>
      </div>
      <div class="modal-body">
        <form id="voucherForm">
          <input type="hidden" id="voucherId">
          
          <div class="form-group">
            <label for="voucherCode">Mã voucher <span style="color: red;">*</span></label>
            <input type="text" id="voucherCode" class="form-control" required placeholder="VD: SUMMER2025">
          </div>
          
          <div class="form-group">
            <label for="voucherDescription">Mô tả</label>
            <textarea id="voucherDescription" class="form-control" rows="2" placeholder="Mô tả voucher..."></textarea>
          </div>
          
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
            <div class="form-group">
              <label for="discountType">Loại giảm giá <span style="color: red;">*</span></label>
              <select id="discountType" class="form-control" required>
                <option value="PERCENTAGE">Giảm theo %</option>
                <option value="FIXED_AMOUNT">Giảm cố định</option>
              </select>
            </div>
            
            <div class="form-group">
              <label for="discountValue">Giá trị giảm <span style="color: red;">*</span></label>
              <input type="number" id="discountValue" class="form-control" required min="0" step="0.01">
            </div>
          </div>
          
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
            <div class="form-group">
              <label for="minOrderValue">Giá trị đơn tối thiểu</label>
              <input type="number" id="minOrderValue" class="form-control" min="0" step="1000" placeholder="0">
            </div>
            
            <div class="form-group">
              <label for="maxDiscountAmount">Giảm tối đa</label>
              <input type="number" id="maxDiscountAmount" class="form-control" min="0" step="1000" placeholder="Không giới hạn">
            </div>
          </div>
          
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
            <div class="form-group">
              <label for="usageLimit">Số lượng</label>
              <input type="number" id="usageLimit" class="form-control" min="1" placeholder="Không giới hạn">
            </div>
            
            <div class="form-group">
              <label for="usageLimitPerUser">Giới hạn/người</label>
              <input type="number" id="usageLimitPerUser" class="form-control" min="1" value="1">
            </div>
          </div>
          
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
            <div class="form-group">
              <label for="startDate">Ngày bắt đầu <span style="color: red;">*</span></label>
              <input type="datetime-local" id="startDate" class="form-control" required>
            </div>
            
            <div class="form-group">
              <label for="endDate">Ngày kết thúc <span style="color: red;">*</span></label>
              <input type="datetime-local" id="endDate" class="form-control" required>
            </div>
          </div>
          
          <div class="form-group">
            <label>
              <input type="checkbox" id="isActive" checked>
              Kích hoạt ngay
            </label>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button class="btn-action btn-secondary" id="btnCancelModal">Hủy</button>
        <button class="btn-action btn-primary" id="btnSaveVoucher">Lưu</button>
      </div>
    </div>
  </div>

  <%@ include file="/WEB-INF/views/common/admin-footer.jspf" %>
  
  <!-- Page-specific JavaScript -->
  <script src="${pageContext.request.contextPath}/static/js/pages/admin-vouchers.js"></script>

  <script>
    document.getElementById('toggleSidebar')?.addEventListener('click', () => {
      document.querySelector('.admin-sidebar')?.classList.toggle('show');
    });
  </script>
</body>
</html>

