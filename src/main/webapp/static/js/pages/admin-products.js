/**
 * Admin Products Management JavaScript
 * Handles CRUD operations for products
 */

// contextPath is declared in admin-footer.jspf

// State
let currentPage = 1;
let currentFilters = {
  keyword: "",
  categoryId: "",
  brandId: "",
  status: "",
};
let categories = [];
let brands = [];

// Initialize on page load
document.addEventListener("DOMContentLoaded", () => {
  // Check admin authentication
  checkAdminAuth();

  // Load initial data
  loadCategories();
  loadBrands();
  loadProducts();

  // Setup event listeners
  setupEventListeners();
});

/**
 * Check if user is authenticated and is admin
 */
function checkAdminAuth() {
  const token = localStorage.getItem("accessToken");
  const user = JSON.parse(localStorage.getItem("user") || "{}");

  // Check if user is admin (case-insensitive)
  if (!token || !user.role || user.role.toUpperCase() !== "ADMIN") {
    alert("Bạn không có quyền truy cập trang này!");
    window.location.href = contextPath + "/login";
    return;
  }
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
  // Search input
  const searchInput = document.getElementById("searchInput");
  searchInput.addEventListener("input", debounce(() => {
    currentFilters.keyword = searchInput.value;
    currentPage = 1;
    loadProducts();
  }, 500));

  // Category filter
  document.getElementById("categoryFilter").addEventListener("change", (e) => {
    currentFilters.categoryId = e.target.value;
    currentPage = 1;
    loadProducts();
  });

  // Brand filter
  document.getElementById("brandFilter").addEventListener("change", (e) => {
    currentFilters.brandId = e.target.value;
    currentPage = 1;
    loadProducts();
  });

  // Status filter
  document.getElementById("statusFilter").addEventListener("change", (e) => {
    currentFilters.status = e.target.value;
    currentPage = 1;
    loadProducts();
  });

  // Add product button
  document.getElementById("btnAddProduct").addEventListener("click", () => {
    openProductModal();
  });

  // Close modal buttons
  document.getElementById("btnCloseModal").addEventListener("click", closeProductModal);
  document.getElementById("btnCancelModal").addEventListener("click", closeProductModal);

  // Form submit
  document.getElementById("productForm").addEventListener("submit", handleFormSubmit);

  // Close modal when clicking outside
  document.getElementById("productModal").addEventListener("click", (e) => {
    if (e.target.id === "productModal") {
      closeProductModal();
    }
  });
}

/**
 * Load categories
 */
async function loadCategories() {
  try {
    const response = await fetch(contextPath + "/api/v1/categories");
    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        categories = result.data;
        renderCategoryFilters();
        renderCategoryOptions();
      }
    }
  } catch (error) {
    console.error("Error loading categories:", error);
  }
}

/**
 * Load brands
 */
async function loadBrands() {
  try {
    const response = await fetch(contextPath + "/api/v1/brands");
    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        brands = result.data;
        renderBrandFilters();
        renderBrandOptions();
      }
    }
  } catch (error) {
    console.error("Error loading brands:", error);
  }
}

/**
 * Render category filters
 */
function renderCategoryFilters() {
  const select = document.getElementById("categoryFilter");
  select.innerHTML = '<option value="">Tất cả danh mục</option>' +
    categories.map(cat => `<option value="${cat.id}">${escapeHtml(cat.name)}</option>`).join("");
}

/**
 * Render brand filters
 */
function renderBrandFilters() {
  const select = document.getElementById("brandFilter");
  select.innerHTML = '<option value="">Tất cả thương hiệu</option>' +
    brands.map(brand => `<option value="${brand.id}">${escapeHtml(brand.name)}</option>`).join("");
}

/**
 * Render category options in modal
 */
function renderCategoryOptions() {
  const select = document.getElementById("productCategory");
  select.innerHTML = '<option value="">-- Chọn danh mục --</option>' +
    categories.map(cat => `<option value="${cat.id}">${escapeHtml(cat.name)}</option>`).join("");
}

/**
 * Render brand options in modal
 */
function renderBrandOptions() {
  const select = document.getElementById("productBrand");
  select.innerHTML = '<option value="">-- Chọn thương hiệu --</option>' +
    brands.map(brand => `<option value="${brand.id}">${escapeHtml(brand.name)}</option>`).join("");
}

