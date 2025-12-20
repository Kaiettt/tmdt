// Profile Page JavaScript - Complete Rewrite
const API_BASE_URL = "/api/v1";

// Global variables for location data
let allProvinces = [];
let allWards = [];

// Choices.js instances
let provinceChoice = null;
let wardChoice = null;

// ==================== UTILITY FUNCTIONS ====================

// Get authentication token
function getToken() {
  return localStorage.getItem("accessToken");
}

// Check authentication status
function checkAuth() {
  const token = getToken();
  if (!token) {
    window.location.href = "/login";
    return false;
  }
  return true;
}

// Fetch API with authentication
async function fetchAPI(url, options = {}) {
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });

    // Handle unauthorized
    if (response.status === 401) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("user");
      window.location.href = "/login";
      return null;
    }

    return response;
  } catch (error) {
    console.error("Fetch error:", error);
    throw error;
  }
}

// Show message helper
function showMessage(elementId, message, isSuccess = true) {
  const messageEl = document.getElementById(elementId);
  if (!messageEl) return;

  messageEl.textContent = message;
  messageEl.className = `message ${isSuccess ? "success" : "error"}`;
  messageEl.style.display = "block";

  setTimeout(() => {
    messageEl.style.display = "none";
  }, 5000);
}

// ==================== LOCATION FUNCTIONS ====================

// Initialize Choices.js for select dropdowns
function initializeChoices() {
  const provinceSelect = document.getElementById("province");
  const wardSelect = document.getElementById("ward");

  if (provinceSelect && typeof Choices !== "undefined") {
    provinceChoice = new Choices(provinceSelect, {
      searchEnabled: true,
      searchPlaceholderValue: "Tìm kiếm tỉnh/thành phố...",
      noResultsText: "Không tìm thấy",
      noChoicesText: "Không có lựa chọn",
      itemSelectText: "Chọn",
      position: "bottom", // Dropdown sổ xuống
      removeItemButton: false,
      shouldSort: false,
      placeholder: true,
      placeholderValue: "-- Chọn Tỉnh/Thành phố --",
      searchResultLimit: 100,
      shouldSortItems: false, // Keep original order for scrollToChoice to work
      classNames: {
        containerOuter: "choices",
        containerInner: "choices__inner",
        input: "choices__input",
        inputCloned: "choices__input--cloned",
        list: "choices__list",
        listItems: "choices__list--multiple",
        listSingle: "choices__list--single",
        listDropdown: "choices__list--dropdown",
        item: "choices__item",
        itemSelectable: "choices__item--selectable",
        itemDisabled: "choices__item--disabled",
        itemChoice: "choices__item--choice",
        placeholder: "choices__placeholder",
        group: "choices__group",
        groupHeading: "choices__heading",
        button: "choices__button",
        activeState: "is-active",
        focusState: "is-focused",
        openState: "is-open",
        disabledState: "is-disabled",
        highlightedState: "is-highlighted",
        selectedState: "is-selected",
        flippedState: "is-flipped",
        loadingState: "is-loading",
      },
    });

    // ✅ Add Choices.js event listener for province change
    provinceSelect.addEventListener("change", handleProvinceChange);
    console.log("✅ Province Choices.js event listener added");
  }

  if (wardSelect && typeof Choices !== "undefined") {
    wardChoice = new Choices(wardSelect, {
      searchEnabled: true,
      searchPlaceholderValue: "Tìm kiếm xã/phường...",
      noResultsText: "Không tìm thấy",
      noChoicesText: "Vui lòng chọn tỉnh/thành phố trước",
      itemSelectText: "Chọn",
      position: "bottom",
      removeItemButton: false,
      shouldSort: false,
      placeholder: true,
      placeholderValue: "-- Chọn Xã/Phường --",
      searchResultLimit: 100,
    });

    // ✅ Add Choices.js event listener for ward change
    wardSelect.addEventListener("change", handleWardChange);
    console.log("✅ Ward Choices.js event listener added");
  }

  console.log("✅ Choices.js initialized");
}

