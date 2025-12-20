/* UTE Phone Hub - Main JavaScript */

// Global variables
let cart = JSON.parse(localStorage.getItem("cart")) || [];
let wishlist = JSON.parse(localStorage.getItem("wishlist")) || [];

// DOM Content Loaded
document.addEventListener("DOMContentLoaded", function () {
  initializeApp();
});

// Initialize Application
function initializeApp() {
  initializeHeader();
  initializeUserAccount(); // Add user account dropdown behavior
  
  // Update cart and voucher badges for logged in users
  if (isLoggedIn()) {
    updateCartBadge();
    updateVoucherBadge();
  }
  
  // Only initialize if elements exist on page
  if (document.querySelector('.product-grid')) {
    initializeProductGrid();
  }
  
  if (document.querySelector('.cart-badge')) {
    initializeCart();
  }
  
  if (document.querySelector('.wishlist-badge')) {
    initializeWishlist();
    updateWishlistBadge();
  }
  
  if (document.querySelector('.filter-option')) {
    initializeFilters();
  }
  
  if (document.querySelector('.pagination-btn')) {
    initializePagination();
  }
}

// Header Functions
function initializeHeader() {
  const mobileMenuBtn = document.querySelector(".mobile-menu-btn");
  const mobileMenuClose = document.getElementById("mobileMenuClose");
  const mainNav = document.getElementById("mainNav");
  const searchInput = document.querySelector(".search-input");
  const searchSuggestions = document.querySelector(".search-suggestions");

  // Mobile menu toggle - open
  if (mobileMenuBtn && mainNav) {
    mobileMenuBtn.addEventListener("click", function (e) {
      e.stopPropagation();
      mainNav.classList.add("mobile-active");
      document.body.style.overflow = "hidden"; // Prevent body scroll
    });
  }

  // Mobile menu toggle - close
  if (mobileMenuClose && mainNav) {
    mobileMenuClose.addEventListener("click", function (e) {
      e.stopPropagation();
      mainNav.classList.remove("mobile-active");
      document.body.style.overflow = ""; // Restore body scroll
    });
  }

  // Close mobile menu when clicking on nav link
  if (mainNav) {
    const navLinks = mainNav.querySelectorAll(".nav-link");
    navLinks.forEach(link => {
      link.addEventListener("click", function () {
        if (mainNav.classList.contains("mobile-active")) {
          mainNav.classList.remove("mobile-active");
          document.body.style.overflow = "";
        }
      });
    });
  }

  // Search functionality
  if (searchInput && searchSuggestions) {
    searchInput.addEventListener("input", function () {
      const query = this.value.trim();
      if (query.length > 2) {
        showSearchSuggestions(query);
      } else {
        hideSearchSuggestions();
      }
    });

    searchInput.addEventListener("focus", function () {
      if (this.value.trim().length > 2) {
        showSearchSuggestions(this.value.trim());
      }
    });

    document.addEventListener("click", function (e) {
      if (
        !searchInput.contains(e.target) &&
        !searchSuggestions.contains(e.target)
      ) {
        hideSearchSuggestions();
      }
    });
  }
}

// Search Functions
function showSearchSuggestions(query) {
  const searchSuggestions = document.querySelector(".search-suggestions");
  if (!searchSuggestions) return;

  // Mock search suggestions (replace with actual API call)
  const suggestions = [
    "iPhone 15 Pro Max",
    "Samsung Galaxy S24 Ultra",
    "MacBook Pro M3",
    "iPad Air",
    "AirPods Pro",
    "Apple Watch Series 9",
    "Sony WH-1000XM5",
    "Dell XPS 13",
    "Surface Pro 9",
    "Google Pixel 8 Pro",
  ].filter((item) => item.toLowerCase().includes(query.toLowerCase()));

  if (suggestions.length > 0) {
    searchSuggestions.innerHTML = suggestions
      .map(
        (item) =>
          `<div class="suggestion-item" data-query="${item}">${item}</div>`
      )
      .join("");

    searchSuggestions.style.display = "block";

    // Add click handlers to suggestions
    searchSuggestions.querySelectorAll(".suggestion-item").forEach((item) => {
      item.addEventListener("click", function () {
        const query = this.dataset.query;
        performSearch(query);
        hideSearchSuggestions();
      });
    });
  } else {
    hideSearchSuggestions();
  }
}