/**
 * Load products with filters
 */
async function loadProducts() {
  try {
    showLoading();

    // Build query params
    const params = new URLSearchParams({
      page: currentPage,
      limit: 10,
    });

    if (currentFilters.keyword) params.append("keyword", currentFilters.keyword);
    if (currentFilters.categoryId) params.append("categoryId", currentFilters.categoryId);
    if (currentFilters.brandId) params.append("brandId", currentFilters.brandId);
    if (currentFilters.status) params.append("status", currentFilters.status);

    const response = await fetch(
      contextPath + "/api/v1/admin/products?" + params.toString(),
      {
        headers: {
          Authorization: "Bearer " + localStorage.getItem("accessToken"),
        },
      }
    );

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        renderProducts(result.data);
        if (result.metadata && result.metadata.pagination) {
          renderPagination(result.metadata.pagination);
        }
      }
    } else if (response.status === 401) {
      window.location.href = contextPath + "/login";
    } else {
      showError("Không thể tải danh sách sản phẩm");
    }
  } catch (error) {
    console.error("Error loading products:", error);
    showError("Lỗi khi tải danh sách sản phẩm");
  }
}

/**
 * Render products table
 */
function renderProducts(products) {
  const tbody = document.getElementById("productsTableBody");

  if (!products || products.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" class="text-center">
          <p style="padding: 2rem; color: #999;">Không tìm thấy sản phẩm nào</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = products.map(product => `
    <tr>
      <td><strong>#${product.id}</strong></td>
      <td>
        <div class="product-info">
          <img src="${escapeHtml(product.thumbnailUrl || 'https://via.placeholder.com/60')}" 
               alt="${escapeHtml(product.name)}" 
               class="product-thumbnail">
          <div class="product-details">
            <h4>${escapeHtml(product.name)}</h4>
            <p>${escapeHtml(product.description || '').substring(0, 50)}${product.description && product.description.length > 50 ? '...' : ''}</p>
          </div>
        </div>
      </td>
      <td>${escapeHtml(product.categoryName || 'N/A')}</td>
      <td>${escapeHtml(product.brandName || 'N/A')}</td>
      <td><strong>${formatPrice(product.price)}</strong></td>
      <td>
        <span class="stock-status ${getStockClass(product.stockQuantity)}">
          ${product.stockQuantity}
        </span>
      </td>
      <td>
        <span class="status-badge ${product.status ? 'delivered' : 'cancelled'}">
          ${product.status ? 'Đang bán' : 'Ngừng bán'}
        </span>
      </td>
      <td>
        <div class="btn-group">
          <button class="btn-action btn-primary btn-icon" onclick="editProduct(${product.id})" title="Sửa">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn-action btn-danger btn-icon" onclick="deleteProduct(${product.id})" title="Xóa">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      </td>
    </tr>
  `).join("");
}

/**
 * Render pagination
 */
function renderPagination(pagination) {
  const container = document.getElementById("pagination");
  const { page, totalPages } = pagination;

  if (totalPages <= 1) {
    container.innerHTML = "";
    return;
  }

  let html = "";

  // Previous button
  html += `<button class="btn-action btn-secondary" ${page === 1 ? 'disabled' : ''} onclick="changePage(${page - 1})">
    <i class="fas fa-chevron-left"></i>
  </button>`;

  // Page numbers
  for (let i = 1; i <= totalPages; i++) {
    if (i === 1 || i === totalPages || (i >= page - 1 && i <= page + 1)) {
      html += `<button class="btn-action ${i === page ? 'btn-primary' : 'btn-secondary'}" onclick="changePage(${i})">
        ${i}
      </button>`;
    } else if (i === page - 2 || i === page + 2) {
      html += `<span style="padding: 0.5rem;">...</span>`;
    }
  }

  // Next button
  html += `<button class="btn-action btn-secondary" ${page === totalPages ? 'disabled' : ''} onclick="changePage(${page + 1})">
    <i class="fas fa-chevron-right"></i>
  </button>`;

  container.innerHTML = html;
}

/**
 * Change page
 */
function changePage(page) {
  currentPage = page;
  loadProducts();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

/**
 * Open product modal for add/edit
 */
function openProductModal(product = null) {
  const modal = document.getElementById("productModal");
  const form = document.getElementById("productForm");
  
  form.reset();
  
  if (product) {
    // Edit mode
    document.getElementById("modalTitle").textContent = "Chỉnh sửa sản phẩm";
    document.getElementById("productId").value = product.id;
    document.getElementById("productName").value = product.name;
    document.getElementById("productDescription").value = product.description || "";
    document.getElementById("productCategory").value = product.categoryId;
    document.getElementById("productBrand").value = product.brandId;
    document.getElementById("productPrice").value = product.price;
    document.getElementById("productStock").value = product.stockQuantity;
    document.getElementById("productThumbnail").value = product.thumbnailUrl || "";
    document.getElementById("productSpecifications").value = product.specifications || "";
    document.getElementById("productStatus").checked = product.status;
  } else {
    // Add mode
    document.getElementById("modalTitle").textContent = "Thêm sản phẩm mới";
    document.getElementById("productId").value = "";
  }
  
  modal.classList.add("show");
}

/**
 * Close product modal
 */
function closeProductModal() {
  document.getElementById("productModal").classList.remove("show");
}

/**
 * Handle form submit
 */
async function handleFormSubmit(e) {
  e.preventDefault();

  const productId = document.getElementById("productId").value;
  const data = {
    name: document.getElementById("productName").value,
    description: document.getElementById("productDescription").value,
    categoryId: parseInt(document.getElementById("productCategory").value),
    brandId: parseInt(document.getElementById("productBrand").value),
    price: parseFloat(document.getElementById("productPrice").value),
    stockQuantity: parseInt(document.getElementById("productStock").value),
    thumbnailUrl: document.getElementById("productThumbnail").value,
    specifications: document.getElementById("productSpecifications").value,
    status: document.getElementById("productStatus").checked,
  };

  try {
    const url = productId
      ? `${contextPath}/api/v1/admin/products/${productId}`
      : `${contextPath}/api/v1/admin/products`;
    
    const method = productId ? "PUT" : "POST";

    const response = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
      body: JSON.stringify(data),
    });

    if (response.ok) {
      showSuccess(productId ? "Cập nhật sản phẩm thành công!" : "Thêm sản phẩm thành công!");
      closeProductModal();
      loadProducts();
    } else {
      const result = await response.json();
      showError(result.message || "Có lỗi xảy ra");
    }
  } catch (error) {
    console.error("Error saving product:", error);
    showError("Không thể lưu sản phẩm");
  }
}

