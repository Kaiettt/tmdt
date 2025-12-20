/**
 * Order Lookup Page JavaScript
 * Handles public order lookup by order code and email
 */

document.addEventListener('DOMContentLoaded', () => {
    initOrderLookup();
});

// contextPath is already declared in footer.jspf

/**
 * Initialize order lookup page
 */
function initOrderLookup() {
    const form = document.getElementById('order-lookup-form');
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        // Validate form
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            return;
        }
        
        form.classList.remove('was-validated');
        await handleLookup();
    });
}

/**
 * Handle order lookup
 */
async function handleLookup() {
    const orderCode = document.getElementById('orderCode').value.trim();
    const email = document.getElementById('email').value.trim();
    const lookupBtn = document.getElementById('lookup-btn');
    const errorMessage = document.getElementById('error-message');
    const orderResult = document.getElementById('order-result');
    
    // Hide previous results/errors
    errorMessage.style.display = 'none';
    orderResult.style.display = 'none';
    
    // Validate inputs
    if (!orderCode || !email) {
        showError('Vui lòng nhập đầy đủ thông tin');
        return;
    }
    
    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showError('Địa chỉ email không hợp lệ');
        return;
    }
    
    // Show loading state
    lookupBtn.classList.add('loading');
    lookupBtn.disabled = true;
    lookupBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Đang tra cứu...';
    
    try {
        // Call API
        const response = await fetch(`${contextPath}/api/v1/orders/lookup`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                orderCode: orderCode,
                email: email
            })
        });
        
        const data = await response.json();
        
        if (response.ok && data.success && data.data) {
            // Display order details
            displayOrderDetails(data.data);
        } else {
            // Show error
            showError(data.message || 'Không tìm thấy đơn hàng hoặc email không khớp');
        }
    } catch (error) {
        console.error('Error looking up order:', error);
        showError('Đã xảy ra lỗi khi tra cứu. Vui lòng thử lại sau.');
    } finally {
        // Reset button state
        lookupBtn.classList.remove('loading');
        lookupBtn.disabled = false;
        lookupBtn.innerHTML = '<i class="fas fa-search me-2"></i>Tra cứu đơn hàng';
    }
}

/**
 * Display order details
 */
function displayOrderDetails(order) {
    const orderDetails = document.getElementById('order-details');
    const orderResult = document.getElementById('order-result');
    
    // Map status to display text and badge color
    const statusMap = {
        'PENDING': { text: 'Chờ xác nhận', class: 'bg-warning' },
        'PROCESSING': { text: 'Đang xử lý', class: 'bg-info' },
        'SHIPPED': { text: 'Đang giao hàng', class: 'bg-primary' },
        'DELIVERED': { text: 'Đã giao hàng', class: 'bg-success' },
        'CANCELLED': { text: 'Đã hủy', class: 'bg-danger' }
    };
    
    const status = statusMap[order.status] || { text: order.status, class: 'bg-secondary' };
    
    // Map payment method to display text
    const paymentMethodMap = {
        'COD': 'Thanh toán khi nhận hàng',
        'BANK_TRANSFER': 'Chuyển khoản ngân hàng',
        'STORE_PICKUP': 'Thanh toán tại cửa hàng'
    };
    
    const paymentMethod = paymentMethodMap[order.paymentMethod] || order.paymentMethod;
    
    // Build order items HTML
    let itemsHtml = '';
    if (order.items && order.items.length > 0) {
        order.items.forEach(item => {
            itemsHtml += `
                <div class="order-item">
                    <img src="${escapeHtml(item.product?.thumbnailUrl || '/static/images/placeholder.jpg')}" 
                         alt="${escapeHtml(item.product?.name || 'Product')}" 
                         class="item-image">
                    <div class="item-details">
                        <div class="item-name">${escapeHtml(item.product?.name || 'N/A')}</div>
                        <div class="item-quantity">
                            <i class="fas fa-times me-1"></i>Số lượng: ${item.quantity}
                        </div>
                    </div>
                    <div class="item-price">
                        ${formatPrice(item.price * item.quantity)}
                    </div>
                </div>
            `;
        });
    }
    
    // Build HTML
    orderDetails.innerHTML = `
        <div class="order-header">
            <div class="order-code">
                <i class="fas fa-receipt me-2"></i>
                Mã đơn hàng: ${escapeHtml(order.orderCode)}
            </div>
            <div>
                <span class="order-status-badge ${status.class}">
                    ${status.text}
                </span>
            </div>
        </div>
        
        <div class="order-info-card">
            <h5 class="mb-3">
                <i class="fas fa-info-circle me-2"></i>Thông tin đơn hàng
            </h5>
            <div class="info-row">
                <span class="info-label">Người nhận:</span>
                <span class="info-value">${escapeHtml(order.recipientName)}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Số điện thoại:</span>
                <span class="info-value">${escapeHtml(order.phoneNumber)}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Email:</span>
                <span class="info-value">${escapeHtml(order.email)}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Địa chỉ:</span>
                <span class="info-value">${escapeHtml(order.streetAddress)}, ${escapeHtml(order.city)}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Phương thức thanh toán:</span>
                <span class="info-value">${paymentMethod}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Ngày đặt hàng:</span>
                <span class="info-value">${formatDate(order.createdAt)}</span>
            </div>
        </div>
        
        ${itemsHtml ? `
            <div class="order-items">
                <h5 class="mb-3">
                    <i class="fas fa-box me-2"></i>Sản phẩm
                </h5>
                ${itemsHtml}
            </div>
        ` : ''}
        
        <div class="total-section">
            <div class="total-row">
                <span>Tổng cộng:</span>
                <span class="total-amount">${formatPrice(order.totalAmount)}</span>
            </div>
        </div>
    `;
    
    // Show result
    orderResult.style.display = 'block';
    
    // Scroll to result
    orderResult.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

/**
 * Show error message
 */
function showError(message) {
    const errorMessage = document.getElementById('error-message');
    const errorText = document.getElementById('error-text');
    
    errorText.textContent = message;
    errorMessage.style.display = 'block';
    
    // Scroll to error
    errorMessage.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
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

