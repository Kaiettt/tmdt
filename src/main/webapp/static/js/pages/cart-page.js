/**
 * Cart Page JavaScript
 * Integrates with Cart API (Redis-backed session cart)
 */

document.addEventListener('DOMContentLoaded', () => {
    // Check authentication
    if (!isLoggedIn()) {
        showLoginRequired();
        return;
    }
    
    // Initialize cart page
    initCartPage();
});

/**
 * Initialize cart page
 */
async function initCartPage() {
    try {
        await loadCart();
        setupCartEventListeners();
    } catch (error) {
        console.error('Error initializing cart page:', error);
        showError('Không thể tải giỏ hàng. Vui lòng thử lại sau.');
    }
}

/**
 * Setup event listeners for cart interactions (event delegation)
 */
function setupCartEventListeners() {
    const cartItemsContainer = document.getElementById('cart-items');
    if (!cartItemsContainer) return;
    
    // Use event delegation for dynamically rendered cart items
    cartItemsContainer.addEventListener('click', async (e) => {
        const target = e.target;
        const cartItem = target.closest('.cart-item');
        if (!cartItem) return;
        
        const productId = parseInt(cartItem.dataset.productId);
        
        // Handle quantity increase
        if (target.closest('.quantity-btn-plus')) {
            const quantityInput = cartItem.querySelector('.quantity-input');
            const currentQuantity = parseInt(quantityInput.value);
            await handleUpdateQuantity(productId, currentQuantity + 1);
        }
        
        // Handle quantity decrease
        else if (target.closest('.quantity-btn-minus')) {
            const quantityInput = cartItem.querySelector('.quantity-input');
            const currentQuantity = parseInt(quantityInput.value);
            if (currentQuantity > 1) {
                await handleUpdateQuantity(productId, currentQuantity - 1);
            }
        }
        
        // Handle remove item
        else if (target.closest('.cart-item-remove')) {
            await handleRemoveItem(productId);
        }
    });
    
    // Checkout button
    const checkoutBtn = document.getElementById('checkout-btn');
    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', handleCheckout);
    }
}

/**
 * Load cart from API
 */
async function loadCart() {
    try {
        showLoadingState();
        
        const response = await CartAPI.getCart();
        
        if (response.success && response.data) {
            const cart = response.data;
            
            if (!cart.items || cart.items.length === 0) {
                showEmptyCart();
            } else {
                renderCart(cart);
                updateCartSummary(cart);
                showCartContent();
                
                // Load suggested products
                loadSuggestedProducts();
            }
        } else {
            showEmptyCart();
        }
    } catch (error) {
        console.error('Error loading cart:', error);
        if (error.message.includes('401')) {
            showLoginRequired();
        } else {
            showError('Không thể tải giỏ hàng. Vui lòng thử lại sau.');
        }
    }
}

/**
 * Render cart items
 */
