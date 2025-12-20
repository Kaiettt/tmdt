/**
 * Order Detail Page JavaScript
 * Handles loading and displaying order details
 */

document.addEventListener('DOMContentLoaded', () => {
    // Check authentication
    if (!isLoggedIn()) {
        showToast('Vui lòng đăng nhập để xem đơn hàng', 'warning');
        setTimeout(() => {
            const currentPath = encodeURIComponent(window.location.pathname + window.location.search);
            window.location.href = `/login?redirect=${currentPath}`;
        }, 2000);
        return;
    }
    
    // Get order ID from URL
    const pathParts = window.location.pathname.split('/');
    const orderId = pathParts[pathParts.length - 1];
    
    if (!orderId || isNaN(orderId)) {
        showErrorState();
        return;
    }
    
    // Load order details
    loadOrderDetails(orderId);
});

// contextPath is already declared in footer.jspf

/**
 * Load order details from API
 */
async function loadOrderDetails(orderId) {
    const loadingState = document.getElementById('loadingState');
    const orderContent = document.getElementById('orderContent');
    const errorState = document.getElementById('errorState');
    
    try {
        const response = await API.get(`/orders/${orderId}`);
        
        if (response.success && response.data) {
            renderOrderDetails(response.data);
            
            // Hide loading, show content
            loadingState.style.display = 'none';
            orderContent.style.display = 'block';
        } else {
            showErrorState();
        }
    } catch (error) {
        console.error('Error loading order:', error);
        showErrorState();
    }
}

/**
 * Render order details
 */
function renderOrderDetails(order) {
    // Order Header
    document.getElementById('orderCode').textContent = order.orderCode;
    document.getElementById('orderDate').textContent = formatDate(order.createdAt);
    
    // Status Badge
    const statusBadge = document.getElementById('orderStatus');
    const statusInfo = getStatusInfo(order.status);
    statusBadge.textContent = statusInfo.text;
    statusBadge.className = `badge ${statusInfo.class}`;
    
    // Order Items
    renderOrderItems(order.items);
    
    // Shipping Info
    document.getElementById('recipientName').textContent = order.recipientName;
    document.getElementById('phoneNumber').textContent = order.phoneNumber;
    document.getElementById('email').textContent = order.email || 'N/A';
    
    const fullAddress = [
        order.streetAddress,
        order.ward,
        order.district,
        order.city
    ].filter(part => part).join(', ');
    document.getElementById('address').textContent = fullAddress;
    
    if (order.notes) {
        document.getElementById('notes').textContent = order.notes;
        document.getElementById('notesRow').style.display = 'flex';
    }
    
    // Payment Method
    const paymentMethodEl = document.getElementById('paymentMethod');
    const paymentInfo = getPaymentMethodInfo(order.paymentMethod);
    paymentMethodEl.innerHTML = `<i class="${paymentInfo.icon}"></i> ${paymentInfo.text}`;
    
    // Voucher (if applicable)
    if (order.voucherCode) {
        document.getElementById('voucherCode').textContent = order.voucherCode;
        document.getElementById('voucherRow').style.display = 'flex';
    }
    
    // Total Amount
    document.getElementById('totalAmount').textContent = formatPrice(order.totalAmount);
    
    // Cancel Button (only show for certain statuses)
    if (order.status === 'PENDING' || order.status === 'PROCESSING') {
        const cancelBtn = document.getElementById('cancelOrderBtn');
        cancelBtn.style.display = 'block';
        cancelBtn.addEventListener('click', () => handleCancelOrder(order.id));
    }
}

/**
 * Render order items
 */
function renderOrderItems(items) {
    const orderItemsContainer = document.getElementById('orderItems');
    
    if (!items || items.length === 0) {
        orderItemsContainer.innerHTML = '<p class="text-muted">Không có sản phẩm nào</p>';
        return;
    }
    
    const itemsHTML = items.map(item => `
        <div class="order-item">
            <img src="${escapeHtml(item.product?.thumbnailUrl || '/static/images/placeholder.jpg')}" 
                 alt="${escapeHtml(item.product?.name || 'Product')}" 
                 class="item-image">
            <div class="item-details">
                <a href="${contextPath}/products/${item.product?.id || '#'}" class="item-name">
                    ${escapeHtml(item.product?.name || 'N/A')}
                </a>
                <div class="item-quantity">
                    <i class="fas fa-times me-1"></i>
                    Số lượng: ${item.quantity}
                </div>
            </div>
            <div class="item-price">
                ${formatPrice(item.price * item.quantity)}
            </div>
        </div>
    `).join('');
    
    orderItemsContainer.innerHTML = itemsHTML;
}

/**
 * Get status info
 */
function getStatusInfo(status) {
    const statusMap = {
        'PENDING': { text: 'Chờ xác nhận', class: 'status-pending' },
        'PROCESSING': { text: 'Đang xử lý', class: 'status-processing' },
        'SHIPPED': { text: 'Đang giao hàng', class: 'status-shipped' },
        'DELIVERED': { text: 'Đã giao hàng', class: 'status-delivered' },
        'CANCELLED': { text: 'Đã hủy', class: 'status-cancelled' }
    };
    
    return statusMap[status] || { text: status, class: 'badge-secondary' };
}

/**
 * Get payment method info
 */
function getPaymentMethodInfo(method) {
    const methodMap = {
        'COD': { text: 'Thanh toán khi nhận hàng', icon: 'fas fa-money-bill-wave text-success' },
        'BANK_TRANSFER': { text: 'Chuyển khoản ngân hàng', icon: 'fas fa-university text-primary' },
        'STORE_PICKUP': { text: 'Thanh toán tại cửa hàng', icon: 'fas fa-store text-success' }
    };
    
    return methodMap[method] || { text: method, icon: 'fas fa-credit-card' };
}

/**
 * Handle cancel order
 */
async function handleCancelOrder(orderId) {
    if (!confirm('Bạn có chắc chắn muốn hủy đơn hàng này?')) {
        return;
    }
    
    try {
        const response = await API.put(`/orders/${orderId}/cancel`, {});
        
        if (response.success) {
            showToast('Hủy đơn hàng thành công', 'success');
            
            // Reload page to update status
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            showToast(response.message || 'Không thể hủy đơn hàng', 'error');
        }
    } catch (error) {
        console.error('Error cancelling order:', error);
        showToast('Đã xảy ra lỗi khi hủy đơn hàng', 'error');
    }
}

/**
 * Show error state
 */
function showErrorState() {
    document.getElementById('loadingState').style.display = 'none';
    document.getElementById('orderContent').style.display = 'none';
    document.getElementById('errorState').style.display = 'flex';
}

/**
 * Format price to VND
 */
function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(price);
}

/**
 * Format date
 */
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

