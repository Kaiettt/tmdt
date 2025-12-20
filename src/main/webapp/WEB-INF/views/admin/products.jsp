<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Quản lý Sản phẩm - UTE Admin</title>

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
    /* Additional styles for products page */
    .product-thumbnail {
      width: 60px;
      height: 60px;
      object-fit: cover;
      border-radius: 8px;
    }
    
    .product-info {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    
    .product-details h4 {
      font-size: 0.875rem;
      font-weight: 600;
      margin-bottom: 0.25rem;
    }
    
    .product-details p {
      font-size: 0.75rem;
      color: var(--admin-text-light);
    }
    
    .btn-group {
      display: flex;
      gap: 0.5rem;
    }
    
    .btn-icon {
      width: 36px;
      height: 36px;
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 6px;
    }
    
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
          <h1 class="page-title">Quản lý Sản phẩm</h1>
        </div>
        <div class="topbar-right">
          <button class="btn-action btn-primary" id="btnAddProduct">
            <i class="fas fa-plus"></i>
            Thêm sản phẩm
          </button>
        </div>
      </div>

      <!-- Products Content -->
      <div class="admin-content">
        <!-- Filters & Search -->
        <div class="dashboard-card" style="margin-bottom: 1.5rem;">
          <div class="card-body">
            <div style="display: flex; gap: 1rem; flex-wrap: wrap;">
              <div style="flex: 1; min-width: 250px;">
                <input type="text" id="searchInput" class="form-control" placeholder="Tìm kiếm sản phẩm...">
              </div>
              <div style="min-width: 200px;">
                <select id="categoryFilter" class="form-control">
                  <option value="">Tất cả danh mục</option>
                </select>
              </div>
              <div style="min-width: 200px;">
                <select id="brandFilter" class="form-control">
                  <option value="">Tất cả thương hiệu</option>
                </select>
              </div>
              <div style="min-width: 150px;">
                <select id="statusFilter" class="form-control">
                  <option value="">Tất cả trạng thái</option>
                  <option value="true">Đang bán</option>
                  <option value="false">Ngừng bán</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <!-- Products Table -->
        <div class="dashboard-card">
          <div class="card-body">
            <div class="table-responsive">
              <table class="admin-table">
                <thead>
                  <tr>
                    <th style="width: 50px;">ID</th>
                    <th>Sản phẩm</th>
                    <th>Danh mục</th>
                    <th>Thương hiệu</th>
                    <th>Giá</th>
                    <th>Tồn kho</th>
                    <th>Trạng thái</th>
                    <th style="width: 150px;">Thao tác</th>
                  </tr>
                </thead>
                <tbody id="productsTableBody">
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
              <!-- Pagination will be rendered here -->
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>

  <!-- Add/Edit Product Modal -->
  <div id="productModal" class="modal">
    <div class="modal-dialog">
      <div class="modal-header">
        <h3 class="modal-title" id="modalTitle">Thêm sản phẩm mới</h3>
        <button class="btn-close" id="btnCloseModal">&times;</button>
      </div>
      <form id="productForm">
        <div class="modal-body">
          <input type="hidden" id="productId">
          
          <div class="form-group">
            <label class="form-label">Tên sản phẩm *</label>
            <input type="text" id="productName" class="form-control" required>
          </div>

          <div class="form-group">
            <label class="form-label">Mô tả</label>
            <textarea id="productDescription" class="form-control"></textarea>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
            <div class="form-group">
              <label class="form-label">Danh mục *</label>
              <select id="productCategory" class="form-control" required>
                <option value="">-- Chọn danh mục --</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">Thương hiệu *</label>
              <select id="productBrand" class="form-control" required>
                <option value="">-- Chọn thương hiệu --</option>
              </select>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
            <div class="form-group">
              <label class="form-label">Giá *</label>
              <input type="number" id="productPrice" class="form-control" min="0" required>
            </div>

            <div class="form-group">
              <label class="form-label">Số lượng *</label>
              <input type="number" id="productStock" class="form-control" min="0" required>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">URL Hình ảnh</label>
            <input type="url" id="productThumbnail" class="form-control" placeholder="https://...">
          </div>

          <div class="form-group">
            <label class="form-label">Thông số kỹ thuật (JSON)</label>
            <textarea id="productSpecifications" class="form-control" placeholder='{"ram": "8GB", "storage": "256GB"}'></textarea>
            <small style="color: var(--admin-text-light);">Nhập dạng JSON hợp lệ</small>
          </div>

          <div class="form-group">
            <label style="display: flex; align-items: center; gap: 0.5rem; cursor: pointer;">
              <input type="checkbox" id="productStatus" checked>
              <span>Đang bán</span>
            </label>
          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn-action btn-secondary" id="btnCancelModal">Hủy</button>
          <button type="submit" class="btn-action btn-primary">
            <i class="fas fa-save"></i>
            Lưu
          </button>
        </div>
      </form>
    </div>
  </div>

  <%@ include file="/WEB-INF/views/common/admin-footer.jspf" %>
  
  <!-- Page-specific JavaScript -->
  <script src="${pageContext.request.contextPath}/static/js/pages/admin-products.js"></script>

  <script>
    // Toggle sidebar on mobile
    document.getElementById('toggleSidebar')?.addEventListener('click', () => {
      document.querySelector('.admin-sidebar')?.classList.toggle('show');
    });
  </script>
</body>
</html>