function hideSearchSuggestions() {
  const searchSuggestions = document.querySelector(".search-suggestions");
  if (searchSuggestions) {
    searchSuggestions.style.display = "none";
  }
}

function performSearch(query) {
  console.log("Searching for:", query);
  // Implement actual search functionality
  // This could redirect to a search results page or filter current products
  window.location.href = `/search?q=${encodeURIComponent(query)}`;
}

// Product Grid Functions
function initializeProductGrid() {
  const productCards = document.querySelectorAll(".product-card");

  productCards.forEach((card) => {
    // Add to cart functionality
    const addToCartBtn = card.querySelector(".btn-add-cart");
    if (addToCartBtn) {
      addToCartBtn.addEventListener("click", function (e) {
        e.preventDefault();
        e.stopPropagation();
        addToCart(card);
      });
    }

    // Wishlist functionality
    const wishlistBtn = card.querySelector(".product-wishlist");
    if (wishlistBtn) {
      wishlistBtn.addEventListener("click", function (e) {
        e.preventDefault();
        e.stopPropagation();
        toggleWishlist(card);
      });
    }

    // Quick view functionality
    const quickViewBtn = card.querySelector(".btn-quick-view");
    if (quickViewBtn) {
      quickViewBtn.addEventListener("click", function (e) {
        e.preventDefault();
        e.stopPropagation();
        showQuickView(card);
      });
    }

    // Card click to view product details
    card.addEventListener("click", function (e) {
      if (
        !e.target.closest(".btn-add-cart") &&
        !e.target.closest(".product-wishlist") &&
        !e.target.closest(".btn-quick-view")
      ) {
        viewProductDetails(card);
      }
    });
  });
}

// Cart Functions
function initializeCart() {
  updateCartDisplay();
}

function addToCart(productCard) {
  const productId =
    productCard.dataset.productId || generateProductId(productCard);
  const productName = productCard.querySelector(".product-title").textContent;
  const productPrice = parseFloat(
    productCard
      .querySelector(".price-current")
      .textContent.replace(/[^\d]/g, "")
  );
  const productImage = productCard.querySelector(".product-image").src;

  const existingItem = cart.find((item) => item.id === productId);

  if (existingItem) {
    existingItem.quantity += 1;
  } else {
    cart.push({
      id: productId,
      name: productName,
      price: productPrice,
      image: productImage,
      quantity: 1,
    });
  }

  saveCart();
  updateCartBadge();
  updateCartDisplay();
  showAddToCartAnimation(productCard);

  // Show success message
  showNotification("Đã thêm vào giỏ hàng!", "success");
}

function removeFromCart(productId) {
  cart = cart.filter((item) => item.id !== productId);
  saveCart();
  updateCartBadge();
  updateCartDisplay();
  showNotification("Đã xóa khỏi giỏ hàng!", "info");
}

function updateCartQuantity(productId, quantity) {
  const item = cart.find((item) => item.id === productId);
  if (item) {
    if (quantity <= 0) {
      removeFromCart(productId);
    } else {
      item.quantity = quantity;
      saveCart();
      updateCartBadge();
      updateCartDisplay();
    }
  }
}

function clearCart() {
  cart = [];
  saveCart();
  updateCartBadge();
  updateCartDisplay();
  showNotification("Đã xóa tất cả sản phẩm!", "info");
}

function saveCart() {
  localStorage.setItem("cart", JSON.stringify(cart));
}

/**
 * Update cart badge from API
 */
async function updateCartBadge() {
  const badge = document.getElementById('cartBadge');
  if (!badge) return;
  
  // If not logged in, hide badge
  if (!isLoggedIn()) {
    badge.style.display = 'none';
    return;
  }
  
  try {
    const response = await CartAPI.getCart();
    if (response && response.success && response.data) {
      const totalItems = response.data.totalItems || 0;
      badge.textContent = totalItems;
      badge.style.display = totalItems > 0 ? 'flex' : 'none';
    }
  } catch (error) {
    console.error('Error fetching cart:', error);
    // Hide badge on error
    badge.style.display = 'none';
  }
}

/**
 * Update voucher badge from API
 */
async function updateVoucherBadge() {
  const badge = document.getElementById('voucherBadge');
  if (!badge) return;
  
  // If not logged in, hide badge
  if (!isLoggedIn()) {
    badge.style.display = 'none';
    return;
  }
  
  try {
    const response = await VoucherAPI.getUserVouchers(1, 100);
    if (response && response.success && response.data) {
      const totalVouchers = response.data.length || 0;
      badge.textContent = totalVouchers;
      badge.style.display = totalVouchers > 0 ? 'flex' : 'none';
    }
  } catch (error) {
    console.error('Error fetching vouchers:', error);
    // Hide badge on error
    badge.style.display = 'none';
  }
}

