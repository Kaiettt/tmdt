/**
 * Vouchers Page JavaScript
 * Handles loading and managing vouchers
 */

document.addEventListener('DOMContentLoaded', () => {
    loadVouchers('all');
    setupFilterTabs();
});

// contextPath is already declared in footer.jspf
let currentFilter = 'all';
let currentVoucher = null;
let savedVoucherCodes = new Set(); // Track saved vouchers

/**
 * Setup filter tabs
 */
function setupFilterTabs() {
    const tabs = document.querySelectorAll('#voucherTabs .nav-link');
    tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            e.preventDefault();
            
            // Update active state
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            
            // Load vouchers with filter
            const filter = tab.dataset.filter;
            currentFilter = filter;
            loadVouchers(filter);
        });
    });
}

/**
 * Load vouchers from API
 */
async function loadVouchers(filter = 'all') {
    const loadingState = document.getElementById('loadingState');
    const vouchersGrid = document.getElementById('vouchersGrid');
    const emptyState = document.getElementById('emptyState');
    
    // Show loading
    loadingState.style.display = 'flex';
    vouchersGrid.style.display = 'none';
    emptyState.style.display = 'none';
    
    try {
        // Build API URL based on filter
        let apiUrl = '/vouchers?page=1&limit=50&status=ACTIVE';
        
        // For 'saved' filter, we need to be logged in
        if (filter === 'saved') {
            if (!isLoggedIn()) {
                showToast('Vui lòng đăng nhập để xem vouchers đã lưu', 'warning');
                loadingState.style.display = 'none';
                emptyState.style.display = 'flex';
                return;
            }
            // Note: Backend would need to implement a user-specific saved vouchers endpoint
            // For now, we'll load all and filter locally
        }
        
        const response = await API.get(apiUrl);
        
        if (response.success && response.data) {
            const vouchers = response.data;
            
            // Filter vouchers based on selection
            let filteredVouchers = vouchers;
            if (filter === 'available') {
                filteredVouchers = vouchers.filter(v => v.usageLimit > v.usedCount);
            } else if (filter === 'saved') {
                filteredVouchers = vouchers.filter(v => savedVoucherCodes.has(v.code));
            }
            
            // Hide loading
            loadingState.style.display = 'none';
            
            if (filteredVouchers.length === 0) {
                emptyState.style.display = 'flex';
            } else {
                renderVouchers(filteredVouchers);
                vouchersGrid.style.display = 'grid';
            }
        } else {
            throw new Error('Invalid response');
        }
    } catch (error) {
        console.error('Error loading vouchers:', error);
        loadingState.style.display = 'none';
        emptyState.style.display = 'flex';
        showToast('Không thể tải vouchers', 'error');
    }
}

/**
 * Render vouchers grid
 */
