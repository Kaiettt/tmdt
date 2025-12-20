/**
 * Shopping Cart JavaScript
 * Handle cart badge updates and cart operations
 */

/**
 * Update cart badge count in header
 */
async function updateCartBadge() {
    if (!isLoggedIn()) {
        // Hide cart badge for non-logged in users
        const badge = document.getElementById('cart-count');
        if (badge) {
            badge.style.display = 'none';
        }
        return;
    }
    
    try {
        const response = await CartAPI.getCart();
        
        if (response.success && response.data) {
            const totalItems = response.data.items?.reduce((sum, item) => sum + item.quantity, 0) || 0;
            
            const badge = document.getElementById('cart-count');
            if (badge) {
                badge.textContent = totalItems;
                badge.style.display = totalItems > 0 ? 'inline' : 'none';
            }
        }
    } catch (error) {
        console.error('Error updating cart badge:', error);
    }
}

/**
 * Initialize cart badge on page load
 */
document.addEventListener('DOMContentLoaded', () => {
    updateCartBadge();
});

/**
 * Update cart badge when user logs in/out
 */
window.addEventListener('storage', (e) => {
    if (e.key === 'accessToken') {
        updateCartBadge();
    }
});