function updateCartDisplay() {
  const cartContainer = document.querySelector(".cart-items");
  if (!cartContainer) return;

  if (cart.length === 0) {
    cartContainer.innerHTML = '<div class="empty-cart">Giỏ hàng trống</div>';
    return;
  }

  cartContainer.innerHTML = cart
    .map(
      (item) => `
        <div class="cart-item" data-product-id="${item.id}">
            <img src="${item.image}" alt="${item.name}" class="cart-item-image">
            <div class="cart-item-info">
                <h4 class="cart-item-name">${item.name}</h4>
                <p class="cart-item-price">${formatPrice(item.price)}</p>
                <div class="cart-item-controls">
                    <button class="btn-quantity" onclick="updateCartQuantity('${
                      item.id
                    }', ${item.quantity - 1})">-</button>
                    <span class="quantity">${item.quantity}</span>
                    <button class="btn-quantity" onclick="updateCartQuantity('${
                      item.id
                    }', ${item.quantity + 1})">+</button>
                    <button class="btn-remove" onclick="removeFromCart('${
                      item.id
                    }')">×</button>
                </div>
            </div>
        </div>
    `
    )
    .join("");
}

// Wishlist Functions
function initializeWishlist() {
  updateWishlistDisplay();
}

function toggleWishlist(productCard) {
  const productId =
    productCard.dataset.productId || generateProductId(productCard);
  const productName = productCard.querySelector(".product-title").textContent;
  const productPrice = parseFloat(
    productCard
      .querySelector(".price-current")
      .textContent.replace(/[^\d]/g, "")
  );
  const productImage = productCard.querySelector(".product-image").src;

  const existingItem = wishlist.find((item) => item.id === productId);

  if (existingItem) {
    wishlist = wishlist.filter((item) => item.id !== productId);
    productCard.querySelector(".product-wishlist").classList.remove("active");
    showNotification("Đã xóa khỏi yêu thích!", "info");
  } else {
    wishlist.push({
      id: productId,
      name: productName,
      price: productPrice,
      image: productImage,
    });
    productCard.querySelector(".product-wishlist").classList.add("active");
    showNotification("Đã thêm vào yêu thích!", "success");
  }

  saveWishlist();
  updateWishlistBadge();
  updateWishlistDisplay();
}

function removeFromWishlist(productId) {
  wishlist = wishlist.filter((item) => item.id !== productId);
  saveWishlist();
  updateWishlistBadge();
  updateWishlistDisplay();
  showNotification("Đã xóa khỏi yêu thích!", "info");
}

function saveWishlist() {
  localStorage.setItem("wishlist", JSON.stringify(wishlist));
}

function updateWishlistBadge() {
  const badge = document.querySelector(".wishlist-badge");
  if (badge) {
    badge.textContent = wishlist.length;
    badge.style.display = wishlist.length > 0 ? "flex" : "none";
  }
}

function updateWishlistDisplay() {
  const wishlistContainer = document.querySelector(".wishlist-items");
  if (!wishlistContainer) return;

  if (wishlist.length === 0) {
    wishlistContainer.innerHTML =
      '<div class="empty-wishlist">Danh sách yêu thích trống</div>';
    return;
  }

  wishlistContainer.innerHTML = wishlist
    .map(
      (item) => `
        <div class="wishlist-item" data-product-id="${item.id}">
            <img src="${item.image}" alt="${
        item.name
      }" class="wishlist-item-image">
            <div class="wishlist-item-info">
                <h4 class="wishlist-item-name">${item.name}</h4>
                <p class="wishlist-item-price">${formatPrice(item.price)}</p>
                <button class="btn-remove" onclick="removeFromWishlist('${
                  item.id
                }')">×</button>
            </div>
        </div>
    `
    )
    .join("");
}

// Filter Functions
function initializeFilters() {
  const filterOptions = document.querySelectorAll(".filter-option");

  filterOptions.forEach((option) => {
    option.addEventListener("click", function () {
      this.classList.toggle("active");
      applyFilters();
    });
  });
}

