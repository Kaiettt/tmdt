/**
 * Checkout Page JavaScript
 * Handles checkout flow: addresses, payment, vouchers, place order
 */

document.addEventListener('DOMContentLoaded', () => {
    // Check authentication
    if (!isLoggedIn()) {
        showToast('Vui lòng đăng nhập để thanh toán', 'warning');
        setTimeout(() => {
            window.location.href = `/login?redirect=${encodeURIComponent('/cart/checkout')}`;
        }, 2000);
        return;
    }

    // Initialize checkout page
    initCheckoutPage();
});

// contextPath is already declared in footer.jspf
let selectedAddressId = null;
let cartData = null;
let provinces = [];
let wards = [];

/**
 * Initialize checkout page
 */
async function initCheckoutPage() {
    try {
        showLoading();

        // Load cart, addresses, and provinces in parallel
        const [cart, addresses, provincesData] = await Promise.all([
            loadCart(),
            loadSavedAddresses(),
            loadProvinces()
        ]);

        if (!cart || !cart.items || cart.items.length === 0) {
            showError('Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.');
            setTimeout(() => {
                window.location.href = `${contextPath}/cart`;
            }, 2000);
            return;
        }

        cartData = cart;
        provinces = provincesData || [];

        renderOrderSummary(cart);
        renderSavedAddresses(addresses);
        populateProvinces();
        setupEventListeners();

        hideLoading();
    } catch (error) {
        console.error('Error initializing checkout:', error);
        showError('Không thể tải trang thanh toán. Vui lòng thử lại.');
    }
}

/**
 * Load cart from API
 */
async function loadCart() {
    try {
        const response = await API.get('/cart/');
        if (response.success && response.data) {
            return response.data;
        }
        throw new Error(response.message || 'Failed to load cart');
    } catch (error) {
        console.error('Error loading cart:', error);
        throw error;
    }
}

/**
 * Load saved addresses
 */
async function loadSavedAddresses() {
    try {
        const response = await API.get('/user/addresses');
        if (response.success && response.data) {
            return response.data;
        }
        return [];
    } catch (error) {
        console.error('Error loading addresses:', error);
        return [];
    }
}

/**
 * Load provinces
 */
async function loadProvinces() {
    try {
        const response = await API.get('/location/provinces');
        if (response.success && response.data) {
            return response.data;
        }
        return [];
    } catch (error) {
        console.error('Error loading provinces:', error);
        return [];
    }
}

/**
 * Render saved addresses
 */
function renderSavedAddresses(addresses) {
    const container = document.getElementById('saved-addresses');
    if (!container) return;

    if (!addresses || addresses.length === 0) {
        container.innerHTML = `
            <div class="alert alert-info">
                <i class="fas fa-info-circle me-2"></i>
                Bạn chưa có địa chỉ đã lưu. Vui lòng thêm địa chỉ giao hàng.
            </div>
        `;
        // Auto show new address form
        document.getElementById('new-address-form').classList.remove('d-none');
        document.getElementById('toggle-address-form').style.display = 'none';
        return;
    }

    container.innerHTML = addresses.map((addr, index) => `
        <div class="saved-address ${index === 0 ? 'selected' : ''}" data-address-id="${addr.id}">
            <div class="address-name">${escapeHtml(addr.recipientName || 'Người nhận')}</div>
            <div class="address-phone">
                <i class="fas fa-phone me-1"></i>${escapeHtml(addr.phoneNumber || '')}
            </div>
            <div class="address-detail">
                ${escapeHtml(addr.streetAddress || '')}, 
                ${escapeHtml(addr.ward || '')}, 
                ${escapeHtml(addr.city || '')}
            </div>
            ${addr.isDefault ? '<div class="address-badges"><span class="badge bg-primary">Mặc định</span></div>' : ''}
        </div>
    `).join('');

    // Set first address as selected
    if (addresses.length > 0) {
        selectedAddressId = addresses[0].id;
    }
}

/**
 * Render order summary
 */
