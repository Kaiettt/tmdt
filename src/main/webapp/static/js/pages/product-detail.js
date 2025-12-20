/**
 * Product Detail Page JavaScript
 * Handles product detail loading, image gallery, add to cart, reviews
 */

document.addEventListener('DOMContentLoaded', () => {
    // Get product ID from URL
    const pathParts = window.location.pathname.split('/');
    const productId = pathParts[pathParts.length - 1];
    
    if (!productId || isNaN(productId)) {
        showError('ID sản phẩm không hợp lệ');
        return;
    }
    
    // Initialize page
    loadProductDetail(productId);
    setupEventListeners(productId);
});

/**
 * Load product detail from API
 */
async function loadProductDetail(productId) {
    try {
        showLoading();
        
        // Fetch product detail
        const response = await ProductAPI.getProductById(productId);
        
        if (response.success && response.data) {
            const product = response.data;
            
            // Render product
            renderProduct(product);
            
            // Load reviews
            loadReviews(productId);
            
            // Load related products
            loadRelatedProducts(product.categoryId);
            
            hideLoading();
        } else {
            showError(response.message || 'Không tìm thấy sản phẩm');
        }
    } catch (error) {
        console.error('Error loading product detail:', error);
        showError('Không thể tải thông tin sản phẩm. Vui lòng thử lại sau.');
    }
}

/**
 * Render product information
 */
function renderProduct(product) {
    const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/products'));
    
    // Update page title and breadcrumb
    document.title = escapeHtml(product.name) + ' - UTE Phone Hub';
    document.getElementById('breadcrumb-category').textContent = product.categoryName || 'Sản phẩm';
    document.getElementById('breadcrumb-product').textContent = product.name;
    
    // Main image
    const mainImage = document.getElementById('main-product-image');
    const placeholderUrl = product.thumbnailUrl || 'https://via.placeholder.com/500x500/cccccc/666666?text=No+Image';
    mainImage.src = placeholderUrl;
    mainImage.alt = product.name;
    
    // Product badges
    const badgesContainer = document.getElementById('product-badges');
    badgesContainer.innerHTML = '';
    if (product.discount > 0) {
        const badge = document.createElement('span');
        badge.className = 'badge badge-sale';
        badge.textContent = '-' + product.discount + '%';
        badgesContainer.appendChild(badge);
    }
    if (product.isNew) {
        const badge = document.createElement('span');
        badge.className = 'badge badge-new';
        badge.textContent = 'Mới';
        badgesContainer.appendChild(badge);
    }
    
    // Thumbnail images (if available)
    const thumbnailsContainer = document.getElementById('thumbnail-images');
    thumbnailsContainer.innerHTML = '';
    if (product.images && product.images.length > 0) {
        product.images.forEach((imageUrl, index) => {
            const thumb = document.createElement('img');
            thumb.src = imageUrl;
            thumb.alt = product.name + ' ' + (index + 1);
            thumb.className = 'thumbnail-image' + (index === 0 ? ' active' : '');
            thumb.onclick = function() {
                mainImage.src = imageUrl;
                document.querySelectorAll('.thumbnail-image').forEach(t => t.classList.remove('active'));
                this.classList.add('active');
            };
            thumbnailsContainer.appendChild(thumb);
        });
    } else {
        // Use main thumbnail as the only image
        const thumb = document.createElement('img');
        thumb.src = product.thumbnailUrl;
        thumb.alt = product.name;
        thumb.className = 'thumbnail-image active';
        thumbnailsContainer.appendChild(thumb);
    }
    
    // Product name
    document.getElementById('product-name').textContent = product.name;
    
    // Rating
    renderRating(product.averageRating || 0, product.reviewCount || 0);
    document.getElementById('sold-count').textContent = 'Đã bán: ' + (product.soldCount || 0);
    
    // Price
    const priceElement = document.getElementById('price-current');
    priceElement.textContent = formatPrice(product.price);
    
    if (product.originalPrice && product.originalPrice > product.price) {
        const originalPriceElement = document.getElementById('price-original');
        originalPriceElement.textContent = formatPrice(product.originalPrice);
        originalPriceElement.classList.remove('d-none');
        
        const discountBadge = document.getElementById('discount-badge');
        const discountPercent = Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100);
        discountBadge.textContent = '-' + discountPercent + '%';
        discountBadge.classList.remove('d-none');
    }
    
    // Key specs
    if (product.specifications) {
        // Parse specifications if it's a JSON string
        const specs = typeof product.specifications === 'string' 
            ? JSON.parse(product.specifications) 
            : product.specifications;
        renderKeySpecs(specs);
    }
    
    // Stock status
    const stockStatus = document.getElementById('stock-status');
    const stockText = document.getElementById('stock-text');
    const stockAvailable = document.getElementById('stock-available');
    const quantityInput = document.getElementById('quantity-input');
    
    if (product.stockQuantity > 0) {
        stockStatus.className = 'stock-status in-stock';
        stockText.innerHTML = '<i class="fas fa-check-circle"></i> Còn hàng';
        stockAvailable.textContent = 'Còn ' + product.stockQuantity + ' sản phẩm';
        quantityInput.max = product.stockQuantity;
        
        // Enable buttons
        document.getElementById('btn-add-cart').disabled = false;
        document.getElementById('btn-buy-now').disabled = false;
    } else {
        stockStatus.className = 'stock-status out-of-stock';
        stockText.innerHTML = '<i class="fas fa-times-circle"></i> Hết hàng';
        stockAvailable.textContent = 'Tạm hết hàng';
        
        // Disable buttons
        document.getElementById('btn-add-cart').disabled = true;
        document.getElementById('btn-buy-now').disabled = true;
    }
    
    // Description
    const descriptionContent = document.getElementById('description-content');
    descriptionContent.innerHTML = product.description || '<p>Chưa có mô tả sản phẩm.</p>';
    
    // Specifications table
    if (product.specifications) {
        // Parse specifications if it's a JSON string
        const specs = typeof product.specifications === 'string' 
            ? JSON.parse(product.specifications) 
            : product.specifications;
        renderSpecsTable(specs);
    }
    
    // Store product ID for later use
    window.currentProduct = product;
}

