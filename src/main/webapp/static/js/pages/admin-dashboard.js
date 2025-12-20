/**
 * Admin Dashboard JavaScript
 * Handles dashboard statistics, charts, and recent data
 */

// contextPath is declared in admin-footer.jspf

// Initialize dashboard on page load
document.addEventListener("DOMContentLoaded", () => {
  // Check admin authentication
  checkAdminAuth();

  // Load dashboard data
  loadDashboardStats();
  loadRecentOrders();
  loadLowStockProducts();
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
 * Load dashboard statistics
 */
async function loadDashboardStats() {
  try {
    const response = await fetch(contextPath + "/api/v1/admin/dashboard/stats", {
      headers: {
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
    });

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        renderDashboardStats(result.data);
      }
    } else if (response.status === 401) {
      // Token expired
      window.location.href = contextPath + "/login";
    } else {
      console.error("Failed to load dashboard stats");
      // Show placeholder data
      renderPlaceholderStats();
    }
  } catch (error) {
    console.error("Error loading dashboard stats:", error);
    renderPlaceholderStats();
  }
}

/**
 * Render dashboard statistics
 */
function renderDashboardStats(stats) {
  // Total Revenue
  document.getElementById("totalRevenue").textContent = formatPrice(
    stats.totalRevenue || 0
  );
  document.getElementById("revenueChange").textContent =
    (stats.revenueChange > 0 ? "+" : "") + (stats.revenueChange || 0) + "%";

  // Total Orders
  document.getElementById("totalOrders").textContent = stats.totalOrders || 0;
  document.getElementById("ordersChange").textContent =
    (stats.ordersChange > 0 ? "+" : "") + (stats.ordersChange || 0) + "%";

  // Total Products
  document.getElementById("totalProducts").textContent = stats.totalProducts || 0;
  document.getElementById("productsChange").textContent =
    stats.productsChange || "0 mới";

  // Total Users
  document.getElementById("totalUsers").textContent = stats.totalUsers || 0;
  document.getElementById("usersChange").textContent =
    (stats.usersChange > 0 ? "+" : "") + (stats.usersChange || 0) + "%";

  // Order Status Distribution
  if (stats.ordersByStatus) {
    document.getElementById("pendingCount").textContent =
      stats.ordersByStatus.PENDING || 0;
    document.getElementById("processingCount").textContent =
      stats.ordersByStatus.PROCESSING || 0;
    document.getElementById("shippedCount").textContent =
      stats.ordersByStatus.SHIPPED || 0;
    document.getElementById("deliveredCount").textContent =
      stats.ordersByStatus.DELIVERED || 0;
    document.getElementById("cancelledCount").textContent =
      stats.ordersByStatus.CANCELLED || 0;
  }
}

/**
 * Render placeholder stats (when API not available)
 */
function renderPlaceholderStats() {
  // Placeholder data for MVP
  const placeholderStats = {
    totalRevenue: 125000000,
    revenueChange: 12.5,
    totalOrders: 156,
    ordersChange: 8.3,
    totalProducts: 48,
    productsChange: "5 mới",
    totalUsers: 234,
    usersChange: 15.2,
    ordersByStatus: {
      PENDING: 12,
      PROCESSING: 25,
      SHIPPED: 18,
      DELIVERED: 89,
      CANCELLED: 12,
    },
  };

  renderDashboardStats(placeholderStats);
}

/**
 * Load recent orders
 */
async function loadRecentOrders() {
  try {
    const response = await fetch(
      contextPath + "/api/v1/admin/dashboard/recent-orders?limit=10",
      {
        headers: {
          Authorization: "Bearer " + localStorage.getItem("accessToken"),
        },
      }
    );

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        renderRecentOrders(result.data);
      }
    } else {
      renderEmptyOrders();
    }
  } catch (error) {
    console.error("Error loading recent orders:", error);
    renderEmptyOrders();
  }
}

/**
 * Render recent orders table
 */
function renderRecentOrders(orders) {
  const tbody = document.getElementById("recentOrdersTable");

  if (!orders || orders.length === 0) {
    renderEmptyOrders();
    return;
  }

  tbody.innerHTML = orders
    .map(
      (order) => `
    <tr>
      <td><strong>#${escapeHtml(order.id || order.orderCode)}</strong></td>
      <td>${escapeHtml(order.customerName || order.fullName || "N/A")}</td>
      <td><strong>${formatPrice(order.totalAmount || 0)}</strong></td>
      <td>
        <span class="status-badge ${getStatusClass(order.status)}">
          ${getStatusText(order.status)}
        </span>
      </td>
      <td>${formatDate(order.createdAt)}</td>
      <td>
        <a href="${contextPath}/admin/orders?id=${order.id}" class="btn-action btn-primary btn-sm">
          <i class="fas fa-eye"></i>
          Xem
        </a>
      </td>
    </tr>
  `
    )
    .join("");
}

/**
 * Render empty orders message
 */
function renderEmptyOrders() {
  const tbody = document.getElementById("recentOrdersTable");
  tbody.innerHTML = `
    <tr>
      <td colspan="6" class="text-center">
        <p style="padding: 2rem; color: #999;">Chưa có đơn hàng nào</p>
      </td>
    </tr>
  `;
}

/**
 * Load low stock products
 */
async function loadLowStockProducts() {
  try {
    const response = await fetch(
      contextPath + "/api/v1/admin/dashboard/low-stock?threshold=10&limit=10",
      {
        headers: {
          Authorization: "Bearer " + localStorage.getItem("accessToken"),
        },
      }
    );

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        renderLowStockProducts(result.data);
      }
    } else {
      renderEmptyLowStock();
    }
  } catch (error) {
    console.error("Error loading low stock products:", error);
    renderEmptyLowStock();
  }
}

/**
 * Render low stock products table
 */
function renderLowStockProducts(products) {
  const tbody = document.getElementById("lowStockTable");

  if (!products || products.length === 0) {
    renderEmptyLowStock();
    return;
  }

  tbody.innerHTML = products
    .map(
      (product) => `
    <tr>
      <td>
        <strong>${escapeHtml(product.name)}</strong>
      </td>
      <td>${escapeHtml(product.categoryName || "N/A")}</td>
      <td>
        <span class="stock-status ${getStockClass(product.stockQuantity)}">
          ${product.stockQuantity} sản phẩm
        </span>
      </td>
      <td>
        ${
          product.stockQuantity === 0
            ? '<span class="status-badge cancelled">Hết hàng</span>'
            : '<span class="status-badge pending">Sắp hết</span>'
        }
      </td>
    </tr>
  `
    )
    .join("");
}

/**
 * Render empty low stock message
 */
function renderEmptyLowStock() {
  const tbody = document.getElementById("lowStockTable");
  tbody.innerHTML = `
    <tr>
      <td colspan="4" class="text-center">
        <p style="padding: 2rem; color: #999;">Tất cả sản phẩm đều còn đủ hàng</p>
      </td>
    </tr>
  `;
}

/**
 * Get status CSS class
 */
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

/**
 * Get status text in Vietnamese
 */
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

/**
 * Get stock CSS class
 */
function getStockClass(quantity) {
  if (quantity === 0) return "low";
  if (quantity <= 10) return "low";
  if (quantity <= 50) return "medium";
  return "high";
}

/**
 * Format price to VND
 */
function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(price);
}

/**
 * Format date
 */
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

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
  if (!text) return "";
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