function renderCart(cart) {
    const cartItemsContainer = document.getElementById('cart-items');
    const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/cart'));
    
    if (!cart.items || cart.items.length === 0) {
        cartItemsContainer.innerHTML = '';
        return;
    }
    
    cartItemsContainer.innerHTML = cart.items.map(item => {
        // API returns: productId, productName, thumbnailUrl, price, quantity, lineTotal
        const subtotal = item.lineTotal || (item.price * item.quantity);
        
        return `
            <div class="cart-item mb-3 p-3 bg-white rounded shadow-sm" data-product-id="${item.productId}">
                <div class="row align-items-center">
                    <!-- Product Image & Info -->
                    <div class="col-md-5">
                        <div class="d-flex gap-3">
                            <img src="${escapeHtml(item.thumbnailUrl || 'https://via.placeholder.com/80x80/cccccc/666666?text=No+Image')}" 
                                 alt="${escapeHtml(item.productName || 'Product')}" 
                                 class="rounded" 
                                 style="width: 80px; height: 80px; object-fit: cover;">
                            <div>
                                <h6 class="mb-1">
                                    <a href="${contextPath}/products/${item.productId}" class="text-decoration-none text-dark">
                                        ${escapeHtml(item.productName || 'Sản phẩm')}
                                    </a>
                                </h6>
                            </div>
                        </div>
                    </div>
                    
                    <!-- Price -->
                    <div class="col-md-2 text-center">
                        <div class="fw-bold text-danger">${formatPrice(item.price || 0)}</div>
                    </div>
                    
                    <!-- Quantity Controls -->
                    <div class="col-md-3 text-center">
                        <div class="input-group input-group-sm mx-auto" style="width: 120px;">
                            <button class="btn btn-outline-secondary quantity-btn-minus" type="button" 
                                    ${item.quantity <= 1 ? 'disabled' : ''}>
                                <i class="fas fa-minus"></i>
                            </button>
                            <input type="text" class="form-control text-center quantity-input" 
                                   value="${item.quantity}" readonly>
                            <button class="btn btn-outline-secondary quantity-btn-plus" type="button">
                                <i class="fas fa-plus"></i>
                            </button>
                        </div>
                    </div>
                    
                    <!-- Subtotal -->
                    <div class="col-md-1 text-end">
                        <div class="fw-bold text-primary">${formatPrice(subtotal)}</div>
                    </div>
                    
                    <!-- Remove Button -->
                    <div class="col-md-1 text-end">
                        <button class="btn btn-sm btn-outline-danger cart-item-remove" type="button" 
                                title="Xóa sản phẩm">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
    
    // Show header actions
    document.getElementById('cart-header-actions').style.display = 'flex';
}

/**
 * Update cart summary
 */
function updateCartSummary(cart) {
    const subtotal = cart.totalPrice || 0;
    const shipping = subtotal >= 1000000 ? 0 : 30000; // Free shipping over 1M VND
    const total = subtotal + shipping;
    
    document.getElementById('subtotal').textContent = formatPrice(subtotal);
    document.getElementById('shipping').textContent = shipping === 0 ? 'Miễn phí' : formatPrice(shipping);
    document.getElementById('total').textContent = formatPrice(total);
}

/**
 * Handle update quantity
 */
async function handleUpdateQuantity(productId, newQuantity) {
    if (newQuantity < 1) {
        handleRemoveItem(productId);
        return;
    }
    
    try {
        const response = await CartAPI.updateItem(productId, newQuantity);
        
        if (response.success) {
            showToast('Đã cập nhật số lượng', 'success');
            await loadCart();
            updateCartBadge();
        } else {
            showToast(response.message || 'Không thể cập nhật số lượng', 'error');
        }
    } catch (error) {
        console.error('Error updating quantity:', error);
        showToast('Không thể cập nhật số lượng', 'error');
    }
}

/**
 * Handle remove item
 */
async function handleRemoveItem(productId) {
    if (!confirm('Bạn có chắc chắn muốn xóa sản phẩm này khỏi giỏ hàng?')) {
        return;
    }
    
    try {
        const response = await CartAPI.removeItem(productId);
        
        if (response.success) {
            showToast('Đã xóa sản phẩm khỏi giỏ hàng', 'success');
            await loadCart();
            updateCartBadge();
        } else {
            showToast(response.message || 'Không thể xóa sản phẩm', 'error');
        }
    } catch (error) {
        console.error('Error removing item:', error);
        showToast('Không thể xóa sản phẩm', 'error');
    }
}

/**
 * Handle clear cart
 */
async function handleClearCart() {
    if (!confirm('Bạn có chắc chắn muốn xóa tất cả sản phẩm khỏi giỏ hàng?')) {
        return;
    }
    
    try {
        // Get current cart
        const cartResponse = await CartAPI.getCart();
        
        if (cartResponse.success && cartResponse.data && cartResponse.data.items) {
            // Remove each item
            const removePromises = cartResponse.data.items.map(item => 
                CartAPI.removeItem(item.productId)
            );
            
            await Promise.all(removePromises);
            
            showToast('Đã xóa tất cả sản phẩm', 'success');
            await loadCart();
            updateCartBadge();
        }
    } catch (error) {
        console.error('Error clearing cart:', error);
        showToast('Không thể xóa giỏ hàng', 'error');
    }
}

/**
 * Handle checkout
 */
function handleCheckout() {
    const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/cart'));
    
    if (!isLoggedIn()) {
        showToast('Vui lòng đăng nhập để thanh toán', 'warning');
        setTimeout(() => {
            window.location.href = contextPath + '/login?redirect=' + encodeURIComponent(window.location.pathname);
        }, 1500);
        return;
    }
    
    // Redirect to checkout page
    window.location.href = contextPath + '/checkout';
}

/**
 * Load suggested products
 */
async function loadSuggestedProducts() {
    try {
        const response = await ProductAPI.getProducts({
            limit: 4,
            page: 1,
            sort: 'popular' // or 'newest'
        });
        
        if (response.success && response.data && response.data.length > 0) {
            renderSuggestedProducts(response.data);
            document.getElementById('suggested-products').classList.remove('d-none');
        }
    } catch (error) {
        console.error('Error loading suggested products:', error);
        // Silently fail - not critical
    }
}

/**
 * Render suggested products
 */
function renderSuggestedProducts(products) {
    const container = document.getElementById('suggested-products-grid');
    const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/cart'));
    
    container.innerHTML = products.map(product => {
        return `
            <div class="product-card" data-product-id="${product.id}">
                <div class="product-image-container">
                    <a href="${contextPath}/products/${product.id}">
                        <img src="${escapeHtml(product.thumbnailUrl || 'https://via.placeholder.com/300x200/cccccc/666666?text=No+Image')}" 
                             alt="${escapeHtml(product.name)}" 
                             class="product-image" 
                             loading="lazy">
                    </a>
                    ${product.discount > 0 ? `<div class="product-badges"><span class="badge badge-sale">-${product.discount}%</span></div>` : ''}
                </div>
                <div class="product-info">
                    <h3 class="product-title">
                        <a href="${contextPath}/products/${product.id}">${escapeHtml(product.name)}</a>
                    </h3>
                    <div class="product-price">
                        <span class="price-current">${formatPrice(product.price)}</span>
                        ${product.originalPrice && product.originalPrice > product.price ? 
                            `<span class="price-original">${formatPrice(product.originalPrice)}</span>` : ''}
                    </div>
                    <button class="btn btn-primary btn-sm w-100" onclick="handleAddToCartFromSuggested(${product.id})">
                        <i class="fas fa-cart-plus"></i>
                        Thêm vào giỏ
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

/**
 * Handle add to cart from suggested products
 */
async function handleAddToCartFromSuggested(productId) {
    try {
        const response = await CartAPI.addItem(productId, 1);
        
        if (response.success) {
            showToast('Đã thêm vào giỏ hàng', 'success');
            await loadCart();
            updateCartBadge();
        } else {
            showToast(response.message || 'Không thể thêm vào giỏ hàng', 'error');
        }
    } catch (error) {
        console.error('Error adding to cart:', error);
        showToast('Không thể thêm vào giỏ hàng', 'error');
    }
}

/**
 * Show loading state
 */
function showLoadingState() {
    document.getElementById('loading-state').style.display = 'block';
    document.getElementById('error-state').classList.add('d-none');
    document.getElementById('empty-cart').classList.add('d-none');
    document.getElementById('cart-content').classList.add('d-none');
}

/**
 * Show cart content
 */
function showCartContent() {
    document.getElementById('loading-state').style.display = 'none';
    document.getElementById('error-state').classList.add('d-none');
    document.getElementById('empty-cart').classList.add('d-none');
    document.getElementById('cart-content').classList.remove('d-none');
}

/**
 * Show empty cart
 */
function showEmptyCart() {
    document.getElementById('loading-state').style.display = 'none';
    document.getElementById('error-state').classList.add('d-none');
    document.getElementById('empty-cart').classList.remove('d-none');
    document.getElementById('cart-content').classList.add('d-none');
    document.getElementById('cart-header-actions').style.display = 'none';
    
    // Hide suggested products when cart is empty
    document.getElementById('suggested-products').classList.add('d-none');
}

/**
 * Show error state
 */
function showError(message) {
    document.getElementById('loading-state').style.display = 'none';
    document.getElementById('error-state').classList.remove('d-none');
    document.getElementById('error-message').textContent = message;
    document.getElementById('empty-cart').classList.add('d-none');
    document.getElementById('cart-content').classList.add('d-none');
}

/**
 * Show login required
 */
function showLoginRequired() {
    const contextPath = window.location.pathname.substring(0, window.location.pathname.indexOf('/cart'));
    
    document.getElementById('loading-state').style.display = 'none';
    document.getElementById('error-state').classList.remove('d-none');
    document.getElementById('error-message').innerHTML = `
        Vui lòng <a href="${contextPath}/login?redirect=${encodeURIComponent(window.location.pathname)}">đăng nhập</a> 
        để xem giỏ hàng.
    `;
    document.getElementById('empty-cart').classList.add('d-none');
    document.getElementById('cart-content').classList.add('d-none');
}