// Load all provinces
async function loadProvinces() {
  try {
    const response = await fetchAPI(`${API_BASE_URL}/location/provinces`);

    if (response && response.ok) {
      const data = await response.json();
      if (data.success) {
        allProvinces = data.data;
        populateProvinceSelect();
        console.log(`✅ Loaded ${allProvinces.length} provinces`);
      }
    }
  } catch (error) {
    console.error("Error loading provinces:", error);
  }
}

// Load wards for a specific province
async function loadWardsForProvince(provinceCode) {
  try {
    const response = await fetchAPI(
      `${API_BASE_URL}/location/provinces/${provinceCode}/wards`
    );

    if (response && response.ok) {
      const data = await response.json();
      if (data.success) {
        console.log(
          `✅ Loaded ${data.data.length} wards for province ${provinceCode}`
        );
        return data.data;
      }
    }
    return [];
  } catch (error) {
    console.error("Error loading wards for province:", error);
    return [];
  }
}

// Populate province select
function populateProvinceSelect() {
  if (provinceChoice) {
    // Clear existing choices
    provinceChoice.clearStore();

    // Add new choices
    const choices = allProvinces.map((province) => ({
      value: province.name,
      label: province.name,
      customProperties: {
        code: province.code,
      },
    }));

    provinceChoice.setChoices(choices, "value", "label", true);
    console.log(`✅ Loaded ${choices.length} provinces into Choices.js`);
  } else {
    // Fallback to native select
    const provinceSelect = document.getElementById("province");
    if (!provinceSelect) return;

    provinceSelect.innerHTML =
      '<option value="">-- Chọn Tỉnh/Thành phố --</option>';

    allProvinces.forEach((province) => {
      const option = document.createElement("option");
      option.value = province.name;
      option.textContent = province.name;
      option.dataset.code = province.code;
      provinceSelect.appendChild(option);
    });
  }
}

// Populate ward select with array of wards
function populateWardSelect(wards) {
  if (wardChoice) {
    // Clear existing choices
    wardChoice.clearStore();

    if (!wards || wards.length === 0) {
      wardChoice.setChoices(
        [
          {
            value: "",
            label: "-- Chọn Xã/Phường --",
            selected: true,
            disabled: false,
          },
        ],
        "value",
        "label",
        true
      );
      return;
    }

    // Add new choices
    const choices = wards.map((ward) => ({
      value: ward.name,
      label: ward.name,
      customProperties: {
        code: ward.code,
      },
    }));

    wardChoice.setChoices(choices, "value", "label", true);
    console.log(`✅ Populated ${choices.length} wards in Choices.js`);
  } else {
    // Fallback to native select
    const wardSelect = document.getElementById("ward");
    if (!wardSelect) return;

    wardSelect.innerHTML = '<option value="">-- Chọn Xã/Phường --</option>';

    if (!wards || wards.length === 0) return;

    wards.forEach((ward) => {
      const option = document.createElement("option");
      option.value = ward.name;
      option.textContent = ward.name;
      option.dataset.code = ward.code;
      wardSelect.appendChild(option);
    });

    console.log(`✅ Populated ${wards.length} wards in dropdown`);
  }
}

