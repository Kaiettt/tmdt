/**
 * Admin Orders Management JavaScript
 * Handles order listing, detail view, and status updates
 */

// contextPath is declared in admin-footer.jspf

// State
let currentPage = 1;
let currentFilters = {
  keyword: "",
  status: "",
  sortBy: "createdAt:desc",
};
let currentOrder = null;

// Initialize on page load
document.addEventListener("DOMContentLoaded", () => {
  // Check admin authentication
  checkAdminAuth();

  // Load orders
  loadOrders();

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
    loadOrders();
  }, 500));

  // Status filter
  document.getElementById("statusFilter").addEventListener("change", (e) => {
    currentFilters.status = e.target.value;
    currentPage = 1;
    loadOrders();
  });

  // Sort by
  document.getElementById("sortBy").addEventListener("change", (e) => {
    currentFilters.sortBy = e.target.value;
    currentPage = 1;
    loadOrders();
  });

  // Close modal buttons
  document.getElementById("btnCloseModal").addEventListener("click", closeOrderModal);

  // Close modal when clicking outside
  document.getElementById("orderDetailModal").addEventListener("click", (e) => {
    if (e.target.id === "orderDetailModal") {
      closeOrderModal();
    }
  });

  // Status action buttons
  document.getElementById("btnProcessOrder").addEventListener("click", () => updateOrderStatus("PROCESSING"));
  document.getElementById("btnShipOrder").addEventListener("click", () => updateOrderStatus("SHIPPED"));
  document.getElementById("btnDeliverOrder").addEventListener("click", () => updateOrderStatus("DELIVERED"));
  document.getElementById("btnCancelOrder").addEventListener("click", () => {
    if (confirm("Bạn có chắc chắn muốn hủy đơn hàng này?")) {
      updateOrderStatus("CANCELLED");
    }
  });
}

/**
 * Load orders with filters
 */
async function loadOrders() {
  try {
    showLoading();

    // Build query params
    const params = new URLSearchParams({
      page: currentPage,
      limit: 10,
      sortBy: currentFilters.sortBy,
    });

    if (currentFilters.keyword) params.append("keyword", currentFilters.keyword);
    if (currentFilters.status) params.append("status", currentFilters.status);

    const response = await fetch(
      contextPath + "/api/v1/admin/orders?" + params.toString(),
      {
        headers: {
          Authorization: "Bearer " + localStorage.getItem("accessToken"),
        },
      }
    );

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        renderOrders(result.data);
        if (result.metadata && result.metadata.pagination) {
          renderPagination(result.metadata.pagination);
        }
      }
    } else if (response.status === 401) {
      window.location.href = contextPath + "/login";
    } else {
      showError("Không thể tải danh sách đơn hàng");
    }
  } catch (error) {
    console.error("Error loading orders:", error);
    showError("Lỗi khi tải danh sách đơn hàng");
  }
}

/**
 * Render orders table
 */
