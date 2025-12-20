/**
 * User Orders Page JavaScript
 * Handles order history display, filtering, and management
 */

// Global variables
// contextPath is already declared in footer.jspf
let allOrders = [];
let currentFilter = "ALL";
let currentPage = 1;
const ordersPerPage = 10;
let currentOrderId = null;

// Check authentication
document.addEventListener("DOMContentLoaded", () => {
  // Only auto-initialize if we're on the standalone orders page (not profile page with tabs)
  const isProfilePage = document.querySelector('.tab-content#tab-orders') !== null;
  
  if (isProfilePage) {
    // On profile page, just setup event listeners but don't load yet
    // Orders will be loaded when user clicks the "Đơn hàng của tôi" tab
    setupEventListeners();
    return;
  }
  
  // Standalone orders page - check authentication and initialize
  const token = localStorage.getItem("accessToken");
  if (!token) {
    showToast("Vui lòng đăng nhập để xem đơn hàng", "warning");
    setTimeout(() => {
      window.location.href = contextPath + "/login?redirect=" + encodeURIComponent("/orders");
    }, 2000);
    return;
  }

  // Initialize page
  initOrdersPage();
});

/**
 * Initialize orders page
 */
async function initOrdersPage() {
  try {
    // Load user info
    await loadUserInfo();

    // Load orders
    await loadOrders();

    // Setup event listeners
    setupEventListeners();
  } catch (error) {
    console.error("Error initializing orders page:", error);
    showError("Không thể tải dữ liệu. Vui lòng thử lại sau.");
  }
}

/**
 * Load user info
 */
async function loadUserInfo() {
  try {
    const response = await fetch(contextPath + "/api/v1/user/me", {
      method: "GET",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
        "Content-Type": "application/json",
      },
    });

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        document.getElementById("userName").textContent =
          result.data.fullName || "User";
        document.getElementById("userEmail").textContent =
          result.data.email || "";
      }
    }
  } catch (error) {
    console.error("Error loading user info:", error);
  }
}

/**
 * Load orders from API
 */
async function loadOrders() {
  try {
    showLoading();

    const response = await fetch(contextPath + "/api/v1/orders", {
      method: "GET",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
        "Content-Type": "application/json",
      },
    });

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        allOrders = result.data;
        renderOrders();
      } else {
        showEmpty();
      }
    } else if (response.status === 401) {
      // Token expired, try refresh
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        return loadOrders(); // Retry
      } else {
        redirectToLogin();
      }
    } else {
      throw new Error("Failed to load orders");
    }
  } catch (error) {
    console.error("Error loading orders:", error);
    showError("Không thể tải danh sách đơn hàng. Vui lòng thử lại sau.");
  }
}

/**
 * Render orders list
 */
function renderOrders() {
  const ordersList = document.getElementById("ordersList");
  const loadingState = document.getElementById("loadingState");
  const emptyState = document.getElementById("emptyState");
  const errorState = document.getElementById("errorState");

  // Hide all states
  loadingState.style.display = "none";
  emptyState.style.display = "none";
  errorState.style.display = "none";

  // Filter orders
  const filteredOrders =
    currentFilter === "ALL"
      ? allOrders
      : allOrders.filter((order) => order.status === currentFilter);

  // Check if empty
  if (filteredOrders.length === 0) {
    showEmpty();
    return;
  }

  // Pagination
  const startIndex = (currentPage - 1) * ordersPerPage;
  const endIndex = startIndex + ordersPerPage;
  const paginatedOrders = filteredOrders.slice(startIndex, endIndex);

  // Render orders
  ordersList.innerHTML = paginatedOrders
    .map((order) => createOrderCard(order))
    .join("");

  // Render pagination
  renderPagination(filteredOrders.length);
}

/**
 * Create order card HTML
 */
