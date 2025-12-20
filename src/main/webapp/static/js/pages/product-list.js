// Product List Page JavaScript
let currentFilters = {
  category: "",
  brand: "",
  minPrice: "",
  maxPrice: "",
  sortBy: "newest",
  page: 1,
  limit: 12,
  q: "",
};

let totalPages = 1;
let categories = [];
let brands = [];

/**
 * Initialize product list page
 */
async function initProductListPage(filters = {}) {
  currentFilters = { ...currentFilters, ...filters };

  // Load brands only (categories removed)
  await loadBrands();

  // Load products
  await loadProducts();

  // Set up event listeners
  setupEventListeners();

  // Update sort select value
  const sortSelect = document.getElementById("sortSelect");
  if (sortSelect) {
    sortSelect.value = currentFilters.sortBy;
  }
}

/**
 * Load categories from API
 */
async function loadCategories() {
  // Category filter removed - navigation handled by header nav
  console.log("Category filter removed");
}

/**
 * Load brands from API
 */
async function loadBrands() {
  try {
    const response = await API.get("/brands");
    if (response && response.success && response.data) {
      brands = response.data;
      renderBrands();
    }
  } catch (error) {
    console.error("Error loading brands:", error);
  }
}

/**
 * Render categories filter
 */
function renderCategories() {
  // Category filter removed - navigation handled by header nav
}

/**
 * Render brands filter - SINGLE SELECT (radio buttons)
 */
function renderBrands() {
  const container = document.getElementById("brandFilter");
  if (!container || !brands || brands.length === 0) return;

  container.innerHTML = brands
    .map(
      (brand) =>
        '<div class="filter-option" data-filter="brand" data-value="' +
        brand.id +
        '">' +
        '<input type="radio" name="brandFilter" id="brand' +
        brand.id +
        '" ' +
        (currentFilters.brand == brand.id ? "checked" : "") +
        ">" +
        '<label for="brand' +
        brand.id +
        '">' +
        escapeHtml(brand.name) +
        "</label>" +
        "</div>"
    )
    .join("");
}

/**
 * Map frontend sortBy values to backend format (field:direction)
 * Note: Backend chỉ hỗ trợ các field: createdAt, price, name, stockQuantity
 */
function mapSortByValue(sortBy) {
  const sortMapping = {
    newest: "createdAt:desc",
    "price-asc": "price:asc",
    "price-desc": "price:desc",
    name: "name:asc",
    rating: "createdAt:desc", // Fallback to newest (chưa có averageRating trong DB)
    popular: "createdAt:desc", // Fallback to newest (chưa có soldQuantity trong DB)
  };

  return sortMapping[sortBy] || "createdAt:desc";
}

/**
 * Load products from API
 */
async function loadProducts() {
  const productGrid = document.getElementById("productGrid");
  const loadingState = document.getElementById("loadingState");
  const emptyState = document.getElementById("emptyState");
  const resultCount = document.getElementById("resultCount");

  if (!productGrid) return;

  try {
    // Show loading
    loadingState.style.display = "block";
    productGrid.style.display = "none";
    emptyState.style.display = "none";

    // Build query params
    const params = {
      page: currentFilters.page,
      limit: currentFilters.limit,
    };

    // Map frontend sortBy to backend format (field:direction)
    if (currentFilters.sortBy) {
      params.sortBy = mapSortByValue(currentFilters.sortBy);
    }

    if (currentFilters.category) params.categoryId = currentFilters.category;
    if (currentFilters.brand) params.brandId = currentFilters.brand;
    if (currentFilters.minPrice) params.minPrice = currentFilters.minPrice;
    if (currentFilters.maxPrice) params.maxPrice = currentFilters.maxPrice;

    // Map 'q' to 'keyword' for backend
    if (currentFilters.q) params.keyword = currentFilters.q;

    // Call API
    const response = await ProductAPI.getProducts(params);

    if (response && response.success && response.data) {
      const products = response.data;
      const metadata = response.metadata;

      // Hide loading
      loadingState.style.display = "none";

      if (products.length === 0) {
        // Show empty state
        emptyState.style.display = "block";
        resultCount.textContent = "Không tìm thấy sản phẩm";
      } else {
        // Render products
        renderProducts(products);
        productGrid.style.display = "grid";

        // Update result count
        if (metadata && metadata.pagination) {
          const { totalItems } = metadata.pagination;
          resultCount.textContent =
            "Hiển thị " + products.length + " / " + totalItems + " sản phẩm";

          // Update totalProducts (for search header)
          const totalProductsEl = document.getElementById("totalProducts");
          if (totalProductsEl) {
            totalProductsEl.textContent = totalItems;
          }

          // Render pagination
          renderPagination(metadata.pagination);
        } else {
          resultCount.textContent = products.length + " sản phẩm";

          // Update totalProducts (for search header)
          const totalProductsEl = document.getElementById("totalProducts");
          if (totalProductsEl) {
            totalProductsEl.textContent = products.length;
          }
        }
      }

      // Update URL
      updateURL();
    } else {
      throw new Error("Invalid response from API");
    }
  } catch (error) {
    console.error("Error loading products:", error);
    loadingState.style.display = "none";
    emptyState.style.display = "block";
    resultCount.textContent = "Lỗi khi tải sản phẩm";
    showToast("Không thể tải danh sách sản phẩm", "error");
  }
}