/**
 * Render rating stars
 */
function renderRating(rating, count) {
    const starsContainer = document.getElementById('rating-stars');
    starsContainer.innerHTML = '';
    
    for (let i = 1; i <= 5; i++) {
        const star = document.createElement('i');
        star.className = i <= rating ? 'fas fa-star' : 'far fa-star';
        starsContainer.appendChild(star);
    }
    
    document.getElementById('rating-text').textContent = '(' + count + ' đánh giá)';
}

/**
 * Render key specifications
 */
function renderKeySpecs(specs) {
    const specsGrid = document.getElementById('specs-grid');
    specsGrid.innerHTML = '';
    
    // Define key specs to display (customize based on product category)
    const keySpecsMap = {
        'Màn hình': specs.screen || specs.display,
        'CPU': specs.processor || specs.cpu,
        'RAM': specs.ram,
        'Bộ nhớ': specs.storage || specs.rom,
        'Camera': specs.camera,
        'Pin': specs.battery,
        'Hệ điều hành': specs.os || specs.operatingSystem
    };
    
    for (const [label, value] of Object.entries(keySpecsMap)) {
        if (value) {
            const specItem = document.createElement('div');
            specItem.className = 'spec-item';
            specItem.innerHTML = '<strong>' + escapeHtml(label) + ':</strong> ' + escapeHtml(value);
            specsGrid.appendChild(specItem);
        }
    }
}

/**
 * Render full specifications table
 */