function createOrderCard(order) {
  const statusClass = order.status.toLowerCase();
  const statusText = getStatusText(order.status);

  return `
    <div class="order-card" data-order-id="${order.id}">
      <div class="order-header">
        <div>
          <div class="order-code">#${escapeHtml(order.orderCode || order.id)}</div>
          <div class="order-date">
            <i class="bx bx-calendar"></i>
            ${formatDate(order.createdAt)}
          </div>
        </div>
        <div class="order-status-badge ${statusClass}">
          ${statusText}
        </div>
      </div>

      <!-- Items section removed - Backend doesn't return items in list API -->
      <!-- Items are only shown in detail modal -->

      <div class="order-footer">
        <div class="order-total">
          Tổng cộng: <span class="amount">${formatPrice(order.totalAmount)}</span>
        </div>
        <div class="order-actions">
          <button class="btn btn-primary" onclick="viewOrderDetail(${order.id})">
            <i class="bx bx-show"></i> Xem chi tiết
          </button>
          ${
            order.status === "PENDING"
              ? `
            <button class="btn btn-danger" onclick="confirmCancelOrder(${order.id})">
              <i class="bx bx-x"></i> Hủy đơn
            </button>
          `
              : ""
          }
          ${
            order.status === "DELIVERED"
              ? `
            <button class="btn btn-success" onclick="reorder(${order.id})">
              <i class="bx bx-repeat"></i> Mua lại
            </button>
          `
              : ""
          }
        </div>
      </div>
    </div>
  `;
}

/**
 * View order detail in modal
 */
async function viewOrderDetail(orderId) {
  try {
    const response = await fetch(
      contextPath + "/api/v1/orders/" + orderId,
      {
        method: "GET",
        headers: {
          Authorization: "Bearer " + localStorage.getItem("accessToken"),
          "Content-Type": "application/json",
        },
      }
    );

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        showOrderDetailModal(result.data);
      } else {
        showToast("Không thể tải chi tiết đơn hàng", "error");
      }
    } else if (response.status === 401) {
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        return viewOrderDetail(orderId);
      } else {
        redirectToLogin();
      }
    } else {
      throw new Error("Failed to load order detail");
    }
  } catch (error) {
    console.error("Error loading order detail:", error);
    showToast("Đã xảy ra lỗi khi tải chi tiết đơn hàng", "error");
  }
}

/**
 * Show order detail modal
 */
function showOrderDetailModal(order) {
  console.log("Order detail data:", order);
  console.log("Shipping address:", order.shippingAddress);
  
  const modal = document.getElementById("orderDetailModal");
  const statusClass = order.status.toLowerCase();
  const statusText = getStatusText(order.status);

  // Populate modal
  document.getElementById("modalOrderCode").textContent =
    "#" + (order.orderCode || order.id);
  document.getElementById("modalOrderDate").textContent = formatDate(
    order.createdAt
  );
  document.getElementById("modalOrderStatus").textContent = statusText;
  document.getElementById("modalPaymentMethod").textContent =
    getPaymentMethodText(order.paymentMethod);

  // Status badge
  const statusBadge = document.getElementById("orderStatus");
  statusBadge.className = "order-status-badge " + statusClass;
  statusBadge.textContent = statusText;

  // Shipping address - Backend returns "shippingInfo" not "shippingAddress"
  const shippingAddr = order.shippingInfo || order.shippingAddress || order.deliveryAddress;
  console.log("Resolved shipping address:", shippingAddr);
  
  if (shippingAddr) {
    const addr = shippingAddr;
    
    // Backend fields: recipientName, phoneNumber, email, streetAddress, city
    const recipientName = addr.recipientName || addr.fullName || addr.name || "";
    const phoneNumber = addr.phoneNumber || addr.phone || "";
    const streetAddress = addr.streetAddress || addr.street || addr.address || "";
    const city = addr.city || addr.province || "";
    
    // Build full address
    const addressParts = [streetAddress, city].filter(part => part && part.trim());
    
    console.log("Parsed address parts:", {
      recipientName,
      phoneNumber,
      addressParts
    });
    
    document.getElementById("modalRecipientName").textContent = recipientName || "Không có tên";
    document.getElementById("modalRecipientPhone").textContent = phoneNumber || "Không có SĐT";
    document.getElementById("modalRecipientAddress").textContent = 
      addressParts.length > 0 ? addressParts.join(", ") : "Không có địa chỉ chi tiết";
  } else {
    // If no shipping address, show placeholder
    document.getElementById("modalRecipientName").textContent = "Không có thông tin";
    document.getElementById("modalRecipientPhone").textContent = "";
    document.getElementById("modalRecipientAddress").textContent = "";
  }

  // Order items - Backend returns flat structure: productId, productName, quantity, price
  const modalOrderItems = document.getElementById("modalOrderItems");
  modalOrderItems.innerHTML = order.items
    .map(
      (item) => `
    <div class="order-item">
      <img 
        src="${escapeHtml(item.thumbnailUrl || item.product?.thumbnailUrl || "https://via.placeholder.com/80x80/cccccc/666666?text=No+Image")}" 
        alt="${escapeHtml(item.productName || item.product?.name || "Product")}"
        class="order-item-image"
      />
      <div class="order-item-info">
        <div class="order-item-name">${escapeHtml(item.productName || item.product?.name || "Sản phẩm")}</div>
        <div class="order-item-quantity">Số lượng: ${item.quantity}</div>
      </div>
      <div class="order-item-price">${formatPrice(item.price * item.quantity)}</div>
    </div>
  `
    )
    .join("");

  // Order summary
  const subtotal = order.items.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0
  );
  document.getElementById("modalSubtotal").textContent = formatPrice(subtotal);
  document.getElementById("modalShippingFee").textContent = formatPrice(
    order.shippingFee || 0
  );

  if (order.discountAmount && order.discountAmount > 0) {
    document.getElementById("modalDiscountRow").style.display = "flex";
    document.getElementById("modalDiscount").textContent =
      "-" + formatPrice(order.discountAmount);
  } else {
    document.getElementById("modalDiscountRow").style.display = "none";
  }

  document.getElementById("modalTotal").textContent = formatPrice(
    order.totalAmount
  );

  // Order actions
  const actionsSection = document.getElementById("modalOrderActions");
  let actionsHTML = "";

  if (order.status === "PENDING") {
    actionsHTML = `
      <button class="btn btn-danger" onclick="confirmCancelOrder(${order.id})">
        <i class="bx bx-x"></i> Hủy đơn hàng
      </button>
    `;
  } else if (order.status === "DELIVERED") {
    actionsHTML = `
      <button class="btn btn-success" onclick="reorder(${order.id})">
        <i class="bx bx-repeat"></i> Mua lại
      </button>
      <button class="btn btn-primary" onclick="printInvoice(${order.id})">
        <i class="bx bx-printer"></i> In hóa đơn
      </button>
    `;
  }

  actionsSection.innerHTML = actionsHTML;

  // Show modal
  modal.classList.add("show");
}

