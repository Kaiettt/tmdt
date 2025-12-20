/**
 * Admin Categories Management
 */

// contextPath is declared in admin-footer.jspf

document.addEventListener("DOMContentLoaded", () => {
  checkAdminAuth();
  loadCategories();
  setupEventListeners();
});

function checkAdminAuth() {
  const token = localStorage.getItem("accessToken");
  const user = JSON.parse(localStorage.getItem("user") || "{}");
  if (!token || !user.role || user.role.toUpperCase() !== "ADMIN") {
    alert("Bạn không có quyền truy cập trang này!");
    window.location.href = contextPath + "/login";
  }
}

function setupEventListeners() {
  document.getElementById("btnAddCategory").addEventListener("click", () => openModal());
  document.getElementById("btnCloseModal").addEventListener("click", closeModal);
  document.getElementById("btnCancelModal").addEventListener("click", closeModal);
  document.getElementById("btnSaveCategory").addEventListener("click", saveCategory);
  document.getElementById("categoryModal").addEventListener("click", (e) => {
    if (e.target.id === "categoryModal") closeModal();
  });
}

async function loadCategories() {
  try {
    const response = await fetch(contextPath + "/api/v1/categories", {
      headers: { Authorization: "Bearer " + localStorage.getItem("accessToken") },
    });
    if (response.ok) {
      const result = await response.json();
      renderCategories(result.data || []);
    }
  } catch (error) {
    console.error("Error loading categories:", error);
  }
}

function renderCategories(categories) {
  const tbody = document.getElementById("categoriesTableBody");
  if (!categories || categories.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" class="text-center" style="padding: 2rem; color: #999;">Chưa có danh mục nào</td></tr>';
    return;
  }
  tbody.innerHTML = categories.map(cat => `
    <tr>
      <td><strong>#${cat.id}</strong></td>
      <td><strong>${escapeHtml(cat.name)}</strong></td>
      <td>${escapeHtml(cat.description || "")}</td>
      <td>${cat.productCount || 0}</td>
      <td>
        <div class="btn-group">
          <button class="btn-action btn-primary btn-icon" onclick="editCategory(${cat.id})" title="Sửa">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn-action btn-danger btn-icon" onclick="deleteCategory(${cat.id})" title="Xóa">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      </td>
    </tr>
  `).join("");
}

function openModal(category = null) {
  document.getElementById("modalTitle").textContent = category ? "Sửa danh mục" : "Thêm danh mục";
  document.getElementById("categoryId").value = category ? category.id : "";
  document.getElementById("categoryName").value = category ? category.name : "";
  document.getElementById("categoryDescription").value = category ? category.description || "" : "";
  document.getElementById("categoryModal").classList.add("show");
}

function closeModal() {
  document.getElementById("categoryModal").classList.remove("show");
  document.getElementById("categoryForm").reset();
}

async function saveCategory() {
  const id = document.getElementById("categoryId").value;
  const name = document.getElementById("categoryName").value.trim();
  const description = document.getElementById("categoryDescription").value.trim();

  if (!name) {
    alert("Vui lòng nhập tên danh mục!");
    return;
  }

  try {
    const url = id ? `${contextPath}/api/v1/admin/categories/${id}` : `${contextPath}/api/v1/admin/categories`;
    const method = id ? "PUT" : "POST";
    const response = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
      body: JSON.stringify({ name, description }),
    });

    if (response.ok) {
      alert(id ? "Cập nhật danh mục thành công!" : "Thêm danh mục thành công!");
      closeModal();
      loadCategories();
    } else {
      alert("Có lỗi xảy ra!");
    }
  } catch (error) {
    console.error("Error saving category:", error);
    alert("Có lỗi xảy ra!");
  }
}

async function editCategory(id) {
  try {
    const response = await fetch(`${contextPath}/api/v1/admin/categories/${id}`, {
      headers: { Authorization: "Bearer " + localStorage.getItem("accessToken") },
    });
    if (response.ok) {
      const result = await response.json();
      openModal(result.data);
    }
  } catch (error) {
    console.error("Error loading category:", error);
  }
}

async function deleteCategory(id) {
  if (!confirm("Bạn có chắc chắn muốn xóa danh mục này?")) return;
  try {
    const response = await fetch(`${contextPath}/api/v1/admin/categories/${id}`, {
      method: "DELETE",
      headers: { Authorization: "Bearer " + localStorage.getItem("accessToken") },
    });
    if (response.ok) {
      alert("Xóa danh mục thành công!");
      loadCategories();
    } else {
      alert("Không thể xóa danh mục!");
    }
  } catch (error) {
    console.error("Error deleting category:", error);
  }
}

function escapeHtml(text) {
  if (!text) return "";
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