/**
 * Render products grid
 */
function renderProducts(products) {
  const contextPath =
    window.location.pathname.substring(
      0,
      window.location.pathname.indexOf("/", 1)
    ) || "";
  const productGrid = document.getElementById("productGrid");
  if (!productGrid) return;

  productGrid.innerHTML = products
    .map((product) => {
      let html =
        '<div class="product-card" data-product-id="' + product.id + '">';
      html += '<div class="product-image-container">';
      html += '<a href="' + contextPath + "/products/" + product.id + '">';
      html +=
        '<img src="' +
        escapeHtml(
          product.thumbnailUrl || "https://via.placeholder.com/300x200"
        ) +
        '" ';
      html +=
        'alt="' +
        escapeHtml(product.name) +
        '" class="product-image" loading="lazy" />';
      html += "</a>";

      // Badges
      if (product.discount > 0) {
        html += '<div class="product-badges">';
        html +=
          '<span class="badge badge-sale">-' + product.discount + "%</span>";
        html += "</div>";
      }

      // Add to cart button (thay thế wishlist)
      html +=
        '<button class="product-add-to-cart" onclick="handleQuickAddToCart(' +
        product.id +
        ')" title="Thêm vào giỏ hàng">';
      html += '<i class="fas fa-shopping-cart"></i>';
      html += "</button>";
      html += "</div>";

      // Product info
      html += '<div class="product-info">';
      html +=
        '<div class="product-category">' +
        escapeHtml(product.categoryName || "Sản phẩm") +
        "</div>";
      html += '<h3 class="product-title">';
      html += '<a href="' + contextPath + "/products/" + product.id + '">';
      html += escapeHtml(product.name);
      html += "</a></h3>";

      // Rating
      if (product.averageRating) {
        html += '<div class="product-rating"><div class="rating-stars">';
        for (let star = 1; star <= 5; star++) {
          html +=
            '<i class="fas fa-star ' +
            (star <= product.averageRating ? "star" : "star empty") +
            '"></i>';
        }
        html += "</div>";
        html +=
          '<span class="rating-text">(' +
          product.averageRating.toFixed(1) +
          ")</span>";
        html += "</div>";
      }

      // Price
      html += '<div class="product-price">';
      html +=
        '<span class="price-current">' + formatPrice(product.price) + "</span>";
      if (product.originalPrice && product.originalPrice > product.price) {
        html +=
          '<span class="price-original">' +
          formatPrice(product.originalPrice) +
          "</span>";
        html +=
          '<span class="discount-percent">-' +
          calculateDiscount(product.originalPrice, product.price) +
          "%</span>";
      }
      html += "</div>";

      // Stock
      if (product.stockQuantity !== undefined) {
        const stockPercent = Math.min(100, (product.stockQuantity / 10) * 100);
        html += '<div class="product-stock">';
        html += '<div class="stock-bar">';
        html +=
          '<div class="stock-progress" style="width: ' +
          stockPercent +
          '%"></div>';
        html += "</div>";
        html +=
          '<div class="stock-text">Còn ' +
          product.stockQuantity +
          " sản phẩm</div>";
        html += "</div>";
      }

      // Actions
      html += '<div class="product-actions">';
      html +=
        '<button class="btn-add-cart" onclick="handleBuyNow(' +
        product.id +
        ')">Mua ngay</button>';
      html +=
        '<button class="btn-quick-view" onclick="viewProductDetail(' +
        product.id +
        ')" title="Xem chi tiết">';
      html += '<i class="fas fa-eye"></i>';
      html += "</button>";
      html += "</div>";

      html += "</div></div>";
      return html;
    })
    .join("");
}