function applyFilters() {
  const activeFilters = Array.from(
    document.querySelectorAll(".filter-option.active")
  ).map((option) => option.textContent.trim());

  console.log("Active filters:", activeFilters);
  // Implement actual filtering logic
  filterProducts(activeFilters);
}

function filterProducts(filters) {
  const productCards = document.querySelectorAll(".product-card");

  productCards.forEach((card) => {
    let shouldShow = true;

    // Apply filter logic here
    // This is a simplified example
    if (filters.length > 0) {
      const productText = card.textContent.toLowerCase();
      shouldShow = filters.some((filter) =>
        productText.includes(filter.toLowerCase())
      );
    }

    card.style.display = shouldShow ? "block" : "none";
  });
}

// Pagination Functions
function initializePagination() {
  const paginationBtns = document.querySelectorAll(".pagination-btn");

  paginationBtns.forEach((btn) => {
    btn.addEventListener("click", function () {
      if (!this.disabled) {
        const page = this.dataset.page;
        goToPage(page);
      }
    });
  });
}

function goToPage(page) {
  console.log("Going to page:", page);
  // Implement pagination logic
  // This could load new products or scroll to top
  window.scrollTo({ top: 0, behavior: "smooth" });
}

// Utility Functions
function generateProductId(productCard) {
  const title = productCard.querySelector(".product-title").textContent;
  return title
    .toLowerCase()
    .replace(/\s+/g, "-")
    .replace(/[^\w-]/g, "");
}

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(price);
}

function showAddToCartAnimation(productCard) {
  const btn = productCard.querySelector(".btn-add-cart");
  const originalText = btn.textContent;

  btn.classList.add("loading");
  btn.textContent = "";

  setTimeout(() => {
    btn.classList.remove("loading");
    btn.textContent = originalText;
  }, 1000);
}

function showQuickView(productCard) {
  const productId =
    productCard.dataset.productId || generateProductId(productCard);
  console.log("Quick view for product:", productId);
  // Implement quick view modal
  showNotification("Tính năng xem nhanh đang phát triển!", "info");
}

function viewProductDetails(productCard) {
  const productId =
    productCard.dataset.productId || generateProductId(productCard);
  console.log("View details for product:", productId);
  // Navigate to product details page
  window.location.href = `/product/${productId}`;
}

