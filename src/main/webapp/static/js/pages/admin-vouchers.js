/**
 * Admin Vouchers Management JavaScript
 */

// contextPath is declared in admin-footer.jspf

let currentPage = 1;
let currentFilters = {
  keyword: "",
  type: "",
  status: "",
};

document.addEventListener("DOMContentLoaded", () => {
  checkAdminAuth();
  loadVouchers();
  setupEventListeners();
});

function checkAdminAuth() {
  const token = localStorage.getItem("accessToken");
  const user = JSON.parse(localStorage.getItem("user") || "{}");

  if (!token || !user.role || user.role.toUpperCase() !== "ADMIN") {
    alert("Bạn không có quyền truy cập trang này!");
    window.location.href = contextPath + "/login";
    return;
  }
}

function setupEventListeners() {
  // Search
  const searchInput = document.getElementById("searchInput");
  searchInput.addEventListener("input", debounce(() => {
    currentFilters.keyword = searchInput.value;
    currentPage = 1;
    loadVouchers();
  }, 500));

  // Filters
  document.getElementById("typeFilter").addEventListener("change", (e) => {
    currentFilters.type = e.target.value;
    currentPage = 1;
    loadVouchers();
  });

  document.getElementById("statusFilter").addEventListener("change", (e) => {
    currentFilters.status = e.target.value;
    currentPage = 1;
    loadVouchers();
  });

  // Modal buttons
  document.getElementById("btnAddVoucher").addEventListener("click", () => openModal());
  document.getElementById("btnCloseModal").addEventListener("click", closeModal);
  document.getElementById("btnCancelModal").addEventListener("click", closeModal);
  document.getElementById("btnSaveVoucher").addEventListener("click", saveVoucher);
  
  document.getElementById("voucherModal").addEventListener("click", (e) => {
    if (e.target.id === "voucherModal") closeModal();
  });
}

async function loadVouchers() {
  try {
    showLoading();

    const params = new URLSearchParams({
      page: currentPage,
      limit: 10,
    });

    if (currentFilters.keyword) params.append("code", currentFilters.keyword);
    if (currentFilters.type) params.append("discountType", currentFilters.type);
    if (currentFilters.status) params.append("status", currentFilters.status);

    const response = await fetch(
      contextPath + "/api/v1/admin/vouchers?" + params.toString(),
      {
        headers: {
          Authorization: "Bearer " + localStorage.getItem("accessToken"),
        },
      }
    );

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        renderVouchers(result.data);
        if (result.metadata && result.metadata.pagination) {
          renderPagination(result.metadata.pagination);
        }
      }
    } else if (response.status === 401) {
      window.location.href = contextPath + "/login";
    } else {
      showError("Không thể tải danh sách voucher");
    }
  } catch (error) {
    console.error("Error loading vouchers:", error);
    showError("Lỗi khi tải danh sách voucher");
  }
}

