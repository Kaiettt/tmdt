/**
 * Admin Brands Management
 */

// contextPath is declared in admin-footer.jspf

document.addEventListener("DOMContentLoaded", () => {
  checkAdminAuth();
  loadBrands();
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
  document.getElementById("btnAddBrand").addEventListener("click", () => openModal());
  document.getElementById("btnCloseModal").addEventListener("click", closeModal);
  document.getElementById("btnCancelModal").addEventListener("click", closeModal);
  document.getElementById("btnSaveBrand").addEventListener("click", saveBrand);
  document.getElementById("brandModal").addEventListener("click", (e) => {
    if (e.target.id === "brandModal") closeModal();
  });
}

async function loadBrands() {
  try {
    const response = await fetch(contextPath + "/api/v1/brands", {
      headers: { Authorization: "Bearer " + localStorage.getItem("accessToken") },
    });
    if (response.ok) {
      const result = await response.json();
      renderBrands(result.data || []);
    }
  } catch (error) {
    console.error("Error loading brands:", error);
  }
}

function renderBrands(brands) {
  const tbody = document.getElementById("brandsTableBody");
  if (!brands || brands.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" class="text-center" style="padding: 2rem; color: #999;">Chưa có thương hiệu nào</td></tr>';
    return;
  }
  tbody.innerHTML = brands.map(brand => `
    <tr>
      <td><strong>#${brand.id}</strong></td>
      <td><strong>${escapeHtml(brand.name)}</strong></td>
      <td>${escapeHtml(brand.description || "")}</td>
      <td>${brand.productCount || 0}</td>
      <td>
        <div class="btn-group">
          <button class="btn-action btn-primary btn-icon" onclick="editBrand(${brand.id})" title="Sửa">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn-action btn-danger btn-icon" onclick="deleteBrand(${brand.id})" title="Xóa">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      </td>
    </tr>
  `).join("");
}

function openModal(brand = null) {
  document.getElementById("modalTitle").textContent = brand ? "Sửa thương hiệu" : "Thêm thương hiệu";
  document.getElementById("brandId").value = brand ? brand.id : "";
  document.getElementById("brandName").value = brand ? brand.name : "";
  document.getElementById("brandLogoUrl").value = brand ? brand.logoUrl || "" : "";
  document.getElementById("brandDescription").value = brand ? brand.description || "" : "";
  document.getElementById("brandModal").classList.add("show");
}

function closeModal() {
  document.getElementById("brandModal").classList.remove("show");
  document.getElementById("brandForm").reset();
}

async function saveBrand() {
  const id = document.getElementById("brandId").value;
  const name = document.getElementById("brandName").value.trim();
  const description = document.getElementById("brandDescription").value.trim();
  const logoUrl = document.getElementById("brandLogoUrl").value.trim();

  if (!name) {
    alert("Vui lòng nhập tên thương hiệu!");
    return;
  }

  try {
    const url = id ? `${contextPath}/api/v1/admin/brands/${id}` : `${contextPath}/api/v1/admin/brands`;
    const method = id ? "PUT" : "POST";
    const response = await fetch(url, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + localStorage.getItem("accessToken"),
      },
      body: JSON.stringify({ name, description, logoUrl }),
    });

    if (response.ok) {
      alert(id ? "Cập nhật thương hiệu thành công!" : "Thêm thương hiệu thành công!");
      closeModal();
      loadBrands();
    } else {
      alert("Có lỗi xảy ra!");
    }
  } catch (error) {
    console.error("Error saving brand:", error);
    alert("Có lỗi xảy ra!");
  }
}

async function editBrand(id) {
  try {
    const response = await fetch(`${contextPath}/api/v1/admin/brands/${id}`, {
      headers: { Authorization: "Bearer " + localStorage.getItem("accessToken") },
    });
    if (response.ok) {
      const result = await response.json();
      openModal(result.data);
    }
  } catch (error) {
    console.error("Error loading brand:", error);
  }
}

async function deleteBrand(id) {
  if (!confirm("Bạn có chắc chắn muốn xóa thương hiệu này?")) return;
  try {
    const response = await fetch(`${contextPath}/api/v1/admin/brands/${id}`, {
      method: "DELETE",
      headers: { Authorization: "Bearer " + localStorage.getItem("accessToken") },
    });
    if (response.ok) {
      alert("Xóa thương hiệu thành công!");
      loadBrands();
    } else {
      alert("Không thể xóa thương hiệu!");
    }
  } catch (error) {
    console.error("Error deleting brand:", error);
  }
}

function escapeHtml(text) {
  if (!text) return "";
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

