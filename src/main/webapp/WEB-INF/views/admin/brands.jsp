<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Quản lý Thương hiệu - UTE Admin</title>

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
          <h1 class="page-title">Quản lý Thương hiệu</h1>
        </div>
        <div class="topbar-right">
          <button class="btn-action btn-primary" id="btnAddBrand">
            <i class="fas fa-plus"></i> Thêm thương hiệu
          </button>
        </div>
      </div>

      <div class="admin-content">
        <div class="dashboard-card">
          <div class="card-body">
            <div class="table-responsive">
              <table class="admin-table">
                <thead>
                  <tr>
                    <th style="width: 80px;">ID</th>
                    <th>Tên thương hiệu</th>
                    <th>Mô tả</th>
                    <th>Số sản phẩm</th>
                    <th style="width: 150px;">Thao tác</th>
                  </tr>
                </thead>
                <tbody id="brandsTableBody">
                  <tr>
                    <td colspan="5" class="text-center">
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

  <!-- Brand Modal -->
  <div id="brandModal" class="modal">
    <div class="modal-dialog">
      <div class="modal-header">
        <h3 class="modal-title" id="modalTitle">Thêm thương hiệu</h3>
        <button class="btn-close" id="btnCloseModal">&times;</button>
      </div>
      <div class="modal-body">
        <form id="brandForm">
          <input type="hidden" id="brandId">
          <div class="form-group">
            <label for="brandName">Tên thương hiệu <span style="color: red;">*</span></label>
            <input type="text" id="brandName" class="form-control" required>
          </div>
          <div class="form-group">
            <label for="brandLogoUrl">Logo URL</label>
            <input type="url" id="brandLogoUrl" class="form-control" placeholder="https://...">
          </div>
          <div class="form-group">
            <label for="brandDescription">Mô tả</label>
            <textarea id="brandDescription" class="form-control" rows="3"></textarea>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button class="btn-action btn-secondary" id="btnCancelModal">Hủy</button>
        <button class="btn-action btn-primary" id="btnSaveBrand">Lưu</button>
      </div>
    </div>
  </div>

  <%@ include file="/WEB-INF/views/common/admin-footer.jspf" %>
  
  <!-- Page-specific JavaScript -->
  <script src="${pageContext.request.contextPath}/static/js/pages/admin-brands.js"></script>

  <script>
    document.getElementById('toggleSidebar')?.addEventListener('click', () => {
      document.querySelector('.admin-sidebar')?.classList.toggle('show');
    });
  </script>
</body>
</html>