/**
 * Render pagination
 */
function renderPagination(pagination) {
  const { page, totalPages: total } = pagination;
  totalPages = total;

  const paginationEl = document.getElementById("pagination");
  if (!paginationEl || totalPages <= 1) {
    paginationEl.style.display = "none";
    return;
  }

  paginationEl.style.display = "flex";

  let html = "";

  // Previous button
  html +=
    '<button class="pagination-btn" onclick="goToPage(' +
    (page - 1) +
    ')" ' +
    (page === 1 ? "disabled" : "") +
    ">";
  html += '<i class="fas fa-chevron-left"></i>';
  html += "</button>";

  // Page numbers
  const startPage = Math.max(1, page - 2);
  const endPage = Math.min(totalPages, page + 2);

  if (startPage > 1) {
    html += '<button class="pagination-btn" onclick="goToPage(1)">1</button>';
    if (startPage > 2) {
      html += '<span class="pagination-dots">...</span>';
    }
  }

  for (let i = startPage; i <= endPage; i++) {
    html +=
      '<button class="pagination-btn ' + (i === page ? "active" : "") + '" ';
    html += 'onclick="goToPage(' + i + ')">' + i + "</button>";
  }

  if (endPage < totalPages) {
    if (endPage < totalPages - 1) {
      html += '<span class="pagination-dots">...</span>';
    }
    html +=
      '<button class="pagination-btn" onclick="goToPage(' +
      totalPages +
      ')">' +
      totalPages +
      "</button>";
  }

  // Next button
  html +=
    '<button class="pagination-btn" onclick="goToPage(' +
    (page + 1) +
    ')" ' +
    (page === totalPages ? "disabled" : "") +
    ">";
  html += '<i class="fas fa-chevron-right"></i>';
  html += "</button>";

  paginationEl.innerHTML = html;
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
  // Filter radio buttons and checkboxes
  document
    .querySelectorAll(
      '.filter-option input[type="radio"], .filter-option input[type="checkbox"]'
    )
    .forEach((input) => {
      input.addEventListener("change", handleFilterChange);
    });
}

/**
 * Handle filter change
 */
function handleFilterChange(e) {
  const filterOption = e.target.closest(".filter-option");
  const filterType = filterOption.dataset.filter;
  const filterValue = filterOption.dataset.value;

  if (filterType === "brand") {
    // Single select: always set to the selected value (radio button behavior)
    currentFilters.brand = e.target.checked ? filterValue : "";
  } else if (filterType === "price") {
    // Single select: always set to the selected price range (radio button behavior)
    const minPrice = filterOption.dataset.min;
    const maxPrice = filterOption.dataset.max;
    if (e.target.checked) {
      currentFilters.minPrice = minPrice;
      currentFilters.maxPrice = maxPrice;
    } else {
      currentFilters.minPrice = "";
      currentFilters.maxPrice = "";
    }
  }

  // Reset to page 1 when filters change
  currentFilters.page = 1;

  // Reload products
  loadProducts();
}

/**
 * Handle sort change
 */
function handleSortChange() {
  const sortSelect = document.getElementById("sortSelect");
  currentFilters.sortBy = sortSelect.value;
  currentFilters.page = 1;
  loadProducts();
}

/**
 * Toggle view (grid/list)
 */