function renderSpecsTable(specs) {
    const tableBody = document.getElementById('specs-table').getElementsByTagName('tbody')[0];
    tableBody.innerHTML = '';
    
    for (const [key, value] of Object.entries(specs)) {
        if (value) {
            const row = tableBody.insertRow();
            const cellLabel = row.insertCell(0);
            const cellValue = row.insertCell(1);
            
            cellLabel.className = 'spec-label';
            cellLabel.textContent = formatSpecLabel(key);
            
            cellValue.className = 'spec-value';
            cellValue.textContent = value;
        }
    }
}

/**
 * Format specification label
 */
function formatSpecLabel(key) {
    const labelMap = {
        'screen': 'Màn hình',
        'display': 'Màn hình',
        'processor': 'Vi xử lý',
        'cpu': 'CPU',
        'ram': 'RAM',
        'storage': 'Bộ nhớ trong',
        'rom': 'ROM',
        'camera': 'Camera',
        'frontCamera': 'Camera trước',
        'rearCamera': 'Camera sau',
        'battery': 'Pin',
        'os': 'Hệ điều hành',
        'operatingSystem': 'Hệ điều hành',
        'connectivity': 'Kết nối',
        'sim': 'SIM',
        'weight': 'Trọng lượng',
        'dimensions': 'Kích thước'
    };
    
    return labelMap[key] || key.charAt(0).toUpperCase() + key.slice(1);
}

/**
 * Load reviews
 */
