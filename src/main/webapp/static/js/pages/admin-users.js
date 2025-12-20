/**
 * Admin Users Management JavaScript
 */

// contextPath is declared in admin-footer.jspf

let currentPage = 1;
let currentFilters = {
  keyword: "",
  role: "",
  status: "",
};

document.addEventListener("DOMContentLoaded", () => {
  checkAdminAuth();
  loadUserStats();
  loadUsers();
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
  const searchInput = document.getElementById("searchInput");
  searchInput.addEventListener("input", debounce(() => {
    currentFilters.keyword = searchInput.value;
    currentPage = 1;
    loadUsers();
  }, 500));

  document.getElementById("roleFilter").addEventListener("change", (e) => {
    currentFilters.role = e.target.value;
    currentPage = 1;
    loadUsers();
  });

  document.getElementById("statusFilter").addEventListener("change", (e) => {
    currentFilters.status = e.target.value;
    currentPage = 1;
    loadUsers();
  });

  // Modal buttons
  document.getElementById("btnAddUser").addEventListener("click", () => openModal());
  document.getElementById("btnCloseModal").addEventListener("click", closeModal);
  document.getElementById("btnCancelModal").addEventListener("click", closeModal);
  document.getElementById("btnSaveUser").addEventListener("click", saveUser);
  
  document.getElementById("userModal").addEventListener("click", (e) => {
    if (e.target.id === "userModal") closeModal();
  });
}

async function loadUserStats() {
  try {
    const response = await fetch(contextPath + "/api/v1/admin/users/stats", {
      headers: {
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
    });

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        // Backend trả về: total, active, locked, pending
        document.getElementById("totalUsers").textContent = result.data.total || 0;
        document.getElementById("activeUsers").textContent = result.data.active || 0;
        document.getElementById("newUsers").textContent = result.data.pending || 0;
      }
    }
  } catch (error) {
    console.error("Error loading user stats:", error);
  }
}

async function loadUsers() {
  try {
    showLoading();

    const params = new URLSearchParams({
      page: currentPage,
      limit: 10,
    });

    if (currentFilters.keyword) params.append("keyword", currentFilters.keyword);
    if (currentFilters.role) params.append("role", currentFilters.role);
    if (currentFilters.status) params.append("status", currentFilters.status);

    const response = await fetch(
      contextPath + "/api/v1/admin/users?" + params.toString(),
      {
        headers: {
          Authorization: "Bearer " + localStorage.getItem("accessToken"),
        },
      }
    );

    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data) {
        renderUsers(result.data);
        if (result.metadata && result.metadata.pagination) {
          renderPagination(result.metadata.pagination);
        }
      }
    } else if (response.status === 401) {
      window.location.href = contextPath + "/login";
    } else {
      showError("Không thể tải danh sách người dùng");
    }
  } catch (error) {
    console.error("Error loading users:", error);
    showError("Lỗi khi tải danh sách người dùng");
  }
}