// Handle province selection change
async function handleProvinceChange(event) {
  console.log("🔔 handleProvinceChange triggered", event);

  const provinceCodeInput = document.getElementById("provinceCode");

  let provinceName, provinceCode;

  if (provinceChoice) {
    // Get from Choices.js
    const selectedChoice = provinceChoice.getValue(true);
    provinceName = selectedChoice;

    console.log("📍 Selected province:", provinceName);

    // Find province code
    const province = allProvinces.find((p) => p.name === provinceName);
    provinceCode = province ? province.code : "";

    console.log("📍 Province code:", provinceCode);

    // Scroll to selected province in dropdown
    if (provinceCode) {
      setTimeout(() => {
        const selectedItem = provinceChoice._currentState.items.find(
          (item) =>
            item.customProperties && item.customProperties.code === provinceCode
        );
        if (selectedItem) {
          provinceChoice.highlightItem(selectedItem, true);
        }
      }, 100);
    }
  } else {
    // Get from native select
    const provinceSelect = document.getElementById("province");
    if (!provinceSelect) return;

    const selectedOption = provinceSelect.options[provinceSelect.selectedIndex];
    provinceName = selectedOption.value;
    provinceCode = selectedOption.dataset.code;
  }

  // Update hidden input
  if (provinceCodeInput) {
    provinceCodeInput.value = provinceCode || "";
  }

  // Load wards for selected province
  if (provinceCode) {
    console.log("🔄 Loading wards for province:", provinceCode);
    const wards = await loadWardsForProvince(provinceCode);
    console.log("✅ Loaded wards:", wards);
    populateWardSelect(wards);
  } else {
    // Clear ward select
    populateWardSelect([]);
    document.getElementById("wardCode").value = "";
  }
}

// Handle ward selection change
function handleWardChange(event) {
  console.log("🔔 handleWardChange triggered", event);

  const wardCodeInput = document.getElementById("wardCode");

  let wardName, wardCode;

  if (wardChoice) {
    // Get from Choices.js
    const selectedChoice = wardChoice.getValue(true);
    wardName = selectedChoice;

    console.log("📍 Selected ward:", wardName);

    // Find ward code from all loaded wards
    const currentWards = wardChoice._currentState.choices.filter(
      (c) => c.value === wardName
    );

    if (currentWards.length > 0 && currentWards[0].customProperties) {
      wardCode = currentWards[0].customProperties.code;
      console.log("📍 Ward code:", wardCode);
    }

    // Scroll to selected ward in dropdown
    if (wardCode) {
      setTimeout(() => {
        const selectedItem = wardChoice._currentState.items.find(
          (item) =>
            item.customProperties && item.customProperties.code === wardCode
        );
        if (selectedItem) {
          wardChoice.highlightItem(selectedItem, true);
        }
      }, 100);
    }
  } else {
    // Get from native select
    const wardSelect = document.getElementById("ward");
    if (!wardSelect) return;

    const selectedOption = wardSelect.options[wardSelect.selectedIndex];
    wardName = selectedOption.value;
    wardCode = selectedOption.dataset.code;
  }

  // Update hidden input
  if (wardCodeInput) {
    wardCodeInput.value = wardCode || "";
    console.log("✅ Updated wardCode input:", wardCode);
  }
}

// ==================== PROFILE FUNCTIONS ====================

// Load user profile data
async function loadProfile() {
  console.log("Loading profile...");

  try {
    const response = await fetchAPI(`${API_BASE_URL}/user/me`);

    if (!response) {
      console.error("No response from API");
      return;
    }

    const result = await response.json();
    console.log("Profile API Response:", result);

    if (result.success && result.data) {
      const user = result.data;
      console.log("User data:", user);

      // Update sidebar user info
      const userNameEl = document.getElementById("userName");
      const userEmailEl = document.getElementById("userEmail");

      if (userNameEl) {
        userNameEl.textContent = user.fullName || user.email.split("@")[0];
      }
      if (userEmailEl) {
        userEmailEl.textContent = user.email;
      }

      // Update form fields
      const fullNameInput = document.getElementById("fullName");
      const phoneNumberInput = document.getElementById("phoneNumber");
      const emailInput = document.getElementById("email");

      if (fullNameInput) fullNameInput.value = user.fullName || "";
      if (phoneNumberInput) phoneNumberInput.value = user.phoneNumber || "";
      if (emailInput) emailInput.value = user.email || "";

      console.log("✅ Profile loaded successfully");
    } else {
      console.error("❌ API error:", result.message || "Unknown error");
      showMessage(
        "profileMessage",
        result.message || "Không thể tải thông tin",
        false
      );
    }
  } catch (error) {
    console.error("❌ Error loading profile:", error);
    showMessage("profileMessage", "Lỗi khi tải thông tin người dùng", false);
  }
}

