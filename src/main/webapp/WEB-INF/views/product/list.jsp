<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%> <%@ taglib prefix="c"
uri="http://java.sun.com/jsp/jstl/core" %> <%@ taglib prefix="fmt"
uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Sản phẩm" scope="request" />
<c:set
  var="pageDescription"
  value="Khám phá bộ sưu tập sản phẩm công nghệ chính hãng tại UTE Phone Hub"
  scope="request"
/>
<!DOCTYPE html>
<html lang="vi">
  <head>
    <%@ include file="/WEB-INF/views/common/meta.jspf" %>
    <title><c:out value="${pageTitle}"/> - UTE Phone Hub</title>

    <!-- Page-specific CSS -->
    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/static/css/pages/product-list.css"
    />
  </head>
  <body>
    <%@ include file="/WEB-INF/views/common/header.jspf" %>

    <!-- Main Content -->
    <main class="main-content">
      <div class="container">
        <!-- Breadcrumb -->
        <div class="breadcrumb">
          <a href="${pageContext.request.contextPath}/">Trang chủ</a>
          <i class="fas fa-chevron-right"></i>
          <span>Sản phẩm</span>
          <c:if test="${not empty param.category}">
            <i class="fas fa-chevron-right"></i>
            <span><c:out value="${param.category}" /></span>
          </c:if>
          <c:if test="${not empty param.q}">
            <i class="fas fa-chevron-right"></i>
            <span>Tìm kiếm: "<c:out value="${param.q}" />"</span>
          </c:if>
        </div>

        <!-- Search Header (if search query exists) -->
        <c:if test="${not empty param.q}">
          <div class="search-header">
            <div class="search-info">
              <h1 class="search-title">
                <i class="fas fa-search"></i>
                Kết quả tìm kiếm cho: "<c:out value="${param.q}" />"
              </h1>
              <p class="search-count">
                Tìm thấy <strong id="totalProducts">0</strong> sản phẩm
              </p>
            </div>
          </div>
        </c:if>

        <div class="product-page-layout">
          <!-- Filter Sidebar -->
          <aside class="filter-sidebar">
            <!-- Brand Filter -->
            <div class="filter-group">
              <h3 class="filter-title">Thương hiệu</h3>
              <div class="filter-options" id="brandFilter">
                <!-- Will be loaded from API -->
              </div>
            </div>

            <!-- Price Filter -->
            <div class="filter-group">
              <h3 class="filter-title">Khoảng giá</h3>
              <div class="filter-options">
                <div
                  class="filter-option"
                  data-filter="price"
                  data-min="0"
                  data-max="5000000"
                >
                  <input type="radio" name="priceFilter" id="price1" />
                  <label for="price1">Dưới 5 triệu</label>
                </div>
                <div
                  class="filter-option"
                  data-filter="price"
                  data-min="5000000"
                  data-max="10000000"
                >
                  <input type="radio" name="priceFilter" id="price2" />
                  <label for="price2">5 - 10 triệu</label>
                </div>
                <div
                  class="filter-option"
                  data-filter="price"
                  data-min="10000000"
                  data-max="20000000"
                >
                  <input type="radio" name="priceFilter" id="price3" />
                  <label for="price3">10 - 20 triệu</label>
                </div>
                <div
                  class="filter-option"
                  data-filter="price"
                  data-min="20000000"
                  data-max="999999999"
                >
                  <input type="radio" name="priceFilter" id="price4" />
                  <label for="price4">Trên 20 triệu</label>
                </div>
              </div>
            </div>

            <!-- Clear Filters Button -->
            <button class="btn-clear-filters" onclick="clearFilters()">
              <i class="fas fa-times"></i>
              Xóa bộ lọc
            </button>
          </aside>

          <!-- Products Section -->
          <section class="products-section">
            <!-- Toolbar -->
            <div class="products-toolbar">
              <div class="toolbar-left">
                <span class="result-count" id="resultCount">Đang tải...</span>
              </div>
              <div class="toolbar-right">
                <!-- Sort Options -->
                <select
                  class="sort-select"
                  id="sortSelect"
                  onchange="handleSortChange()"
                >
                  <option value="newest">Mới nhất</option>
                  <option value="price-asc">Giá thấp đến cao</option>
                  <option value="price-desc">Giá cao đến thấp</option>
                  <option value="name">Tên A-Z</option>
                  <option value="rating">Đánh giá cao</option>
                </select>

                <!-- View Toggle -->
                <div class="view-toggle">
                  <button
                    class="view-btn active"
                    data-view="grid"
                    onclick="toggleView('grid')"
                  >
                    <i class="fas fa-th"></i>
                  </button>
                  <button
                    class="view-btn"
                    data-view="list"
                    onclick="toggleView('list')"
                  >
                    <i class="fas fa-list"></i>
                  </button>
                </div>
              </div>
            </div>

            <!-- Loading State -->
            <div class="loading-state" id="loadingState" style="display: none">
              <i class="fas fa-spinner fa-spin"></i>
              <p>Đang tải sản phẩm...</p>
            </div>

            <!-- Empty State -->
            <div class="empty-state" id="emptyState" style="display: none">
              <i class="fas fa-box-open"></i>
              <h3>Không tìm thấy sản phẩm</h3>
              <p>Vui lòng thử điều chỉnh bộ lọc hoặc tìm kiếm từ khóa khác</p>
              <button class="btn btn-primary" onclick="clearFilters()">
                Xóa bộ lọc
              </button>
            </div>

            <!-- Products Grid -->
            <div class="product-grid" id="productGrid">
              <!-- Products will be loaded here dynamically -->
            </div>

            <!-- Pagination -->
            <div class="pagination" id="pagination" style="display: none">
              <!-- Pagination buttons will be generated here -->
            </div>
          </section>
        </div>
      </div>
    </main>

    <%@ include file="/WEB-INF/views/common/footer.jspf" %>

    <!-- Page-specific JavaScript -->
    <script src="${pageContext.request.contextPath}/static/js/pages/product-list.js"></script>

    <script>
      // Initialize page with URL params
      document.addEventListener("DOMContentLoaded", () => {
        const urlParams = new URLSearchParams(window.location.search);
        const filters = {
          category: urlParams.get("category") || "",
          brand: urlParams.get("brand") || "",
          minPrice: urlParams.get("minPrice") || "",
          maxPrice: urlParams.get("maxPrice") || "",
          sortBy: urlParams.get("sortBy") || "newest",
          page: parseInt(urlParams.get("page")) || 1,
          q: urlParams.get("q") || "",
        };

        // Initialize product list page
        initProductListPage(filters);
      });
    </script>
  </body>
</html>