async function loadReviews(productId) {
    try {
        const response = await fetch('/api/v1/products/' + productId + '/reviews?page=1&limit=10', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        const data = await response.json();
        
        if (data.success && data.data) {
            renderReviews(data.data, productId);
            
            // Update review count in tab
            const totalReviews = data.metadata?.pagination?.totalItems || 0;
            document.getElementById('review-count-tab').textContent = totalReviews;
        }
        
        // Show write review button if user is logged in (regardless of reviews)
        if (isLoggedIn()) {
            const writeReviewAction = document.getElementById('write-review-action');
            if (writeReviewAction) {
                writeReviewAction.classList.remove('d-none');
            }
        }
    } catch (error) {
        console.error('Error loading reviews:', error);
    }
}

/**
 * Reload product info (to update rating stats after review changes)
 */
async function reloadProductInfo(productId) {
    try {
        const response = await fetch('/api/v1/products/' + productId, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        const data = await response.json();
        
        if (data.success && data.data) {
            const product = data.data;
            // Update rating display in header
            renderRating(product.averageRating || 0, product.reviewCount || 0);
        }
    } catch (error) {
        console.error('Error reloading product info:', error);
    }
}

/**
 * Render reviews
 */
function renderReviews(reviews, productId) {
    const reviewsList = document.getElementById('reviews-list');
    
    // Ensure reviews is an array
    if (!Array.isArray(reviews) || reviews.length === 0) {
        reviewsList.innerHTML = '<div class="no-reviews"><p>Chưa có đánh giá nào cho sản phẩm này.</p><p>Hãy là người đầu tiên đánh giá!</p></div>';
        return;
    }
    
    // Calculate average rating
    const avgRating = reviews.reduce((sum, r) => sum + (r.rating || 0), 0) / reviews.length;
    document.getElementById('avg-rating').textContent = avgRating.toFixed(1);
    document.getElementById('total-reviews').textContent = reviews.length + ' đánh giá';
    
    // Render stars in summary
    const summaryStars = document.getElementById('summary-stars');
    summaryStars.innerHTML = '';
    for (let i = 1; i <= 5; i++) {
        const star = document.createElement('i');
        star.className = i <= avgRating ? 'fas fa-star' : 'far fa-star';
        summaryStars.appendChild(star);
    }
    
    // Render review items
    reviewsList.innerHTML = '';
    const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
    const currentUserId = currentUser.id;
    
    reviews.forEach(review => {
        const reviewItem = document.createElement('div');
        reviewItem.className = 'review-item';
        reviewItem.dataset.reviewId = review.id;
        
        let starsHtml = '';
        for (let i = 1; i <= 5; i++) {
            starsHtml += '<i class="fas fa-star' + (i <= review.rating ? '' : ' text-muted') + '"></i>';
        }
        
        // Check if this review belongs to current user
        const isOwnReview = currentUserId && review.user && review.user.id === currentUserId;
        const actionsHtml = isOwnReview ? `
            <div class="review-actions">
                <button class="btn btn-sm btn-outline-primary edit-review-btn" data-review-id="${review.id}">
                    <i class="fas fa-edit"></i> Sửa
                </button>
                <button class="btn btn-sm btn-outline-danger delete-review-btn" data-review-id="${review.id}">
                    <i class="fas fa-trash"></i> Xóa
                </button>
            </div>
        ` : '';
        
        reviewItem.innerHTML = `
            <div class="review-header">
                <div class="review-user">
                    <i class="fas fa-user-circle"></i> ${escapeHtml(review.user?.fullName || 'Anonymous')}
                </div>
                <div class="review-date">${formatDate(review.createdAt)}</div>
            </div>
            <div class="review-rating">${starsHtml}</div>
            <div class="review-comment">${escapeHtml(review.comment)}</div>
            ${actionsHtml}
        `;
        
        reviewsList.appendChild(reviewItem);
    });
}

/**
 * Load related products
 */
async function loadRelatedProducts(categoryId) {
    try {
        const response = await ProductAPI.getProducts({
            categoryId: categoryId,
            limit: 4,
            page: 1
        });
        
        if (response.success && response.data && response.data.length > 0) {
            renderRelatedProducts(response.data);
        } else {
            document.getElementById('related-products').innerHTML = '<p class="text-center">Không có sản phẩm tương tự.</p>';
        }
    } catch (error) {
        console.error('Error loading related products:', error);
    }
}

/**
 * Render related products
 */
function renderRelatedProducts(products) {
    const container = document.getElementById('related-products');
    const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/products'));
    
    container.innerHTML = products.map(product => {
        return '<div class="product-card" data-product-id="' + product.id + '">' +
            '<div class="product-image-container">' +
            '<a href="' + contextPath + '/products/' + product.id + '">' +
            '<img src="' + escapeHtml(product.thumbnailUrl || 'https://via.placeholder.com/300x200/cccccc/666666?text=No+Image') + '" alt="' + escapeHtml(product.name) + '" class="product-image" loading="lazy">' +
            '</a>' +
            (product.discount > 0 ? '<div class="product-badges"><span class="badge badge-sale">-' + product.discount + '%</span></div>' : '') +
            '</div>' +
            '<div class="product-info">' +
            '<h3 class="product-title"><a href="' + contextPath + '/products/' + product.id + '">' + escapeHtml(product.name) + '</a></h3>' +
            '<div class="product-price">' +
            '<span class="price-current">' + formatPrice(product.price) + '</span>' +
            (product.originalPrice && product.originalPrice > product.price ? '<span class="price-original">' + formatPrice(product.originalPrice) + '</span>' : '') +
            '</div>' +
            '</div>' +
            '</div>';
    }).join('');
}

/**
 * Setup event listeners
 */
function setupEventListeners(productId) {
    // Quantity controls
    document.getElementById('btn-decrease').addEventListener('click', () => {
        const input = document.getElementById('quantity-input');
        if (input.value > 1) {
            input.value = parseInt(input.value) - 1;
        }
    });
    
    document.getElementById('btn-increase').addEventListener('click', () => {
        const input = document.getElementById('quantity-input');
        const max = parseInt(input.max);
        if (parseInt(input.value) < max) {
            input.value = parseInt(input.value) + 1;
        }
    });
    
    // Add to cart
    document.getElementById('btn-add-cart').addEventListener('click', async () => {
        const quantity = parseInt(document.getElementById('quantity-input').value);
        await handleAddToCart(productId, quantity);
    });
    
    // Buy now
    document.getElementById('btn-buy-now').addEventListener('click', async () => {
        const quantity = parseInt(document.getElementById('quantity-input').value);
        const success = await handleAddToCart(productId, quantity);
        if (success) {
            const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/products'));
            window.location.href = contextPath + '/cart';
        }
    });
    
    // Wishlist
    document.getElementById('btn-wishlist').addEventListener('click', () => {
        if (!isLoggedIn()) {
            showToast('Vui lòng đăng nhập để thêm vào danh sách yêu thích', 'warning');
            return;
        }
        // TODO: Implement wishlist functionality
        showToast('Tính năng đang được phát triển', 'info');
    });
    
    // Open Review Modal
    const openReviewModalBtn = document.getElementById('open-review-modal-btn');
    if (openReviewModalBtn) {
        openReviewModalBtn.addEventListener('click', () => {
            openReviewModal(productId);
        });
    }
    
    // Review Modal - Rating Stars
    document.querySelectorAll('#rating-input-modal i').forEach(star => {
        star.addEventListener('click', function() {
            const rating = this.getAttribute('data-rating');
            document.getElementById('rating-value-modal').value = rating;
            
            // Update stars display
            document.querySelectorAll('#rating-input-modal i').forEach((s, index) => {
                s.className = index < rating ? 'fas fa-star text-warning' : 'far fa-star text-warning';
            });
            
            // Hide error if any
            document.getElementById('rating-error').style.display = 'none';
        });
    });
    
    // Review Modal - Submit Button
    const submitReviewBtn = document.getElementById('submit-review-btn');
    if (submitReviewBtn) {
        submitReviewBtn.addEventListener('click', async () => {
            await handleSubmitReviewFromModal(productId);
        });
    }
    
    // Edit/Delete review buttons (event delegation)
    document.addEventListener('click', function(e) {
        // Edit review
        if (e.target.closest('.edit-review-btn')) {
            const reviewId = e.target.closest('.edit-review-btn').dataset.reviewId;
            handleEditReview(productId, reviewId);
        }
        
        // Delete review
        if (e.target.closest('.delete-review-btn')) {
            const reviewId = e.target.closest('.delete-review-btn').dataset.reviewId;
            handleDeleteReview(productId, reviewId);
        }
    });
}

/**
 * Handle add to cart
 */
async function handleAddToCart(productId, quantity) {
    if (!isLoggedIn()) {
        showToast('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng', 'warning');
        const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/products'));
        setTimeout(() => {
            window.location.href = contextPath + '/login?redirect=' + encodeURIComponent(window.location.pathname);
        }, 1500);
        return false;
    }
    
    try {
        const response = await CartAPI.addItem(productId, quantity);
        
        if (response.success) {
            showToast('Đã thêm sản phẩm vào giỏ hàng', 'success');
            updateCartBadge();
            return true;
        } else {
            showToast(response.message || 'Không thể thêm vào giỏ hàng', 'danger');
            return false;
        }
    } catch (error) {
        console.error('Error adding to cart:', error);
        showToast('Đã xảy ra lỗi. Vui lòng thử lại.', 'danger');
        return false;
    }
}

/**
 * Handle submit review
 */
async function handleSubmitReview(productId) {
    const rating = parseInt(document.getElementById('rating-value').value);
    const comment = document.getElementById('review-comment').value.trim();
    
    if (rating === 0) {
        showToast('Vui lòng chọn số sao đánh giá', 'warning');
        return;
    }
    
    if (!comment) {
        showToast('Vui lòng nhập nhận xét', 'warning');
        return;
    }
    
    try {
        const token = localStorage.getItem('accessToken');
        const response = await fetch('/api/v1/products/' + productId + '/reviews', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ rating, comment })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Cảm ơn bạn đã đánh giá!', 'success');
            
            // Reset form
            document.getElementById('review-form').reset();
            document.getElementById('rating-value').value = '0';
            document.querySelectorAll('#rating-input i').forEach(s => {
                s.className = 'far fa-star';
            });
            
            // Reload reviews
            loadReviews(productId);
        } else {
            showToast(data.message || 'Không thể gửi đánh giá', 'danger');
        }
    } catch (error) {
        console.error('Error submitting review:', error);
        showToast('Đã xảy ra lỗi. Vui lòng thử lại.', 'danger');
    }
}