// Update profile
async function updateProfile(formData) {
  try {
    const response = await fetchAPI(`${API_BASE_URL}/user/profile`, {
      method: "POST",
      body: JSON.stringify(formData),
    });

    if (!response) return;

    const result = await response.json();
    console.log("Update profile response:", result);

    if (result.success) {
      showMessage(
        "profileMessage",
        "Cập nhật thông tin thành công! Đang tải lại trang...",
        true
      );

      // Reload page after 1 second to ensure data consistency
      setTimeout(() => {
        window.location.reload();
      }, 1000);
    } else {
      showMessage(
        "profileMessage",
        result.message || "Cập nhật thất bại",
        false
      );
    }
  } catch (error) {
    console.error("Error updating profile:", error);
    showMessage("profileMessage", "Có lỗi xảy ra khi cập nhật", false);
  }
}

// Change password
async function changePassword(formData) {
  try {
    const response = await fetchAPI(`${API_BASE_URL}/user/password`, {
      method: "POST",
      body: JSON.stringify(formData),
    });

    if (!response) return;

    const result = await response.json();
    console.log("Change password response:", result);

    if (result.success) {
      showMessage("passwordMessage", "Đổi mật khẩu thành công!", true);
      // Clear password fields
      document.getElementById("changePasswordForm").reset();
    } else {
      showMessage(
        "passwordMessage",
        result.message || "Đổi mật khẩu thất bại",
        false
      );
    }
  } catch (error) {
    console.error("Error changing password:", error);
    showMessage("passwordMessage", "Có lỗi xảy ra khi đổi mật khẩu", false);
  }
}

// ==================== ADDRESS FUNCTIONS ====================

// Load addresses
async function loadAddresses() {
  console.log("Loading addresses...");

  try {
    const response = await fetchAPI(`${API_BASE_URL}/user/addresses`);

    if (!response) return;

    const result = await response.json();
    console.log("Addresses response:", result);

    const addressList = document.getElementById("addressList");
    if (!addressList) return;

    if (result.success && result.data && result.data.length > 0) {
      // Sort addresses: Default address first, then others
      const sortedAddresses = result.data.sort((a, b) => {
        if (a.isDefault && !b.isDefault) return -1;
        if (!a.isDefault && b.isDefault) return 1;
        return 0;
      });

      addressList.innerHTML = sortedAddresses
        .map(
          (address) => `
        <div class="address-card ${address.isDefault ? "default" : ""}">
          ${
            address.isDefault
              ? '<span class="default-badge">Mặc định</span>'
              : ""
          }
          <h4>${address.recipientName}</h4>
          <p><i class='bx bx-phone'></i> ${address.phoneNumber}</p>
          <p><i class='bx bx-map'></i> ${address.streetAddress}, ${
            address.ward ? address.ward : "N/A"
          }, ${address.province ? address.province : "N/A"}</p>
          <div class="address-actions">
            <button class="btn btn-sm btn-primary" onclick="editAddress(${
              address.id
            })">
              <i class='bx bx-edit'></i> Sửa
            </button>
            ${
              !address.isDefault
                ? `<button class="btn btn-sm btn-danger" onclick="deleteAddress(${address.id})">
                <i class='bx bx-trash'></i> Xóa
              </button>`
                : ""
            }
          </div>
        </div>
      `
        )
        .join("");
    } else {
      addressList.innerHTML =
        '<p class="no-data">Chưa có địa chỉ nào. Vui lòng thêm địa chỉ giao hàng.</p>';
    }
  } catch (error) {
    console.error("Error loading addresses:", error);
    const addressList = document.getElementById("addressList");
    if (addressList) {
      addressList.innerHTML =
        '<p class="error">Không thể tải danh sách địa chỉ</p>';
    }
  }
}