/**
 * Confirm cancel order
 */
function confirmCancelOrder(orderId) {
  currentOrderId = orderId;
  const modal = document.getElementById("confirmCancelModal");
  modal.classList.add("show");

  // Close order detail modal if open
  document.getElementById("orderDetailModal").classList.remove("show");
}

/**
 * Cancel order
 */
async function cancelOrder() {
  if (!currentOrderId) return;

  const reason = document.getElementById("cancelReason").value.trim();

  try {
    const response = await fetch(
      contextPath + "/api/v1/orders/" + currentOrderId + "/cancel",
      {
        method: "PUT",
        headers: {
          Authorization: "Bearer " + localStorage.getItem("accessToken"),
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ reason: reason || "Khách hàng hủy" }),
      }
    );

    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        showToast("Hủy đơn hàng thành công!", "success");

        // Close modal
        document.getElementById("confirmCancelModal").classList.remove("show");
        document.getElementById("cancelReason").value = "";

        // Reload orders
        await loadOrders();
      } else {
        showToast(result.message || "Không thể hủy đơn hàng", "error");
      }
    } else if (response.status === 401) {
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        return cancelOrder();
      } else {
        redirectToLogin();
      }
    } else {
      throw new Error("Failed to cancel order");
    }
  } catch (error) {
    console.error("Error cancelling order:", error);
    showToast("Đã xảy ra lỗi khi hủy đơn hàng", "error");
  }
}

/**
 * Reorder - Add all items to cart
 */