function renderOrders(orders) {
  const tbody = document.getElementById("ordersTableBody");

  if (!orders || orders.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="text-center">
          <p style="padding: 2rem; color: #999;">Không tìm thấy đơn hàng nào</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = orders.map(order => `
    <tr>
      <td><strong>#${escapeHtml(order.id || order.orderCode)}</strong></td>
      <td>
        <div>
          <strong>${escapeHtml(order.fullName || order.customerName || "N/A")}</strong><br>
          <small style="color: var(--admin-text-light);">${escapeHtml(order.email || "")}</small>
        </div>
      </td>
      <td>
        <small>${escapeHtml(truncate(order.shippingAddress || order.address || "N/A", 50))}</small>
      </td>
      <td><strong>${formatPrice(order.totalAmount || 0)}</strong></td>
      <td>
        <span class="status-badge ${getStatusClass(order.status)}">
          ${getStatusText(order.status)}
        </span>
      </td>
      <td>${formatDate(order.createdAt)}</td>
      <td>
        <div class="btn-group">
          <button class="btn-action btn-primary btn-icon" onclick="viewOrderDetail(${order.id})" title="Xem chi tiết">
            <i class="fas fa-eye"></i>
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
  loadOrders();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

/**
 * View order detail
 */
async function viewOrderDetail(orderId) {
  try {
    const response = await fetch(`${contextPath}/api/v1/admin/orders/${orderId}`, {
      headers: {
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
    });

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        currentOrder = result.data;
        renderOrderDetail(result.data);
        openOrderModal();
      }
    } else {
      showError("Không thể tải thông tin đơn hàng");
    }
  } catch (error) {
    console.error("Error loading order detail:", error);
    showError("Lỗi khi tải thông tin đơn hàng");
  }
}

/**
 * Render order detail in modal
 */
function renderOrderDetail(order) {
  // Order code
  document.getElementById("modalOrderCode").textContent = `#${order.id || order.orderCode}`;

  // Customer info
  document.getElementById("customerName").textContent = order.fullName || order.customerName || "N/A";
  document.getElementById("customerEmail").textContent = order.email || "N/A";
  document.getElementById("customerPhone").textContent = order.phoneNumber || order.phone || "N/A";

  // Shipping address
  document.getElementById("shippingAddress").textContent = order.shippingAddress || order.address || "N/A";

  // Order items
  const itemsList = document.getElementById("orderItemsList");
  if (order.items && order.items.length > 0) {
    itemsList.innerHTML = order.items.map(item => `
      <div class="order-item">
        <img src="${escapeHtml(item.thumbnailUrl || item.productThumbnail || 'https://via.placeholder.com/80')}" 
             alt="${escapeHtml(item.productName)}" 
             class="order-item-image">
        <div class="order-item-info">
          <div class="order-item-name">${escapeHtml(item.productName)}</div>
          <div class="order-item-price">
            ${formatPrice(item.price)} x ${item.quantity}
          </div>
        </div>
        <div style="text-align: right;">
          <strong>${formatPrice(item.price * item.quantity)}</strong>
        </div>
      </div>
    `).join("");
  } else {
    itemsList.innerHTML = '<p style="color: #999;">Không có sản phẩm</p>';
  }

  // Order summary
  const subtotal = order.subtotal || order.totalAmount || 0;
  const shippingFee = order.shippingFee || 0;
  const discount = order.discount || order.discountAmount || 0;
  const totalAmount = order.totalAmount || 0;

  document.getElementById("subtotal").textContent = formatPrice(subtotal);
  document.getElementById("shippingFee").textContent = formatPrice(shippingFee);
  document.getElementById("discount").textContent = formatPrice(discount);
  document.getElementById("totalAmount").textContent = formatPrice(totalAmount);

  // Show/hide status action buttons based on current status
  updateStatusButtons(order.status);
}

/**
 * Update status buttons visibility
 */
function updateStatusButtons(status) {
  // Hide all buttons first
  document.getElementById("btnProcessOrder").style.display = "none";
  document.getElementById("btnShipOrder").style.display = "none";
  document.getElementById("btnDeliverOrder").style.display = "none";
  document.getElementById("btnCancelOrder").style.display = "none";

  // Show appropriate buttons based on status
  switch (status) {
    case "PENDING":
      document.getElementById("btnProcessOrder").style.display = "inline-flex";
      document.getElementById("btnCancelOrder").style.display = "inline-flex";
      break;
    case "PROCESSING":
      document.getElementById("btnShipOrder").style.display = "inline-flex";
      document.getElementById("btnCancelOrder").style.display = "inline-flex";
      break;
    case "SHIPPED":
      document.getElementById("btnDeliverOrder").style.display = "inline-flex";
      break;
    // DELIVERED and CANCELLED - no actions available
  }
}

/**
 * Update order status
 */
async function updateOrderStatus(newStatus) {
  if (!currentOrder) return;

  try {
    const response = await fetch(
      `${contextPath}/api/v1/admin/orders/${currentOrder.id}/status`,
      {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer " + localStorage.getItem("accessToken"),
        },
        body: JSON.stringify({ status: newStatus }),
      }
    );

    if (response.ok) {
      showSuccess("Cập nhật trạng thái đơn hàng thành công!");
      closeOrderModal();
      loadOrders();
    } else {
      const result = await response.json();
      showError(result.message || "Không thể cập nhật trạng thái");
    }
  } catch (error) {
    console.error("Error updating order status:", error);
    showError("Lỗi khi cập nhật trạng thái");
  }
}

/**
 * Open order modal
 */
function openOrderModal() {
  document.getElementById("orderDetailModal").classList.add("show");
}

/**
 * Close order modal
 */
function closeOrderModal() {
  document.getElementById("orderDetailModal").classList.remove("show");
  currentOrder = null;
}

/**
 * Utility functions
 */
function showLoading() {
  const tbody = document.getElementById("ordersTableBody");
  tbody.innerHTML = `
    <tr>
      <td colspan="7" class="text-center">
        <div class="loading-spinner">
          <i class="fas fa-spinner fa-spin"></i>
          <span>Đang tải...</span>
        </div>
      </td>
    </tr>
  `;
}

function getStatusClass(status) {
  const statusMap = {
    PENDING: "pending",
    PROCESSING: "processing",
    SHIPPED: "shipped",
    DELIVERED: "delivered",
    CANCELLED: "cancelled",
  };
  return statusMap[status] || "pending";
}

function getStatusText(status) {
  const statusMap = {
    PENDING: "Chờ xác nhận",
    PROCESSING: "Đang xử lý",
    SHIPPED: "Đang giao",
    DELIVERED: "Đã giao",
    CANCELLED: "Đã hủy",
  };
  return statusMap[status] || status;
}

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(price);
}

function formatDate(dateString) {
  if (!dateString) return "N/A";
  const date = new Date(dateString);
  return new Intl.DateTimeFormat("vi-VN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function escapeHtml(text) {
  if (!text) return "";
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

function truncate(text, length) {
  if (!text) return "";
  return text.length > length ? text.substring(0, length) + "..." : text;
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