// Add address
async function addAddress(formData) {
  try {
    const response = await fetchAPI(`${API_BASE_URL}/user/addresses`, {
      method: "POST",
      body: JSON.stringify(formData),
    });

    if (!response) return;

    const result = await response.json();
    console.log("Add address response:", result);

    if (result.success) {
      showMessage("addressFormMessage", "Thêm địa chỉ thành công!", true);
      closeModal();
      await loadAddresses();
    } else {
      showMessage(
        "addressFormMessage",
        result.message || "Thêm địa chỉ thất bại",
        false
      );
    }
  } catch (error) {
    console.error("Error adding address:", error);
    showMessage("addressFormMessage", "Có lỗi xảy ra khi thêm địa chỉ", false);
  }
}

// Update address
async function updateAddress(id, formData) {
  try {
    const response = await fetchAPI(`${API_BASE_URL}/user/addresses/${id}`, {
      method: "PUT",
      body: JSON.stringify(formData),
    });

    if (!response) return;

    const result = await response.json();
    console.log("Update address response:", result);

    if (result.success) {
      showMessage("addressFormMessage", "Cập nhật địa chỉ thành công!", true);
      closeModal();
      await loadAddresses();
    } else {
      showMessage(
        "addressFormMessage",
        result.message || "Cập nhật địa chỉ thất bại",
        false
      );
    }
  } catch (error) {
    console.error("Error updating address:", error);
    showMessage(
      "addressFormMessage",
      "Có lỗi xảy ra khi cập nhật địa chỉ",
      false
    );
  }
}

// Delete address
async function deleteAddress(id) {
  if (!confirm("Bạn có chắc chắn muốn xóa địa chỉ này?")) {
    return;
  }

  try {
    const response = await fetchAPI(`${API_BASE_URL}/user/addresses/${id}`, {
      method: "DELETE",
    });

    if (!response) return;

    const result = await response.json();
    console.log("Delete address response:", result);

    if (result.success) {
      await loadAddresses();
    } else {
      alert(result.message || "Xóa địa chỉ thất bại");
    }
  } catch (error) {
    console.error("Error deleting address:", error);
    alert("Có lỗi xảy ra khi xóa địa chỉ");
  }
}

// Edit address
async function editAddress(id) {
  try {
    const response = await fetchAPI(`${API_BASE_URL}/user/addresses`);

    if (!response) return;

    const result = await response.json();

    if (result.success && result.data) {
      const address = result.data.find((a) => a.id === id);
      if (address) {
        // Fill form with address data
        document.getElementById("addressId").value = address.id;
        document.getElementById("recipientName").value = address.recipientName;
        document.getElementById("recipientPhone").value = address.phoneNumber;
        document.getElementById("streetAddress").value = address.streetAddress;
        document.getElementById("isDefault").checked = address.isDefault;

        // Store the address data to set after modal opens
        window.editingAddress = address;

        // Update modal title
        document.getElementById("modalTitle").textContent = "Sửa địa chỉ";

        // Show modal - will trigger province/ward population
        openModal();
      }
    }
  } catch (error) {
    console.error("Error loading address for edit:", error);
  }
}

// ==================== MODAL FUNCTIONS ====================

function openModal() {
  const modal = document.getElementById("addressModal");
  if (modal) {
    modal.classList.add("show");
    modal.style.display = "flex";

    // Load provinces when modal opens
    if (allProvinces.length === 0) {
      console.log("Loading provinces for address modal...");
      loadProvinces().then(() => {
        // After provinces loaded, set editing address if exists
        setEditingAddress();
      });
    } else {
      console.log("Provinces already loaded:", allProvinces.length);
      populateProvinceSelect();
      // Set editing address after populating
      setEditingAddress();
    }
  }
}