function renderVouchers(vouchers) {
    const vouchersGrid = document.getElementById('vouchersGrid');
    
    const vouchersHTML = vouchers.map(voucher => {
        const isSaved = savedVoucherCodes.has(voucher.code);
        const saveButtonHTML = isSaved 
            ? `<button class="btn btn-saved" disabled><i class="fas fa-check me-2"></i>Đã lưu</button>`
            : `<button class="btn btn-save-voucher" data-voucher-code="${escapeHtml(voucher.code)}"><i class="fas fa-bookmark me-2"></i>Lưu voucher</button>`;
        
        return `
            <div class="voucher-card" data-voucher-code="${escapeHtml(voucher.code)}">
                <div class="voucher-header">
                    ${getStatusBadge(voucher)}
                    <div class="voucher-code">${escapeHtml(voucher.code)}</div>
                    <div class="voucher-type-badge">${getVoucherType(voucher.discountType)}</div>
                </div>
                <div class="voucher-body">
                    <div class="voucher-description">${escapeHtml(voucher.description || 'Voucher giảm giá đặc biệt')}</div>
                    <div class="voucher-discount">${formatDiscount(voucher)}</div>
                    <div class="voucher-conditions">
                        ${getConditions(voucher).map(cond => `
                            <div class="condition-item">
                                <i class="fas fa-check-circle"></i>
                                <span>${cond}</span>
                            </div>
                        `).join('')}
                    </div>
                    <div class="voucher-expiry">
                        <i class="far fa-clock"></i>
                        <span>Hết hạn: ${formatDate(voucher.expiryDate)}</span>
                    </div>
                    <div class="voucher-footer">
                        ${saveButtonHTML}
                        <button class="btn btn-view-detail" data-voucher-code="${escapeHtml(voucher.code)}">
                            <i class="fas fa-info-circle me-2"></i>
                            Chi tiết
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
    
    vouchersGrid.innerHTML = vouchersHTML;
    
    // Add event listeners
    setupVoucherCardListeners();
}

/**
 * Setup voucher card event listeners
 */
function setupVoucherCardListeners() {
    // Save voucher buttons
    document.querySelectorAll('.btn-save-voucher').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.stopPropagation();
            const voucherCode = btn.dataset.voucherCode;
            await handleSaveVoucher(voucherCode);
        });
    });
    
    // View detail buttons
    document.querySelectorAll('.btn-view-detail').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const voucherCode = btn.dataset.voucherCode;
            showVoucherDetail(voucherCode);
        });
    });
    
    // Card click to show detail
    document.querySelectorAll('.voucher-card').forEach(card => {
        card.addEventListener('click', () => {
            const voucherCode = card.dataset.voucherCode;
            showVoucherDetail(voucherCode);
        });
    });
}

/**
 * Handle save voucher
 */
async function handleSaveVoucher(voucherCode) {
    if (!isLoggedIn()) {
        showToast('Vui lòng đăng nhập để lưu voucher', 'warning');
        setTimeout(() => {
            window.location.href = contextPath + '/login?redirect=' + encodeURIComponent(window.location.pathname);
        }, 1500);
        return;
    }
    
    try {
        // In real app, this would save to backend
        // For now, we'll just track locally
        savedVoucherCodes.add(voucherCode);
        localStorage.setItem('savedVouchers', JSON.stringify([...savedVoucherCodes]));
        
        showToast('Đã lưu voucher thành công!', 'success');
        
        // Reload to update UI
        loadVouchers(currentFilter);
    } catch (error) {
        console.error('Error saving voucher:', error);
        showToast('Không thể lưu voucher', 'error');
    }
}

/**
 * Show voucher detail modal
 */
async function showVoucherDetail(voucherCode) {
    try {
        // Fetch voucher details
        const response = await API.get(`/vouchers/${voucherCode}`);
        
        if (response.success && response.data) {
            const voucher = response.data;
            currentVoucher = voucher;
            
            // Populate modal
            document.getElementById('modalVoucherCode').innerHTML = `
                <div class="code-display">${escapeHtml(voucher.code)}</div>
            `;
            document.getElementById('modalVoucherDescription').textContent = voucher.description || 'Voucher giảm giá đặc biệt';
            document.getElementById('modalVoucherDiscount').textContent = formatDiscount(voucher);
            
            const conditionsList = document.getElementById('modalVoucherConditions');
            conditionsList.innerHTML = getConditions(voucher).map(cond => `<li>${escapeHtml(cond)}</li>`).join('');
            
            document.getElementById('modalVoucherExpiry').textContent = `Từ ${formatDate(voucher.startDate)} đến ${formatDate(voucher.expiryDate)}`;
            
            // Update save button
            const saveBtn = document.getElementById('saveVoucherBtn');
            const isSaved = savedVoucherCodes.has(voucher.code);
            if (isSaved) {
                saveBtn.disabled = true;
                saveBtn.innerHTML = '<i class="fas fa-check me-2"></i>Đã lưu';
                saveBtn.classList.remove('btn-primary');
                saveBtn.classList.add('btn-success');
            } else {
                saveBtn.disabled = false;
                saveBtn.innerHTML = '<i class="fas fa-bookmark me-2"></i>Lưu voucher';
                saveBtn.classList.remove('btn-success');
                saveBtn.classList.add('btn-primary');
                saveBtn.onclick = () => {
                    handleSaveVoucher(voucher.code);
                    bootstrap.Modal.getInstance(document.getElementById('voucherDetailModal')).hide();
                };
            }
            
            // Show modal
            const modal = new bootstrap.Modal(document.getElementById('voucherDetailModal'));
            modal.show();
        }
    } catch (error) {
        console.error('Error loading voucher detail:', error);
        showToast('Không thể tải thông tin voucher', 'error');
    }
}

/**
 * Get status badge HTML
 */
function getStatusBadge(voucher) {
    const remaining = voucher.usageLimit - voucher.usedCount;
    const percentRemaining = (remaining / voucher.usageLimit) * 100;
    
    if (percentRemaining <= 20 && percentRemaining > 0) {
        return `<div class="voucher-status-badge status-limited"><i class="fas fa-exclamation-circle me-1"></i>Sắp hết</div>`;
    } else if (new Date(voucher.expiryDate) - new Date() < 7 * 24 * 60 * 60 * 1000) {
        return `<div class="voucher-status-badge status-expiring"><i class="fas fa-clock me-1"></i>Sắp hết hạn</div>`;
    } else {
        return `<div class="voucher-status-badge status-active"><i class="fas fa-check-circle me-1"></i>Khả dụng</div>`;
    }
}

/**
 * Get voucher type display
 */
function getVoucherType(discountType) {
    return discountType === 'PERCENTAGE' ? 'GIẢM %' : 'GIẢM TIỀN';
}

/**
 * Format discount display
 */
function formatDiscount(voucher) {
    if (voucher.discountType === 'PERCENTAGE') {
        return `Giảm ${voucher.discountValue}%`;
    } else {
        return `Giảm ${formatPrice(voucher.discountValue)}`;
    }
}

/**
 * Get voucher conditions
 */
function getConditions(voucher) {
    const conditions = [];
    
    if (voucher.minOrderAmount) {
        conditions.push(`Đơn hàng tối thiểu ${formatPrice(voucher.minOrderAmount)}`);
    }
    
    if (voucher.maxDiscount && voucher.discountType === 'PERCENTAGE') {
        conditions.push(`Giảm tối đa ${formatPrice(voucher.maxDiscount)}`);
    }
    
    if (voucher.usageLimit) {
        const remaining = voucher.usageLimit - voucher.usedCount;
        conditions.push(`Còn ${remaining}/${voucher.usageLimit} lượt sử dụng`);
    }
    
    return conditions;
}

/**
 * Format price
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
        day: '2-digit'
    }).format(date);
}

/**
 * Escape HTML
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Check if user is logged in
 */
function isLoggedIn() {
    return !!localStorage.getItem('accessToken');
}

// Load saved vouchers from localStorage on page load
const savedVouchersStr = localStorage.getItem('savedVouchers');
if (savedVouchersStr) {
    try {
        const savedArray = JSON.parse(savedVouchersStr);
        savedVoucherCodes = new Set(savedArray);
    } catch (e) {
        console.error('Error parsing saved vouchers:', e);
    }
}