function renderUsers(users) {
  const tbody = document.getElementById("usersTableBody");

  if (!users || users.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="text-center">
          <p style="padding: 2rem; color: #999;">Không tìm thấy người dùng nào</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = users.map(user => `
    <tr>
      <td><strong>#${user.id}</strong></td>
      <td>
        <div class="user-info-cell">
          <div class="user-avatar">
            ${getInitials(user.fullName || user.username)}
          </div>
          <div class="user-details">
            <h4>${escapeHtml(user.fullName || user.username)}</h4>
            <p>${escapeHtml(user.email)}</p>
          </div>
        </div>
      </td>
      <td>${escapeHtml(user.phoneNumber || "N/A")}</td>
      <td>
        <span class="role-badge ${user.role.toLowerCase()}">
          ${getRoleText(user.role)}
        </span>
      </td>
      <td>
        <label class="toggle-switch">
          <input type="checkbox" ${user.status === 'active' ? 'checked' : ''} 
                 onchange="toggleUserStatus(${user.id}, this.checked)">
          <span class="toggle-slider"></span>
        </label>
      </td>
      <td>${formatDate(user.createdAt)}</td>
      <td>
        <button class="btn-action btn-danger btn-icon" 
                onclick="deleteUser(${user.id})" 
                title="Xóa"
                ${user.role === 'admin' ? 'disabled' : ''}>
          <i class="fas fa-trash"></i>
        </button>
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
  loadUsers();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

async function toggleUserStatus(userId, isActive) {
  try {
    const newStatus = isActive ? "active" : "locked";
    const response = await fetch(
      `${contextPath}/api/v1/admin/users/${userId}/status`,
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
      showSuccess(isActive ? "Đã kích hoạt người dùng" : "Đã khóa người dùng");
      loadUserStats();
    } else {
      showError("Không thể cập nhật trạng thái");
      loadUsers(); // Reload to reset toggle
    }
  } catch (error) {
    console.error("Error toggling user status:", error);
    showError("Lỗi khi cập nhật trạng thái");
    loadUsers();
  }
}

async function deleteUser(userId) {
  if (!confirm("Bạn có chắc chắn muốn xóa người dùng này?")) {
    return;
  }

  try {
    const response = await fetch(`${contextPath}/api/v1/admin/users/${userId}`, {
      method: "DELETE",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
    });

    if (response.ok) {
      showSuccess("Xóa người dùng thành công!");
      loadUsers();
      loadUserStats();
    } else {
      showError("Không thể xóa người dùng");
    }
  } catch (error) {
    console.error("Error deleting user:", error);
    showError("Lỗi khi xóa người dùng");
  }
}

function showLoading() {
  const tbody = document.getElementById("usersTableBody");
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

function getInitials(name) {
  if (!name) return "?";
  const parts = name.trim().split(" ");
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return name.substring(0, 2).toUpperCase();
}

function getRoleText(role) {
  const roleMap = {
    admin: "Quản trị",
    customer: "Khách hàng",
  };
  return roleMap[role.toLowerCase()] || role;
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
  alert(message);
}

function showError(message) {
  alert(message);
}

/**
 * Open modal for create/edit user
 */
function openModal(user = null) {
  document.getElementById("modalTitle").textContent = user ? "Chỉnh sửa người dùng" : "Thêm người dùng";
  document.getElementById("userId").value = user ? user.id : "";
  document.getElementById("userEmail").value = user ? user.email : "";
  document.getElementById("userFullName").value = user ? user.fullName : "";
  document.getElementById("userPassword").value = "";
  document.getElementById("userPhoneNumber").value = user ? (user.phoneNumber || "") : "";
  document.getElementById("userRole").value = user ? user.role.toLowerCase() : "customer";
  document.getElementById("userStatus").value = user ? user.status.toLowerCase() : "active";
  
  // Password is required for new users, optional for edit
  const passwordInput = document.getElementById("userPassword");
  if (user) {
    passwordInput.required = false;
    passwordInput.placeholder = "Để trống nếu không muốn thay đổi";
  } else {
    passwordInput.required = true;
    passwordInput.placeholder = "";
  }
  
  document.getElementById("userModal").classList.add("show");
}

/**
 * Close modal
 */
function closeModal() {
  document.getElementById("userModal").classList.remove("show");
  document.getElementById("userForm").reset();
  document.getElementById("userPassword").required = true;
}

/**
 * Save user (create or update)
 */
async function saveUser() {
  const id = document.getElementById("userId").value;
  const email = document.getElementById("userEmail").value.trim();
  const username = document.getElementById("userUsername").value.trim();
  const fullName = document.getElementById("userFullName").value.trim();
  const password = document.getElementById("userPassword").value;
  const phoneNumber = document.getElementById("userPhoneNumber").value.trim();
  const role = document.getElementById("userRole").value;
  const status = document.getElementById("userStatus").value;

  // Validation
  if (!email || !fullName) {
    alert("Vui lòng điền đầy đủ thông tin bắt buộc!");
    return;
  }

  if (!id && !password) {
    alert("Vui lòng nhập mật khẩu!");
    return;
  }

  if (password && password.length < 6) {
    alert("Mật khẩu phải có ít nhất 6 ký tự!");
    return;
  }

  const userData = {
    email,
    username: username || null, // Auto-generate from email if empty
    fullName,
    phoneNumber: phoneNumber || null,
    role,
    status,
  };

  // Only include password if it's provided
  if (password) {
    userData.password = password;
  }

  try {
    const url = id 
      ? `${contextPath}/api/v1/admin/users/${id}` 
      : `${contextPath}/api/v1/admin/users`;
    const method = id ? "PUT" : "POST";

    const response = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
      body: JSON.stringify(userData),
    });

    if (response.ok) {
      alert(id ? "Cập nhật người dùng thành công!" : "Thêm người dùng thành công!");
      closeModal();
      loadUsers();
      loadUserStats();
    } else {
      const result = await response.json();
      alert(result.message || "Có lỗi xảy ra!");
    }
  } catch (error) {
    console.error("Error saving user:", error);
    alert("Có lỗi xảy ra!");
  }
}