/**
 * Set province and ward for editing address
 */
async function setEditingAddress() {
  if (window.editingAddress) {
    const address = window.editingAddress;
    console.log("🔧 Setting editing address:", address);

    // Set province using Choices.js by finding province with matching code
    if (address.provinceCode && provinceChoice) {
      // Find province name from code
      const province = allProvinces.find((p) => p.code == address.provinceCode);
      if (province) {
        console.log(
          "🔧 Setting province:",
          province.name,
          "code:",
          province.code
        );
        provinceChoice.setChoiceByValue(province.name);
        document.getElementById("provinceCode").value = address.provinceCode;

        // Load and set wards
        console.log("🔄 Loading wards for province:", address.provinceCode);
        const wards = await loadWardsForProvince(address.provinceCode);
        populateWardSelect(wards);

        // Set ward after wards are populated
        if (address.wardCode && wardChoice) {
          setTimeout(() => {
            // Find ward name from code
            const ward = wards.find((w) => w.code == address.wardCode);
            if (ward) {
              console.log("🔧 Setting ward:", ward.name, "code:", ward.code);
              wardChoice.setChoiceByValue(ward.name);
              document.getElementById("wardCode").value = address.wardCode;
            } else {
              console.warn("⚠️ Ward not found with code:", address.wardCode);
            }
          }, 300);
        }
      } else {
        console.warn("⚠️ Province not found with code:", address.provinceCode);
      }
    }

    // Clear the editing address
    window.editingAddress = null;
    console.log("✅ Editing address populated and cleared");
  }
}

function closeModal() {
  const modal = document.getElementById("addressModal");
  if (modal) {
    modal.classList.remove("show");
    modal.style.display = "none";
    document.getElementById("addressForm").reset();
    document.getElementById("addressId").value = "";
    document.getElementById("modalTitle").textContent = "Thêm địa chỉ mới";

    // Clear any error messages
    const messageEl = document.getElementById("addressFormMessage");
    if (messageEl) {
      messageEl.style.display = "none";
    }
  }
}

// ==================== EVENT LISTENERS ====================