function renderOrderSummary(cart) {
    // Render items
    const itemsContainer = document.getElementById('order-items-summary');
    if (itemsContainer) {
        itemsContainer.innerHTML = cart.items.map(item => `
            <div class="order-item-summary">
                <img src="${escapeHtml(item.thumbnailUrl || 'https://via.placeholder.com/80x80/cccccc/666666?text=No+Image')}" 
                     alt="${escapeHtml(item.productName)}" 
                     class="order-item-image"
                     onerror="this.src='https://via.placeholder.com/80x80/cccccc/666666?text=Error'">
                <div class="order-item-info">
                    <div class="order-item-name">${escapeHtml(item.productName)}</div>
                    <div class="order-item-quantity">Số lượng: ${item.quantity}</div>
                </div>
                <div class="order-item-price">${formatPrice(item.lineTotal || (item.price * item.quantity))}</div>
            </div>
        `).join('');
    }

    // Update summary totals
    updateOrderSummary(cart);
}

/**
 * Update order summary totals
 */
function updateOrderSummary(cart) {
    const subtotal = cart.totalPrice || 0;
    const shipping = subtotal >= 1000000 ? 0 : 30000; // Free shipping > 1M VND
    const discount = cart.discountAmount || 0;
    const total = subtotal + shipping - discount;

    document.getElementById('summary-subtotal').textContent = formatPrice(subtotal);
    document.getElementById('summary-shipping').textContent = shipping === 0 ? 'Miễn phí' : formatPrice(shipping);
    document.getElementById('summary-discount').textContent = formatPrice(discount);
    document.getElementById('summary-total').textContent = formatPrice(total);

    // Show/hide discount row
    const discountRow = document.getElementById('discount-row');
    if (discount > 0) {
        discountRow.classList.add('active');
    } else {
        discountRow.classList.remove('active');
    }
}

/**
 * Populate provinces dropdown
 */
function populateProvinces() {
    const citySelect = document.getElementById('city');
    if (!citySelect || !provinces || provinces.length === 0) return;

    citySelect.innerHTML = '<option value="">Chọn tỉnh/thành</option>' +
        provinces.map(p => `<option value="${p.code}" data-code="${p.code}">${escapeHtml(p.name)}</option>`).join('');
}

/**
 * Load wards by province code
 */
async function loadWards(provinceCode) {
    try {
        const response = await API.get(`/location/provinces/${provinceCode}/wards`);
        if (response.success && response.data) {
            wards = response.data;
            populateWards();
            return true;
        }
        return false;
    } catch (error) {
        console.error('Error loading wards:', error);
        return false;
    }
}

/**
 * Populate wards dropdown
 */
function populateWards() {
    const wardSelect = document.getElementById('ward');
    if (!wardSelect) return;

    if (!wards || wards.length === 0) {
        wardSelect.innerHTML = '<option value="">Chọn phường/xã</option>';
        return;
    }

    wardSelect.innerHTML = '<option value="">Chọn phường/xã</option>' +
        wards.map(w => `<option value="${w.name}" data-code="${w.code}">${escapeHtml(w.name)}</option>`).join('');
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Address selection
    document.getElementById('saved-addresses').addEventListener('click', (e) => {
        const addressDiv = e.target.closest('.saved-address');
        if (addressDiv) {
            document.querySelectorAll('.saved-address').forEach(a => a.classList.remove('selected'));
            addressDiv.classList.add('selected');
            selectedAddressId = parseInt(addressDiv.dataset.addressId);
        }
    });

    // Toggle new address form
    document.getElementById('toggle-address-form').addEventListener('click', () => {
        const form = document.getElementById('new-address-form');
        const btn = document.getElementById('toggle-address-form');

        if (form.classList.contains('d-none')) {
            form.classList.remove('d-none');
            btn.innerHTML = '<i class="fas fa-minus me-2"></i>Ẩn form';
        } else {
            form.classList.add('d-none');
            btn.innerHTML = '<i class="fas fa-plus me-2"></i>Thêm địa chỉ mới';
        }
    });

    // Province change
    document.getElementById('city').addEventListener('change', async (e) => {
        const citySelect = e.target;
        const selectedOption = citySelect.options[citySelect.selectedIndex];

        // Get province code from selected option's data attribute
        const provinceCode = selectedOption.dataset.code || selectedOption.value;

        if (provinceCode) {
            console.log('Loading wards for province:', provinceCode);
            const success = await loadWards(provinceCode);
            if (!success) {
                console.error('Failed to load wards');
                document.getElementById('ward').innerHTML = '<option value="">Chọn phường/xã</option>';
            }
        } else {
            document.getElementById('ward').innerHTML = '<option value="">Chọn phường/xã</option>';
            wards = [];
        }
    });

    // Apply/Remove voucher
    document.getElementById('apply-voucher-btn').addEventListener('click', function () {
        if (this.classList.contains('btn-remove-voucher')) {
            removeVoucher();
        } else {
            handleApplyVoucher();
        }
    });

    // Payment method change - show/hide payment form
    const paymentMethods = document.querySelectorAll('input[name="paymentMethod"]');
    paymentMethods.forEach(radio => {
        radio.addEventListener('change', handlePaymentMethodChange);
    });

    // Auto-format card number
    const cardNumber = document.getElementById('cardNumber');
    if (cardNumber) {
        cardNumber.addEventListener('input', formatCardNumber);
    }

    // Auto-format expiry date
    const expiryDate = document.getElementById('expiryDate');
    if (expiryDate) {
        expiryDate.addEventListener('input', formatExpiryDate);
    }

    // Place order
    document.getElementById('place-order-btn').addEventListener('click', handlePlaceOrder);
}