/**
 * Handle edit review
 */
async function handleEditReview(productId, reviewId) {
    // Find the review in the current list
    const reviewItem = document.querySelector(`.review-item[data-review-id="${reviewId}"]`);
    if (!reviewItem) return;
    
    // Get current review data
    const ratingStars = reviewItem.querySelectorAll('.review-rating i.fas.fa-star').length;
    const commentText = reviewItem.querySelector('.review-comment').textContent;
    
    // Show edit form (replace the review item temporarily)
    const editFormHtml = `
        <div class="review-edit-form" data-review-id="${reviewId}">
            <h5>Chỉnh sửa đánh giá</h5>
            <div class="mb-3">
                <label>Đánh giá của bạn:</label>
                <div class="rating-edit-input" id="rating-edit-input-${reviewId}">
                    ${[1,2,3,4,5].map(i => `<i class="${i <= ratingStars ? 'fas' : 'far'} fa-star" data-rating="${i}"></i>`).join('')}
                </div>
                <input type="hidden" id="rating-edit-value-${reviewId}" value="${ratingStars}">
            </div>
            <div class="mb-3">
                <label>Nhận xét:</label>
                <textarea class="form-control" id="review-edit-comment-${reviewId}" rows="4" required>${commentText}</textarea>
            </div>
            <div class="btn-group">
                <button class="btn btn-sm btn-primary save-edit-btn" data-review-id="${reviewId}">
                    <i class="fas fa-save"></i> Lưu
                </button>
                <button class="btn btn-sm btn-secondary cancel-edit-btn" data-review-id="${reviewId}">
                    <i class="fas fa-times"></i> Hủy
                </button>
            </div>
        </div>
    `;
    
    // Replace review item with edit form
    reviewItem.innerHTML = editFormHtml;
    
    // Add event listeners for edit form
    const ratingEditInput = document.getElementById(`rating-edit-input-${reviewId}`);
    ratingEditInput.querySelectorAll('i').forEach(star => {
        star.addEventListener('click', function() {
            const rating = this.getAttribute('data-rating');
            document.getElementById(`rating-edit-value-${reviewId}`).value = rating;
            
            // Update stars display
            ratingEditInput.querySelectorAll('i').forEach((s, index) => {
                s.className = index < rating ? 'fas fa-star' : 'far fa-star';
            });
        });
    });
    
    // Save edit
    document.querySelector(`.save-edit-btn[data-review-id="${reviewId}"]`).addEventListener('click', async () => {
        const newRating = parseInt(document.getElementById(`rating-edit-value-${reviewId}`).value);
        const newComment = document.getElementById(`review-edit-comment-${reviewId}`).value.trim();
        
        if (newRating === 0) {
            showToast('Vui lòng chọn số sao đánh giá', 'warning');
            return;
        }
        
        if (!newComment) {
            showToast('Vui lòng nhập nhận xét', 'warning');
            return;
        }
        
        try {
            const token = localStorage.getItem('accessToken');
            const response = await fetch(`/api/v1/reviews/${reviewId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token
                },
                body: JSON.stringify({ rating: newRating, comment: newComment })
            });
            
            const data = await response.json();
            
            if (data.success) {
                showToast('Cập nhật đánh giá thành công!', 'success');
                loadReviews(productId);
                reloadProductInfo(productId); // Reload rating stats
            } else {
                showToast(data.message || 'Không thể cập nhật đánh giá', 'error');
            }
        } catch (error) {
            console.error('Error updating review:', error);
            showToast('Đã xảy ra lỗi. Vui lòng thử lại.', 'error');
        }
    });
    
    // Cancel edit
    document.querySelector(`.cancel-edit-btn[data-review-id="${reviewId}"]`).addEventListener('click', () => {
        loadReviews(productId);
    });
}

/**
 * Handle delete review
 */
async function handleDeleteReview(productId, reviewId) {
    if (!confirm('Bạn có chắc chắn muốn xóa đánh giá này?')) {
        return;
    }
    
    try {
        const token = localStorage.getItem('accessToken');
        const response = await fetch(`/api/v1/reviews/${reviewId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': 'Bearer ' + token
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Xóa đánh giá thành công!', 'success');
            loadReviews(productId);
            reloadProductInfo(productId); // Reload rating stats
        } else {
            showToast(data.message || 'Không thể xóa đánh giá', 'error');
        }
    } catch (error) {
        console.error('Error deleting review:', error);
        showToast('Đã xảy ra lỗi. Vui lòng thử lại.', 'error');
    }
}

/**
 * Open review modal
 */
function openReviewModal(productId) {
    if (!isLoggedIn()) {
        showToast('Vui lòng đăng nhập để đánh giá sản phẩm', 'warning');
        const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/products'));
        setTimeout(() => {
            window.location.href = contextPath + '/login?redirect=' + encodeURIComponent(window.location.pathname);
        }, 1500);
        return;
    }
    
    // Reset form
    document.getElementById('review-modal-form').reset();
    document.getElementById('rating-value-modal').value = '0';
    document.querySelectorAll('#rating-input-modal i').forEach(s => {
        s.className = 'far fa-star text-warning';
    });
    
    // Hide errors
    document.getElementById('rating-error').style.display = 'none';
    document.getElementById('comment-error').style.display = 'none';
    
    // Show modal
    const reviewModal = new bootstrap.Modal(document.getElementById('reviewModal'));
    reviewModal.show();
}

/**
 * Handle submit review from modal
 */
async function handleSubmitReviewFromModal(productId) {
    const rating = parseInt(document.getElementById('rating-value-modal').value);
    const comment = document.getElementById('review-comment-modal').value.trim();
    
    // Validate
    let hasError = false;
    
    if (rating === 0) {
        document.getElementById('rating-error').style.display = 'block';
        hasError = true;
    } else {
        document.getElementById('rating-error').style.display = 'none';
    }
    
    if (!comment) {
        document.getElementById('review-comment-modal').classList.add('is-invalid');
        document.getElementById('comment-error').style.display = 'block';
        hasError = true;
    } else {
        document.getElementById('review-comment-modal').classList.remove('is-invalid');
        document.getElementById('comment-error').style.display = 'none';
    }
    
    if (hasError) {
        return;
    }
    
    // Disable submit button
    const submitBtn = document.getElementById('submit-review-btn');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang gửi...';
    
    try {
        const token = localStorage.getItem('accessToken');
        const response = await fetch('/api/v1/products/' + productId + '/reviews', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ rating, comment })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Cảm ơn bạn đã đánh giá!', 'success');
            
            // Close modal
            const reviewModal = bootstrap.Modal.getInstance(document.getElementById('reviewModal'));
            reviewModal.hide();
            
            // Reload reviews and product info
            loadReviews(productId);
            reloadProductInfo(productId); // Reload rating stats
        } else {
            showToast(data.message || 'Không thể gửi đánh giá', 'danger');
        }
    } catch (error) {
        console.error('Error submitting review:', error);
        showToast('Đã xảy ra lỗi. Vui lòng thử lại.', 'danger');
    } finally {
        // Re-enable submit button
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    }
}

/**
 * Show loading state
 */
function showLoading() {
    document.getElementById('loading-state').style.display = 'block';
    document.getElementById('error-state').classList.add('d-none');
    document.getElementById('product-content').classList.add('d-none');
}

/**
 * Hide loading state
 */
function hideLoading() {
    document.getElementById('loading-state').style.display = 'none';
    document.getElementById('product-content').classList.remove('d-none');
}

/**
 * Show error state
 */
function showError(message) {
    document.getElementById('loading-state').style.display = 'none';
    document.getElementById('error-state').classList.remove('d-none');
    document.getElementById('error-message').textContent = message;
}