function toggleView(view) {
  const productGrid = document.getElementById("productGrid");
  const buttons = document.querySelectorAll(".view-btn");

  buttons.forEach((btn) => {
    if (btn.dataset.view === view) {
      btn.classList.add("active");
    } else {
      btn.classList.remove("active");
    }
  });

  if (view === "list") {
    productGrid.classList.remove("product-grid");
    productGrid.classList.add("product-list");
  } else {
    productGrid.classList.remove("product-list");
    productGrid.classList.add("product-grid");
  }
}

/**
 * Go to specific page
 */
function goToPage(page) {
  if (page < 1 || page > totalPages) return;
  currentFilters.page = page;
  loadProducts();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

/**
 * Clear all filters
 */
function clearFilters() {
  // Reset filters
  currentFilters = {
    category: "",
    brand: "",
    minPrice: "",
    maxPrice: "",
    sortBy: "newest",
    page: 1,
    limit: 12,
    q: "",
  };

  // Uncheck all checkboxes and radio buttons
  document
    .querySelectorAll('.filter-option input[type="checkbox"]')
    .forEach((checkbox) => {
      checkbox.checked = false;
    });

  document
    .querySelectorAll('.filter-option input[type="radio"]')
    .forEach((radio) => {
      radio.checked = false;
    });

  // Reset sort select
  const sortSelect = document.getElementById("sortSelect");
  if (sortSelect) {
    sortSelect.value = "newest";
  }

  // Reload products
  loadProducts();
}

/**
 * Update URL with current filters
 */
function updateURL() {
  const params = new URLSearchParams();

  if (currentFilters.category) params.set("category", currentFilters.category);
  if (currentFilters.brand) params.set("brand", currentFilters.brand);
  if (currentFilters.minPrice) params.set("minPrice", currentFilters.minPrice);
  if (currentFilters.maxPrice) params.set("maxPrice", currentFilters.maxPrice);
  if (currentFilters.sortBy !== "newest")
    params.set("sortBy", currentFilters.sortBy);
  if (currentFilters.page > 1) params.set("page", currentFilters.page);
  if (currentFilters.q) params.set("q", currentFilters.q);

  const newURL =
    window.location.pathname +
    (params.toString() ? "?" + params.toString() : "");
  window.history.pushState({}, "", newURL);
}

/**
 * Add to cart handler - Mua ngay (redirect to cart)
 */
async function handleBuyNow(productId) {
  if (!isLoggedIn()) {
    showToast("Vui lòng đăng nhập để mua hàng", "warning");
    setTimeout(() => {
      window.location.href =
        "/login?returnUrl=" + encodeURIComponent(window.location.pathname);
    }, 1500);
    return;
  }

  try {
    showLoading("Đang thêm vào giỏ hàng...");
    await CartAPI.addItem(productId, 1);
    // Redirect to cart page
    window.location.href = "/cart";
  } catch (error) {
    console.error("Error adding to cart:", error);
    showToast(error.message || "Không thể thêm vào giỏ hàng", "error");
    hideLoading();
  }
}

/**
 * Quick add to cart - Thêm vào giỏ không redirect
 */
async function handleQuickAddToCart(productId) {
  if (!isLoggedIn()) {
    showToast("Vui lòng đăng nhập để thêm vào giỏ hàng", "warning");
    setTimeout(() => {
      window.location.href =
        "/login?returnUrl=" + encodeURIComponent(window.location.pathname);
    }, 1500);
    return;
  }

  try {
    showLoading("Đang thêm vào giỏ hàng...");
    await CartAPI.addItem(productId, 1);
    showToast("Đã thêm vào giỏ hàng", "success");
    updateCartBadge();
  } catch (error) {
    console.error("Error adding to cart:", error);
    showToast(error.message || "Không thể thêm vào giỏ hàng", "error");
  } finally {
    hideLoading();
  }
}

/**
 * View product detail - Navigate to product detail page
 */
function viewProductDetail(productId) {
  // Đơn giản chỉ cần navigate đến /products/{id}
  window.location.href = "/products/" + productId;
}

/**
 * Calculate discount percentage
 */
function calculateDiscount(originalPrice, currentPrice) {
  return Math.round(((originalPrice - currentPrice) / originalPrice) * 100);
}