/**
 * Handle payment method change
 */
function handlePaymentMethodChange(e) {
    const paymentForm = document.getElementById('bank-transfer-form');

    if (e.target.value === 'BANK_TRANSFER') {
        paymentForm.style.display = 'block';
        // Mark fields as required
        document.getElementById('cardNumber').required = true;
        document.getElementById('cardHolder').required = true;
        document.getElementById('expiryDate').required = true;
        document.getElementById('cvv').required = true;
    } else {
        paymentForm.style.display = 'none';
        // Remove required attribute
        document.getElementById('cardNumber').required = false;
        document.getElementById('cardHolder').required = false;
        document.getElementById('expiryDate').required = false;
        document.getElementById('cvv').required = false;
    }
}

/**
 * Format card number (add spaces every 4 digits)
 */
function formatCardNumber(e) {
    let value = e.target.value.replace(/\s/g, '');
    value = value.replace(/\D/g, ''); // Remove non-digits
    value = value.substring(0, 16); // Max 16 digits

    // Add space every 4 digits
    value = value.match(/.{1,4}/g)?.join(' ') || value;

    e.target.value = value;
}

/**
 * Format expiry date (MM/YY format)
 */
function formatExpiryDate(e) {
    let value = e.target.value.replace(/\//g, '');
    value = value.replace(/\D/g, ''); // Remove non-digits
    value = value.substring(0, 4); // Max 4 digits

    // Add slash after 2 digits
    if (value.length >= 2) {
        value = value.substring(0, 2) + '/' + value.substring(2);
    }

    e.target.value = value;
}

/**
 * Validate payment info
 */
function validatePaymentInfo() {
    const cardNumber = document.getElementById('cardNumber').value.replace(/\s/g, '');
    const cardHolder = document.getElementById('cardHolder').value.trim();
    const expiryDate = document.getElementById('expiryDate').value;
    const cvv = document.getElementById('cvv').value;

    // Validate card number (13-19 digits)
    if (cardNumber.length < 13 || cardNumber.length > 19) {
        showToast('Số thẻ không hợp lệ', 'error');
        return false;
    }

    // Validate card holder
    if (cardHolder.length < 2) {
        showToast('Tên chủ thẻ không hợp lệ', 'error');
        return false;
    }

    // Validate expiry date format (MM/YY)
    if (!/^\d{2}\/\d{2}$/.test(expiryDate)) {
        showToast('Ngày hết hạn không hợp lệ (MM/YY)', 'error');
        return false;
    }

    const [month, year] = expiryDate.split('/');
    if (parseInt(month) < 1 || parseInt(month) > 12) {
        showToast('Tháng không hợp lệ', 'error');
        return false;
    }

    // Validate CVV (3 digits)
    if (!/^\d{3}$/.test(cvv)) {
        showToast('CVV không hợp lệ (3 chữ số)', 'error');
        return false;
    }

    return true;
}

/**
 * Calculate order total before discount
 * @returns {number} Total amount in VND
 */
function calculateOrderTotal() {
    if (!cartData || !cartData.totalPrice) {
        return 0;
    }

    const subtotal = cartData.totalPrice || 0;
    const shipping = subtotal >= 1000000 ? 0 : 30000; // Free shipping > 1M VND

    return subtotal + shipping;
}

/**
 * Handle apply voucher
 */
async function handleApplyVoucher() {
    const voucherCode = document.getElementById('voucher-code').value.trim();
    const messageDiv = document.getElementById('voucher-message');
    const applyBtn = document.getElementById('apply-voucher-btn');

    if (!voucherCode) {
        messageDiv.innerHTML = '<i class="fas fa-exclamation-circle"></i> Vui lòng nhập mã giảm giá';
        messageDiv.className = 'voucher-message error';
        messageDiv.style.display = 'block';
        return;
    }

    // Show loading
    applyBtn.disabled = true;
    applyBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    messageDiv.style.display = 'none';

    try {
        // Calculate total before discount
        const orderTotal = calculateOrderTotal();

        if (!orderTotal || orderTotal <= 0) {
            messageDiv.innerHTML = '<i class="fas fa-exclamation-circle"></i> Không thể áp dụng mã giảm giá cho đơn hàng trống';
            messageDiv.className = 'voucher-message error';
            messageDiv.style.display = 'block';
            applyBtn.innerHTML = '<i class="fas fa-check"></i> Áp dụng';
            applyBtn.disabled = false;
            return;
        }

        // Validate voucher
        const response = await API.post('/vouchers/validate', {
            code: voucherCode,
            orderTotal: orderTotal  // Backend expects: orderTotal, not totalAmount
        });

        if (response.success && response.data) {
            const voucher = response.data;

            // Show success message
            messageDiv.innerHTML = `<i class="fas fa-check-circle"></i> Đã áp dụng mã giảm giá: <strong>${voucherCode}</strong>`;
            messageDiv.className = 'voucher-message success';
            messageDiv.style.display = 'block';

            // Update order summary with discount
            updateOrderSummaryWithVoucher(voucher);

            // Change button to remove
            applyBtn.innerHTML = '<i class="fas fa-times"></i> Hủy';
            applyBtn.classList.add('btn-remove-voucher');
            applyBtn.disabled = false;

            // Store voucher data
            window.appliedVoucher = voucher;
        } else {
            messageDiv.innerHTML = `<i class="fas fa-times-circle"></i> ${response.message || 'Mã giảm giá không hợp lệ hoặc không áp dụng được'}`;
            messageDiv.className = 'voucher-message error';
            messageDiv.style.display = 'block';

            applyBtn.innerHTML = '<i class="fas fa-check"></i> Áp dụng';
            applyBtn.disabled = false;
        }
    } catch (error) {
        console.error('Error applying voucher:', error);
        messageDiv.innerHTML = '<i class="fas fa-times-circle"></i> Đã xảy ra lỗi khi áp dụng mã giảm giá';
        messageDiv.className = 'voucher-message error';
        messageDiv.style.display = 'block';

        applyBtn.innerHTML = '<i class="fas fa-check"></i> Áp dụng';
        applyBtn.disabled = false;
    }
}

/**
 * Update order summary with voucher discount
 * Backend already calculated discountAmount and finalAmount
 */
function updateOrderSummaryWithVoucher(voucherResponse) {
    if (!voucherResponse || !cartData) return;

    // Get values from backend response
    const subtotal = cartData.totalPrice || 0;
    const shipping = subtotal >= 1000000 ? 0 : 30000;
    const discount = voucherResponse.discountAmount || 0;
    const finalAmount = voucherResponse.finalAmount || 0;

    // Calculate total with shipping
    const total = finalAmount + shipping;

    // Update summary display
    document.getElementById('summary-subtotal').textContent = formatPrice(subtotal);
    document.getElementById('summary-shipping').textContent = shipping === 0 ? 'Miễn phí' : formatPrice(shipping);
    document.getElementById('summary-discount').textContent = '- ' + formatPrice(discount);
    document.getElementById('summary-total').textContent = formatPrice(total);

    // Show discount row
    const discountRow = document.getElementById('discount-row');
    if (discountRow) {
        discountRow.classList.add('active');
        discountRow.style.display = 'flex';
    }
}

/**
 * Remove voucher
 */
function removeVoucher() {
    const messageDiv = document.getElementById('voucher-message');
    const applyBtn = document.getElementById('apply-voucher-btn');

    // Clear voucher
    document.getElementById('voucher-code').value = '';
    window.appliedVoucher = null;

    // Reset button
    applyBtn.innerHTML = '<i class="fas fa-check"></i> Áp dụng';
    applyBtn.classList.remove('btn-remove-voucher');
    applyBtn.disabled = false;

    // Hide message
    messageDiv.style.display = 'none';

    // Update summary without discount
    updateOrderSummary(cartData);

    // Hide discount row
    const discountRow = document.getElementById('discount-row');
    if (discountRow) {
        discountRow.style.setProperty('display', 'none', 'important');
    }
}

/**
 * Handle place order
 */
async function handlePlaceOrder() {
    const placeOrderBtn = document.getElementById('place-order-btn');

    // Validate address
    const addressForm = document.getElementById('address-form');
    const isNewAddressVisible = !document.getElementById('new-address-form').classList.contains('d-none');

    let shippingData = {};

    if (isNewAddressVisible) {
        // Validate new address form
        if (!addressForm.checkValidity()) {
            addressForm.classList.add('was-validated');
            showToast('Vui lòng điền đầy đủ thông tin giao hàng', 'error');
            return;
        }

        // Collect form data
        const formData = new FormData(addressForm);
        const citySelect = document.getElementById('city');
        const wardSelect = document.getElementById('ward');

        shippingData = {
            recipientName: formData.get('recipientName'),
            phoneNumber: formData.get('phoneNumber'),
            email: formData.get('email') || '',
            city: citySelect.options[citySelect.selectedIndex].text,
            ward: wardSelect.options[wardSelect.selectedIndex].text,
            streetAddress: formData.get('streetAddress'),
            notes: formData.get('notes') || ''
        };
    } else {
        // Use selected saved address
        if (!selectedAddressId) {
            showToast('Vui lòng chọn địa chỉ giao hàng', 'error');
            return;
        }

        shippingData = {
            addressId: selectedAddressId
        };
    }

    // Get payment method
    const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;

    // Validate payment info if BANK_TRANSFER
    if (paymentMethod === 'BANK_TRANSFER') {
        if (!validatePaymentInfo()) {
            return;
        }
    }

    // Prepare checkout data with proper structure
    const checkoutData = {
        shippingInfo: shippingData,  // Wrap shipping data in shippingInfo object
        paymentMethod: paymentMethod,
        voucherCode: document.getElementById('voucher-code').value.trim() || null
    };

    // Debug log
    console.log('Checkout data:', checkoutData);

    // Add payment info if BANK_TRANSFER
    if (paymentMethod === 'BANK_TRANSFER') {
        checkoutData.paymentInfo = {
            cardNumber: document.getElementById('cardNumber').value.replace(/\s/g, ''),
            cardHolder: document.getElementById('cardHolder').value.trim().toUpperCase(),
            expiryDate: document.getElementById('expiryDate').value,
            cvv: document.getElementById('cvv').value
        };
    }

    // Show loading
    placeOrderBtn.classList.add('loading');
    placeOrderBtn.disabled = true;

    try {
        const response = await API.post('/checkout', checkoutData);

        if (response.success && response.data) {
            const order = response.data;

            showToast('Đặt hàng thành công!', 'success');

            const paymentUrl = order.paymentUrl || order.payment_url;

            // ✅ If VNPay URL exists → redirect to VNPay
            if (paymentUrl && paymentUrl.trim() !== '') {
                setTimeout(() => {
                    window.location.href = paymentUrl;
                }, 800);
                return;
            }

            // ✅ Otherwise → normal order success page
            setTimeout(() => {
                window.location.href =
                    `${contextPath}/orders/${order.orderId}?success=true`;
            }, 1500);

        } else {
            showToast(
                response.message || 'Không thể đặt hàng. Vui lòng thử lại.',
                'error'
            );
            placeOrderBtn.classList.remove('loading');
            placeOrderBtn.disabled = false;
        }
    } catch (error) {
        console.error('Error placing order:', error);
        showToast('Đã xảy ra lỗi khi đặt hàng. Vui lòng thử lại.', 'error');
        placeOrderBtn.classList.remove('loading');
        placeOrderBtn.disabled = false;
    }

}

/**
 * Show loading state
 */
function showLoading() {
    document.getElementById('checkout-loading').classList.remove('d-none');
    document.getElementById('checkout-content').classList.add('d-none');
    document.getElementById('checkout-error').classList.add('d-none');
}

/**
 * Hide loading state
 */
function hideLoading() {
    document.getElementById('checkout-loading').classList.add('d-none');
    document.getElementById('checkout-content').classList.remove('d-none');
}

/**
 * Show error state
 */
function showError(message) {
    document.getElementById('checkout-error-message').textContent = message;
    document.getElementById('checkout-error').classList.remove('d-none');
    document.getElementById('checkout-loading').classList.add('d-none');
    document.getElementById('checkout-content').classList.add('d-none');
}