async function reorder(orderId) {
  try {
    const order = allOrders.find((o) => o.id === orderId);
    if (!order || !order.items) {
      showToast("Không tìm thấy đơn hàng", "error");
      return;
    }

    let successCount = 0;

    for (const item of order.items) {
      try {
        const response = await fetch(contextPath + "/api/v1/cart/add", {
          method: "POST",
          headers: {
            Authorization: "Bearer " + localStorage.getItem("accessToken"),
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            productId: item.product.id,
            quantity: item.quantity,
          }),
        });

        if (response.ok) {
          successCount++;
        }
      } catch (error) {
        console.error("Error adding item to cart:", error);
      }
    }

    if (successCount > 0) {
      showToast(
        `Đã thêm ${successCount}/${order.items.length} sản phẩm vào giỏ hàng!`,
        "success"
      );
      // Update cart badge
      if (typeof updateCartBadge === 'function') {
        updateCartBadge();
      }
      setTimeout(() => {
        window.location.href = contextPath + "/products";
      }, 1500);
    } else {
      showToast("Không thể thêm sản phẩm vào giỏ hàng", "error");
    }
  } catch (error) {
    console.error("Error reordering:", error);
    showToast("Đã xảy ra lỗi khi mua lại", "error");
  }
}

/**
 * Print invoice - Send invoice via email
 */
async function printInvoice(orderId) {
  try {
    // TODO: Implement send invoice API endpoint
    // Endpoint: POST /api/v1/orders/{orderId}/send-invoice
    showToast(
      "Chức năng gửi hóa đơn qua email đang được phát triển. Vui lòng liên hệ hotline 1800.1234 để nhận hóa đơn.",
      "info"
    );
    return;
    
    /* Uncomment when API is ready
    showToast("Đang gửi hóa đơn qua email...", "info");
    
    const response = await fetch(
      contextPath + "/api/v1/orders/" + orderId + "/send-invoice",
      {
        method: "POST",
        headers: {
          Authorization: "Bearer " + localStorage.getItem("accessToken"),
          "Content-Type": "application/json",
        },
      }
    );

    if (response.ok) {
      const result = await response.json();
      showToast("Hóa đơn đã được gửi đến email của bạn!", "success");
    } else if (response.status === 404) {
      showToast(
        "Chức năng gửi hóa đơn qua email đang được phát triển. Vui lòng liên hệ hotline 1800.1234 để nhận hóa đơn.",
        "info"
      );
    } else {
      throw new Error("Failed to send invoice");
    }
    */
  } catch (error) {
    console.error("Error sending invoice:", error);
    showToast(
      "Có lỗi xảy ra. Vui lòng thử lại sau.",
      "error"
    );
  }
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
  // Filter buttons
  document.querySelectorAll(".filter-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      // Update active state
      document
        .querySelectorAll(".filter-btn")
        .forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");

      // Update filter and reload
      currentFilter = btn.dataset.status;
      currentPage = 1;
      renderOrders();
    });
  });

  // Modal close buttons
  document.querySelectorAll(".modal .close").forEach((closeBtn) => {
    closeBtn.addEventListener("click", () => {
      closeBtn.closest(".modal").classList.remove("show");
    });
  });

  // Close modal on background click
  window.addEventListener("click", (e) => {
    if (e.target.classList.contains("modal")) {
      e.target.classList.remove("show");
    }
  });

  // Cancel order confirmation
  document.getElementById("btnCancelYes").addEventListener("click", cancelOrder);
  document.getElementById("btnCancelNo").addEventListener("click", () => {
    document.getElementById("confirmCancelModal").classList.remove("show");
    document.getElementById("cancelReason").value = "";
  });
}

/**
 * Render pagination
 */
function renderPagination(totalOrders) {
  const pagination = document.getElementById("pagination");
  const totalPages = Math.ceil(totalOrders / ordersPerPage);

  if (totalPages <= 1) {
    pagination.style.display = "none";
    return;
  }

  pagination.style.display = "flex";

  let paginationHTML = `
    <button class="pagination-btn" onclick="changePage(${currentPage - 1})" ${currentPage === 1 ? "disabled" : ""}>
      <i class="bx bx-chevron-left"></i> Trước
    </button>
  `;

  // Page numbers
  for (let i = 1; i <= totalPages; i++) {
    if (
      i === 1 ||
      i === totalPages ||
      (i >= currentPage - 1 && i <= currentPage + 1)
    ) {
      paginationHTML += `
        <button class="pagination-btn ${i === currentPage ? "active" : ""}" onclick="changePage(${i})">
          ${i}
        </button>
      `;
    } else if (i === currentPage - 2 || i === currentPage + 2) {
      paginationHTML += `<span style="padding: 0 0.5rem;">...</span>`;
    }
  }

  paginationHTML += `
    <button class="pagination-btn" onclick="changePage(${currentPage + 1})" ${currentPage === totalPages ? "disabled" : ""}>
      Sau <i class="bx bx-chevron-right"></i>
    </button>
  `;

  pagination.innerHTML = paginationHTML;
}