function renderVouchers(vouchers) {
  const tbody = document.getElementById("vouchersTableBody");

  if (!vouchers || vouchers.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" class="text-center">
          <p style="padding: 2rem; color: #999;">Chưa có voucher nào</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = vouchers.map(voucher => `
    <tr>
      <td><strong>#${voucher.id}</strong></td>
      <td><span class="voucher-code">${escapeHtml(voucher.code)}</span></td>
      <td>
        <span class="discount-badge">
          ${voucher.discountType === 'PERCENTAGE' 
            ? voucher.discountValue + '%' 
            : formatPrice(voucher.discountValue)}
        </span>
      </td>
      <td>${formatPrice(voucher.minOrderValue || 0)}</td>
      <td>
        ${voucher.usageLimit 
          ? (voucher.usedCount || 0) + '/' + voucher.usageLimit 
          : 'Không giới hạn'}
      </td>
      <td>
        <small>${formatDate(voucher.startDate)}<br>đến ${formatDate(voucher.endDate)}</small>
      </td>
      <td>
        <span class="status-badge ${getStatusClass(voucher)}">
          ${getStatusText(voucher)}
        </span>
      </td>
      <td>
        <div class="btn-group">
          <button class="btn-action btn-primary btn-icon" onclick="editVoucher(${voucher.id})" title="Sửa">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn-action btn-danger btn-icon" onclick="deleteVoucher(${voucher.id})" title="Xóa">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      </td>
    </tr>
  `).join("");
}

function renderPagination(pagination) {
  const container = document.getElementById("pagination");
  const { page, totalPages } = pagination;

  if (totalPages <= 1) {
    container.innerHTML = "";
    return;
  }

  let html = "";
  html += `<button class="btn-action btn-secondary" ${page === 1 ? 'disabled' : ''} onclick="changePage(${page - 1})">
    <i class="fas fa-chevron-left"></i>
  </button>`;

  for (let i = 1; i <= totalPages; i++) {
    if (i === 1 || i === totalPages || (i >= page - 1 && i <= page + 1)) {
      html += `<button class="btn-action ${i === page ? 'btn-primary' : 'btn-secondary'}" onclick="changePage(${i})">
        ${i}
      </button>`;
    } else if (i === page - 2 || i === page + 2) {
      html += `<span style="padding: 0.5rem;">...</span>`;
    }
  }

  html += `<button class="btn-action btn-secondary" ${page === totalPages ? 'disabled' : ''} onclick="changePage(${page + 1})">
    <i class="fas fa-chevron-right"></i>
  </button>`;

  container.innerHTML = html;
}

function changePage(page) {
  currentPage = page;
  loadVouchers();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function openModal(voucher = null) {
  document.getElementById("modalTitle").textContent = voucher ? "Sửa voucher" : "Thêm voucher";
  
  if (voucher) {
    document.getElementById("voucherId").value = voucher.id;
    document.getElementById("voucherCode").value = voucher.code;
    document.getElementById("voucherDescription").value = voucher.description || "";
    document.getElementById("discountType").value = voucher.discountType;
    document.getElementById("discountValue").value = voucher.discountValue;
    document.getElementById("minOrderValue").value = voucher.minOrderValue || "";
    document.getElementById("maxDiscountAmount").value = voucher.maxDiscountAmount || "";
    document.getElementById("usageLimit").value = voucher.usageLimit || "";
    document.getElementById("usageLimitPerUser").value = voucher.usageLimitPerUser || 1;
    document.getElementById("startDate").value = formatDateTimeLocal(voucher.startDate);
    document.getElementById("endDate").value = formatDateTimeLocal(voucher.endDate);
    document.getElementById("isActive").checked = voucher.isActive;
  } else {
    document.getElementById("voucherForm").reset();
    document.getElementById("voucherId").value = "";
    document.getElementById("isActive").checked = true;
    
    // Set default dates (today to 30 days from now)
    const now = new Date();
    const future = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
    document.getElementById("startDate").value = formatDateTimeLocal(now);
    document.getElementById("endDate").value = formatDateTimeLocal(future);
  }
  
  document.getElementById("voucherModal").classList.add("show");
}

function closeModal() {
  document.getElementById("voucherModal").classList.remove("show");
  document.getElementById("voucherForm").reset();
}

async function saveVoucher() {
  const id = document.getElementById("voucherId").value;
  const code = document.getElementById("voucherCode").value.trim().toUpperCase();
  const description = document.getElementById("voucherDescription").value.trim();
  const discountType = document.getElementById("discountType").value;
  const discountValue = parseFloat(document.getElementById("discountValue").value);
  const minOrderValue = parseFloat(document.getElementById("minOrderValue").value) || null;
  const maxDiscountAmount = parseFloat(document.getElementById("maxDiscountAmount").value) || null;
  const usageLimit = parseInt(document.getElementById("usageLimit").value) || null;
  const usageLimitPerUser = parseInt(document.getElementById("usageLimitPerUser").value) || 1;
  const startDate = document.getElementById("startDate").value;
  const endDate = document.getElementById("endDate").value;
  const isActive = document.getElementById("isActive").checked;

  // Validation
  if (!code || !discountValue || !startDate || !endDate) {
    alert("Vui lòng điền đầy đủ thông tin bắt buộc!");
    return;
  }

  if (discountType === 'PERCENTAGE' && (discountValue <= 0 || discountValue > 100)) {
    alert("Giá trị giảm % phải từ 0-100!");
    return;
  }

  if (new Date(startDate) >= new Date(endDate)) {
    alert("Ngày kết thúc phải sau ngày bắt đầu!");
    return;
  }

  const voucherData = {
    code,
    description,
    discountType,
    discountValue,
    minOrderValue,
    maxDiscountAmount,
    usageLimit,
    usageLimitPerUser,
    startDate,
    endDate,
    isActive,
  };

  try {
    const url = id 
      ? `${contextPath}/api/v1/admin/vouchers/${id}` 
      : `${contextPath}/api/v1/admin/vouchers`;
    const method = id ? "PUT" : "POST";

    const response = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
      body: JSON.stringify(voucherData),
    });

    if (response.ok) {
      alert(id ? "Cập nhật voucher thành công!" : "Thêm voucher thành công!");
      closeModal();
      loadVouchers();
    } else {
      const result = await response.json();
      alert(result.message || "Có lỗi xảy ra!");
    }
  } catch (error) {
    console.error("Error saving voucher:", error);
    alert("Có lỗi xảy ra!");
  }
}

async function editVoucher(id) {
  try {
    const response = await fetch(`${contextPath}/api/v1/admin/vouchers/${id}`, {
      headers: {
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
    });

    if (response.ok) {
      const result = await response.json();
      openModal(result.data);
    }
  } catch (error) {
    console.error("Error loading voucher:", error);
  }
}

async function deleteVoucher(id) {
  if (!confirm("Bạn có chắc chắn muốn xóa voucher này?")) return;

  try {
    const response = await fetch(`${contextPath}/api/v1/admin/vouchers/${id}`, {
      method: "DELETE",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
    });

    if (response.ok) {
      alert("Xóa voucher thành công!");
      loadVouchers();
    } else {
      alert("Không thể xóa voucher!");
    }
  } catch (error) {
    console.error("Error deleting voucher:", error);
    alert("Có lỗi xảy ra!");
  }
}

function showLoading() {
  const tbody = document.getElementById("vouchersTableBody");
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

function getStatusClass(voucher) {
  const now = new Date();
  const endDate = new Date(voucher.endDate);
  
  if (!voucher.isActive) return "cancelled";
  if (endDate < now) return "cancelled";
  if (voucher.usageLimit && voucher.usedCount >= voucher.usageLimit) return "cancelled";
  return "delivered";
}

function getStatusText(voucher) {
  const now = new Date();
  const endDate = new Date(voucher.endDate);
  
  if (!voucher.isActive) return "Không hoạt động";
  if (endDate < now) return "Hết hạn";
  if (voucher.usageLimit && voucher.usedCount >= voucher.usageLimit) return "Hết lượt";
  return "Đang hoạt động";
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
  }).format(date);
}

function formatDateTimeLocal(dateString) {
  if (!dateString) return "";
  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day}T${hours}:${minutes}`;
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

function showError(message) {
  alert(message);
}