document.addEventListener("DOMContentLoaded", function () {
  console.log("Profile page initialized");

  // Check authentication
  if (!checkAuth()) {
    return;
  }

  // Initialize Choices.js for searchable selects
  initializeChoices();

  // Load profile data and locations
  loadProfile();
  loadProvinces();

  // Tab switching function
  function switchToTab(tabName) {
    console.log("Switching to tab:", tabName);

    const navItems = document.querySelectorAll(".nav-item");
    const tabContents = document.querySelectorAll(".tab-content");

    // Update active nav item
    navItems.forEach((nav) => {
      if (nav.dataset.tab === tabName) {
        nav.classList.add("active");
      } else {
        nav.classList.remove("active");
      }
    });

    // Update active tab content
    tabContents.forEach((content) => content.classList.remove("active"));
    const activeTab = document.getElementById("tab-" + tabName);
    if (activeTab) {
      activeTab.classList.add("active");
    }

    // Load data for specific tabs
    if (tabName === "addresses") {
      loadAddresses();
    } else if (tabName === "orders") {
      // Load orders when switching to orders tab
      if (typeof loadOrders === "function") {
        loadOrders();
      }
    }

    // Update URL hash without scrolling
    history.replaceState(null, null, "#" + tabName);
  }

  // Tab switching via sidebar clicks
  const navItems = document.querySelectorAll(".nav-item");
  navItems.forEach((item) => {
    item.addEventListener("click", function (e) {
      e.preventDefault();
      const tab = this.dataset.tab;
      switchToTab(tab);
    });
  });

  // Handle hash navigation on page load
  function handleHashNavigation() {
    const hash = window.location.hash.substring(1); // Remove #
    const validTabs = ["info", "orders", "addresses", "password"];
    
    if (hash && validTabs.includes(hash)) {
      switchToTab(hash);
    } else {
      // Default to info tab if no hash or invalid hash
      switchToTab("info");
    }
  }

  // Handle hash changes (browser back/forward)
  window.addEventListener("hashchange", handleHashNavigation);

  // Handle initial hash on page load
  handleHashNavigation();

  // Update profile form
  const updateProfileForm = document.getElementById("updateProfileForm");
  if (updateProfileForm) {
    updateProfileForm.addEventListener("submit", async function (e) {
      e.preventDefault();

      const formData = {
        fullName: document.getElementById("fullName").value,
        phoneNumber: document.getElementById("phoneNumber").value,
      };

      console.log("Updating profile with data:", formData);
      await updateProfile(formData);
    });
  }

  // Change password form
  const changePasswordForm = document.getElementById("changePasswordForm");
  if (changePasswordForm) {
    changePasswordForm.addEventListener("submit", async function (e) {
      e.preventDefault();

      const oldPassword = document.getElementById("oldPassword").value;
      const newPassword = document.getElementById("newPassword").value;
      const confirmPassword = document.getElementById(
        "profileConfirmPassword"
      ).value;

      // Validate passwords
      if (newPassword !== confirmPassword) {
        showMessage("passwordMessage", "Mật khẩu xác nhận không khớp!", false);
        return;
      }

      if (newPassword.length < 6) {
        showMessage(
          "passwordMessage",
          "Mật khẩu phải có ít nhất 6 ký tự!",
          false
        );
        return;
      }

      const formData = {
        oldPassword,
        newPassword,
      };

      console.log("Changing password...");
      await changePassword(formData);
    });
  }

  // Add address button
  const btnAddAddress = document.getElementById("btnAddAddress");
  if (btnAddAddress) {
    btnAddAddress.addEventListener("click", function () {
      document.getElementById("addressForm").reset();
      document.getElementById("addressId").value = "";
      document.getElementById("modalTitle").textContent = "Thêm địa chỉ mới";
      openModal();
    });
  }

  // Address form submit
  const addressForm = document.getElementById("addressForm");
  if (addressForm) {
    addressForm.addEventListener("submit", async function (e) {
      e.preventDefault();

      const addressId = document.getElementById("addressId").value;
      const formData = {
        recipientName: document.getElementById("recipientName").value,
        phoneNumber: document.getElementById("recipientPhone").value,
        streetAddress: document.getElementById("streetAddress").value,
        province: document.getElementById("province").value,
        provinceCode: document.getElementById("provinceCode").value,
        ward: document.getElementById("ward").value,
        wardCode: document.getElementById("wardCode").value,
        isDefault: document.getElementById("isDefault").checked,
      };

      console.log("Address form data:", formData, "ID:", addressId);

      if (addressId) {
        // Update existing address
        await updateAddress(addressId, formData);
      } else {
        // Add new address
        await addAddress(formData);
      }
    });
  }

  // Close modal
  const closeBtn = document.querySelector(".close");
  if (closeBtn) {
    closeBtn.addEventListener("click", closeModal);
  }

  // Close modal when clicking outside
  const modal = document.getElementById("addressModal");
  if (modal) {
    modal.addEventListener("click", function (e) {
      if (e.target === modal) {
        closeModal();
      }
    });
  }

  // Add event listeners for province and ward dropdowns
  const provinceSelect = document.getElementById("province");
  const wardSelect = document.getElementById("ward");

  if (provinceSelect) {
    provinceSelect.addEventListener("change", handleProvinceChange);
    console.log("✅ Province select event listener added");
  }

  if (wardSelect) {
    wardSelect.addEventListener("change", handleWardChange);
    console.log("✅ Ward select event listener added");
  }
});

// Make functions globally available
window.editAddress = editAddress;
window.deleteAddress = deleteAddress;

console.log("✅ Profile.js loaded successfully");