/**
 * Change page
 */
function changePage(page) {
  currentPage = page;
  renderOrders();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

/**
 * Utility Functions
 */

function showLoading() {
  document.getElementById("loadingState").style.display = "block";
  document.getElementById("emptyState").style.display = "none";
  document.getElementById("errorState").style.display = "none";
  document.getElementById("ordersList").innerHTML = "";
}

function showEmpty() {
  document.getElementById("loadingState").style.display = "none";
  document.getElementById("emptyState").style.display = "block";
  document.getElementById("errorState").style.display = "none";
  document.getElementById("ordersList").innerHTML = "";
}

function showError(message) {
  document.getElementById("loadingState").style.display = "none";
  document.getElementById("emptyState").style.display = "none";
  document.getElementById("errorState").style.display = "block";
  document.getElementById("errorMessage").textContent = message;
  document.getElementById("ordersList").innerHTML = "";
}

function getStatusText(status) {
  const statusMap = {
    PENDING: "Chờ xác nhận",
    CONFIRMED: "Đã xác nhận",
    SHIPPING: "Đang giao",
    DELIVERED: "Đã giao",
    CANCELLED: "Đã hủy",
  };
  return statusMap[status] || status;
}

function getPaymentMethodText(method) {
  const methodMap = {
    COD: "Thanh toán khi nhận hàng",
    CREDIT_CARD: "Thẻ tín dụng",
    BANK_TRANSFER: "Chuyển khoản ngân hàng",
    VNPAY: "VNPay",
    MOMO: "MoMo",
  };
  return methodMap[method] || method;
}

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(price);
}

function formatDate(dateString) {
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
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

function showToast(message, type = "info") {
  // Create toast element
  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <i class="bx ${type === "success" ? "bx-check-circle" : type === "error" ? "bx-error" : "bx-info-circle"}"></i>
    <span>${message}</span>
  `;

  // Add to body
  document.body.appendChild(toast);

  // Show animation
  setTimeout(() => toast.classList.add("show"), 100);

  // Hide after 3 seconds
  setTimeout(() => {
    toast.classList.remove("show");
    setTimeout(() => toast.remove(), 300);
  }, 3000);

  // Add toast styles if not exists
  if (!document.getElementById("toast-styles")) {
    const style = document.createElement("style");
    style.id = "toast-styles";
    style.textContent = `
      .toast {
        position: fixed;
        top: 100px;
        right: 20px;
        background: white;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        display: flex;
        align-items: center;
        gap: 0.75rem;
        z-index: 10000;
        opacity: 0;
        transform: translateX(400px);
        transition: all 0.3s ease;
      }
      .toast.show {
        opacity: 1;
        transform: translateX(0);
      }
      .toast i {
        font-size: 1.5rem;
      }
      .toast-success {
        border-left: 4px solid #28a745;
      }
      .toast-success i {
        color: #28a745;
      }
      .toast-error {
        border-left: 4px solid #dc3545;
      }
      .toast-error i {
        color: #dc3545;
      }
      .toast-info, .toast-warning {
        border-left: 4px solid #ffc107;
      }
      .toast-info i, .toast-warning i {
        color: #ffc107;
      }
    `;
    document.head.appendChild(style);
  }
}

async function refreshAccessToken() {
  try {
    const refreshToken = localStorage.getItem("refreshToken");
    if (!refreshToken) return false;

    const response = await fetch(contextPath + "/api/v1/auth/refresh-token", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken }),
    });

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        localStorage.setItem("accessToken", result.data.accessToken);
        if (result.data.refreshToken) {
          localStorage.setItem("refreshToken", result.data.refreshToken);
        }
        return true;
      }
    }
    return false;
  } catch (error) {
    console.error("Error refreshing token:", error);
    return false;
  }
}

function redirectToLogin() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  showToast("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", "warning");
  setTimeout(() => {
    window.location.href = contextPath + "/login?redirect=" + encodeURIComponent("/orders");
  }, 2000);
}