/**
 * Edit product
 */
async function editProduct(id) {
  try {
    const response = await fetch(`${contextPath}/api/v1/products/${id}`);
    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        openProductModal(result.data);
      }
    }
  } catch (error) {
    console.error("Error loading product:", error);
    showError("Không thể tải thông tin sản phẩm");
  }
}

/**
 * Delete product
 */
async function deleteProduct(id) {
  if (!confirm("Bạn có chắc chắn muốn xóa sản phẩm này?")) {
    return;
  }

  try {
    const response = await fetch(`${contextPath}/api/v1/admin/products/${id}`, {
      method: "DELETE",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
    });

    if (response.ok) {
      showSuccess("Xóa sản phẩm thành công!");
      loadProducts();
    } else {
      showError("Không thể xóa sản phẩm");
    }
  } catch (error) {
    console.error("Error deleting product:", error);
    showError("Lỗi khi xóa sản phẩm");
  }
}

/**
 * Utility functions
 */
function showLoading() {
  const tbody = document.getElementById("productsTableBody");
  tbody.innerHTML = `
    <tr>
      <td colspan="8" class="text-center">
        <div class="loading-spinner">
          <i class="fas fa-spinner fa-spin"></i>
          <span>Đang tải...</span>
        </div>
      </td>
    </tr>
  `;
}

function getStockClass(quantity) {
  if (quantity === 0) return "low";
  if (quantity <= 10) return "low";
  if (quantity <= 50) return "medium";
  return "high";
}

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(price);
}

function escapeHtml(text) {
  if (!text) return "";
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

function showSuccess(message) {
  alert(message); // Có thể thay bằng toast notification
}

function showError(message) {
  alert(message); // Có thể thay bằng toast notification
}