function showNotification(message, type = "info") {
  // Create notification element
  const notification = document.createElement("div");
  notification.className = `notification notification-${type}`;
  notification.textContent = message;

  // Add styles
  notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${
          type === "success"
            ? "#28a745"
            : type === "error"
            ? "#dc3545"
            : "#17a2b8"
        };
        color: white;
        padding: 15px 20px;
        border-radius: 8px;
        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
        z-index: 10000;
        animation: slideInRight 0.3s ease-out;
    `;

  document.body.appendChild(notification);

  // Remove after 3 seconds
  setTimeout(() => {
    notification.style.animation = "slideOutRight 0.3s ease-in";
    setTimeout(() => {
      if (notification.parentNode) {
        notification.parentNode.removeChild(notification);
      }
    }, 300);
  }, 3000);
}

// Lazy Loading for Images
function initializeLazyLoading() {
  const images = document.querySelectorAll("img[data-src]");

  const imageObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        const img = entry.target;
        img.src = img.dataset.src;
        img.classList.remove("lazy");
        imageObserver.unobserve(img);
      }
    });
  });

  images.forEach((img) => imageObserver.observe(img));
}

// Smooth Scrolling
function initializeSmoothScrolling() {
  const links = document.querySelectorAll('a[href^="#"]');

  links.forEach((link) => {
    link.addEventListener("click", function (e) {
      const href = this.getAttribute("href");
      
      // Skip if href is just "#" or empty
      if (!href || href === "#" || href.length <= 1) {
        e.preventDefault();
        return;
      }
      
      e.preventDefault();
      const target = document.querySelector(href);
      if (target) {
        target.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
      }
    });
  });
}

// Initialize additional features
document.addEventListener("DOMContentLoaded", function () {
  initializeLazyLoading();
  initializeSmoothScrolling();
  setupAutoRefreshToken();
});

// ============================================
// AUTO REFRESH TOKEN INTERCEPTOR
// ============================================

/**
 * Setup auto refresh token when access token expires
 */
function setupAutoRefreshToken() {
  // Override fetch to intercept 401 errors
  const originalFetch = window.fetch;

  window.fetch = async function (...args) {
    let [url, options] = args;

    // Skip auto-refresh for refresh token endpoint to avoid infinite loop
    if (url && url.includes("/auth/refresh")) {
      return originalFetch(...args);
    }

    // Add access token if not already present
    const token = localStorage.getItem("accessToken");
    if (token && options && !options.headers) {
      options.headers = {};
    }
    if (
      token &&
      options &&
      options.headers &&
      !options.headers["Authorization"]
    ) {
      options.headers["Authorization"] = `Bearer ${token}`;
    }

    // Make request
    let response = await originalFetch(...args);

    // If 401 and we have a refresh token, try to refresh
    if (response.status === 401 && localStorage.getItem("refreshToken")) {
      console.log("Access token expired, refreshing...");

      const newToken = await refreshAccessToken();

      if (newToken) {
        // Retry request with new token
        if (options && options.headers) {
          options.headers["Authorization"] = `Bearer ${newToken}`;
        }
        response = await originalFetch(...args);
      }
    }

    return response;
  };
}

/**
 * Refresh access token
 */
async function refreshAccessToken() {
  const refreshToken = localStorage.getItem("refreshToken");

  if (!refreshToken) {
    console.error("No refresh token found");
    redirectToLogin();
    return null;
  }

  try {
    const response = await fetch("/api/v1/auth/refresh", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken: refreshToken }),
    });

    const data = await response.json();

    if (data.success && data.data.accessToken) {
      // Update access token
      localStorage.setItem("accessToken", data.data.accessToken);
      console.log("✅ Access token refreshed successfully");
      return data.data.accessToken;
    } else {
      console.error("❌ Failed to refresh token:", data.message);
      redirectToLogin();
      return null;
    }
  } catch (error) {
    console.error("❌ Error refreshing token:", error);
    redirectToLogin();
    return null;
  }
}

/**
 * Initialize user account dropdown behavior
 * - When NOT logged in: click goes to /login, no dropdown
 * - When logged in: hover shows dropdown, click goes to /profile
 */
function initializeUserAccount() {
  const userAccountBtn = document.getElementById('userAccountBtn');
  const userAccountText = document.getElementById('userAccountText');
  const accountDropdownMenu = document.getElementById('accountDropdownMenu');
  const userAccountDropdown = document.getElementById('userAccountDropdown');
  const logoutBtn = document.getElementById('logoutBtn');
  
  if (!userAccountBtn || !accountDropdownMenu || !userAccountDropdown) {
    return;
  }
  
  const isLoggedIn = !!localStorage.getItem('accessToken');
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  
  if (isLoggedIn && user.fullName) {
    // User is logged in
    userAccountText.textContent = user.fullName;
    userAccountBtn.href = contextPath + '/profile';
    
    // KHÔNG preventDefault - để link hoạt động bình thường
    // Chỉ xử lý hover để show/hide dropdown
    
    // Show dropdown on hover only
    userAccountDropdown.addEventListener('mouseenter', function() {
      accountDropdownMenu.style.display = 'block';
    });
    
    userAccountDropdown.addEventListener('mouseleave', function() {
      accountDropdownMenu.style.display = 'none';
    });
    
    // Logout handler
    if (logoutBtn) {
      logoutBtn.addEventListener('click', function(e) {
        e.preventDefault();
        logout();
      });
    }
  } else {
    // User is NOT logged in
    userAccountText.textContent = 'Đăng nhập';
    userAccountBtn.href = contextPath + '/login';
    accountDropdownMenu.style.display = 'none';
    
    // KHÔNG preventDefault - để link hoạt động bình thường
    // Link sẽ tự động redirect đến /login
    
    // Prevent dropdown on hover when not logged in
    userAccountDropdown.addEventListener('mouseenter', function() {
      accountDropdownMenu.style.display = 'none';
    });
  }
}

/**
 * Redirect to login page
 */
function redirectToLogin() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
  window.location.href =
    "/auth/login?returnUrl=" + encodeURIComponent(window.location.pathname);
}

// Export functions for global access
window.UTEPhoneHub = {
  addToCart,
  removeFromCart,
  updateCartQuantity,
  clearCart,
  toggleWishlist,
  removeFromWishlist,
  showNotification,
  formatPrice,
  refreshAccessToken,
  logout: function () {
    if (typeof logout === "function") {
      logout();
    } else {
      // Fallback logout
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
      window.location.href = "/";
    }
  },
};
