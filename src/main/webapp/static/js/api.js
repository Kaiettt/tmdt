/* UTE Phone Hub - API Helper */
/* Centralized API communication với error handling */

const API = {
    get baseURL() {
        // Get contextPath from window or default to empty string
        const ctx = (typeof contextPath !== 'undefined') ? contextPath : '';
        return ctx + '/api/v1';
    },
    
    /**
     * Get headers với JWT token
     */
    getHeaders() {
        const headers = {
            'Content-Type': 'application/json'
        };
        
        const token = localStorage.getItem('accessToken');
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        
        return headers;
    },
    
    /**
     * Handle API response
     */
    async handleResponse(response) {
        // Handle 401 Unauthorized
        if (response.status === 401) {
            console.warn('Unauthorized - redirecting to login');
            localStorage.removeItem('accessToken');
            localStorage.removeItem('user');
            
            // Redirect to login with return URL
            const ctx = (typeof contextPath !== 'undefined') ? contextPath : '';
            const returnUrl = encodeURIComponent(window.location.pathname + window.location.search);
            window.location.href = `${ctx}/login?returnUrl=${returnUrl}`;
            return null;
        }
        
        // Handle 403 Forbidden
        if (response.status === 403) {
            throw new Error('Bạn không có quyền thực hiện thao tác này');
        }
        
        // Try to parse JSON response
        let data;
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        } else {
            data = await response.text();
        }
        
        // Handle non-OK responses
        if (!response.ok) {
            const errorMessage = data.message || data.error || `HTTP error! status: ${response.status}`;
            throw new Error(errorMessage);
        }
        
        return data;
    },
    
    /**
     * GET request
     */
    async get(endpoint, params = {}) {
        try {
            // Build query string
            const queryString = new URLSearchParams(params).toString();
            const url = `${this.baseURL}${endpoint}${queryString ? '?' + queryString : ''}`;
            
            const response = await fetch(url, {
                method: 'GET',
                headers: this.getHeaders()
            });
            
            return await this.handleResponse(response);
        } catch (error) {
            console.error('API GET Error:', error);
            throw error;
        }
    },
    
    /**
     * POST request
     */
    async post(endpoint, data = {}) {
        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify(data)
            });
            
            return await this.handleResponse(response);
        } catch (error) {
            console.error('API POST Error:', error);
            throw error;
        }
    },
    
    /**
     * PUT request
     */
    async put(endpoint, data = {}) {
        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, {
                method: 'PUT',
                headers: this.getHeaders(),
                body: JSON.stringify(data)
            });
            
            return await this.handleResponse(response);
        } catch (error) {
            console.error('API PUT Error:', error);
            throw error;
        }
    },
    
    /**
     * DELETE request
     */
    async delete(endpoint) {
        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, {
                method: 'DELETE',
                headers: this.getHeaders()
            });
            
            return await this.handleResponse(response);
        } catch (error) {
            console.error('API DELETE Error:', error);
            throw error;
        }
    },
    
    /**
     * Upload file (multipart/form-data)
     */
    async upload(endpoint, formData) {
        try {
            const headers = {};
            const token = localStorage.getItem('accessToken');
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
            // Don't set Content-Type, let browser set it with boundary
            
            const response = await fetch(`${this.baseURL}${endpoint}`, {
                method: 'POST',
                headers: headers,
                body: formData
            });
            
            return await this.handleResponse(response);
        } catch (error) {
            console.error('API Upload Error:', error);
            throw error;
        }
    }
};

/**
 * Product API
 */
const ProductAPI = {
    /**
     * Get all products with filters
     */
    async getProducts(filters = {}) {
        return await API.get('/products', filters);
    },
    
    /**
     * Get product by ID
     */
    async getProductById(id) {
        return await API.get(`/products/${id}`);
    },
    
    /**
     * Get product reviews
     */
    async getProductReviews(productId, page = 1, limit = 10) {
        return await API.get(`/products/${productId}/reviews`, { page, limit });
    },
    
    /**
     * Submit product review
     */
    async submitReview(productId, data) {
        return await API.post(`/products/${productId}/reviews`, data);
    },
    
    /**
     * Search products
     */
    async searchProducts(query, filters = {}) {
        return await API.get('/products', { q: query, ...filters });
    }
};

/**
 * Cart API
 */
const CartAPI = {
    /**
     * Get current cart
     */
    async getCart() {
        return await API.get('/cart/');
    },
    
    /**
     * Add item to cart
     */
    async addItem(productId, quantity = 1) {
        return await API.post('/cart/items', { productId, quantity });
    },
    
    /**
     * Update cart item quantity
     */
    async updateItem(productId, quantity) {
        return await API.put(`/cart/items/${productId}`, { quantity });
    },
    
    /**
     * Remove item from cart
     */
    async removeItem(productId) {
        return await API.delete(`/cart/items/${productId}`);
    },
    
    /**
     * Clear cart
     */
    async clearCart() {
        return await API.delete('/cart/');
    }
};

/**
 * Order API
 */
const OrderAPI = {
    /**
     * Create order (checkout)
     */
    async createOrder(data) {
        return await API.post('/checkout', data);
    },
    
    /**
     * Get user orders
     */
    async getOrders(page = 1, limit = 10) {
        return await API.get('/orders', { page, limit });
    },
    
    /**
     * Get order by ID
     */
    async getOrderById(id) {
        return await API.get(`/orders/${id}`);
    },
    
    /**
     * Lookup order (public - no auth required)
     */
    async lookupOrder(data) {
        return await API.post('/orders/lookup', data);
    }
};

/**
 * Voucher API
 */
const VoucherAPI = {
    /**
     * Get user vouchers
     */
    async getUserVouchers(page = 1, limit = 100) {
        return await API.get('/vouchers/user', { page, limit });
    },
    
    /**
     * Get available vouchers
     */
    async getAvailableVouchers(page = 1, limit = 20) {
        return await API.get('/vouchers', { page, limit });
    },
    
    /**
     * Claim a voucher
     */
    async claimVoucher(voucherCode) {
        return await API.post('/vouchers/claim', { voucherCode });
    }
};

/**
 * User API
 */
const UserAPI = {
    /**
     * Get current user info
     */
    async getMe() {
        return await API.get('/user/me');
    },
    
    /**
     * Update profile
     */
    async updateProfile(data) {
        return await API.post('/user/profile', data);
    },
    
    /**
     * Change password
     */
    async changePassword(data) {
        return await API.post('/user/password', data);
    },
    
    /**
     * Get addresses
     */
    async getAddresses() {
        return await API.get('/user/addresses');
    },
    
    /**
     * Add address
     */
    async addAddress(data) {
        return await API.post('/user/addresses', data);
    },
    
    /**
     * Update address
     */
    async updateAddress(id, data) {
        return await API.put(`/user/addresses/${id}`, data);
    },
    
    /**
     * Delete address
     */
    async deleteAddress(id) {
        return await API.delete(`/user/addresses/${id}`);
    }
};

/**
 * Category & Brand API
 */
const CategoryAPI = {
    async getCategories() {
        return await API.get('/categories');
    },
    
    async getCategoryById(id) {
        return await API.get(`/categories/${id}`);
    }
};

const BrandAPI = {
    async getBrands() {
        return await API.get('/brands');
    },
    
    async getBrandById(id) {
        return await API.get(`/brands/${id}`);
    }
};

/**
 * Admin API
 */
const AdminAPI = {
    // Dashboard
    async getDashboardStats() {
        return await API.get('/admin/dashboard/stats');
    },
    
    // Products
    async getProducts(filters = {}) {
        return await API.get('/admin/products/', filters);
    },
    
    async createProduct(data) {
        return await API.post('/admin/products', data);
    },
    
    async updateProduct(id, data) {
        return await API.put(`/admin/products/${id}`, data);
    },
    
    async deleteProduct(id) {
        return await API.delete(`/admin/products/${id}`);
    },
    
    // Orders
    async getOrders(filters = {}) {
        return await API.get('/admin/orders/', filters);
    },
    
    async getOrderById(id) {
        return await API.get(`/admin/orders/${id}`);
    },
    
    async updateOrderStatus(id, status) {
        return await API.put(`/admin/orders/${id}/status`, { status });
    },
    
    // Users
    async getUsers(filters = {}) {
        return await API.get('/admin/users/', filters);
    },
    
    async updateUserStatus(id, status) {
        return await API.put(`/admin/users/${id}/status`, { status });
    },
    
    // Vouchers
    async getVouchers(filters = {}) {
        return await API.get('/admin/vouchers/', filters);
    },
    
    async createVoucher(data) {
        return await API.post('/admin/vouchers', data);
    },
    
    async updateVoucher(id, data) {
        return await API.put(`/admin/vouchers/${id}`, data);
    },
    
    async deleteVoucher(id) {
        return await API.delete(`/admin/vouchers/${id}`);
    },
    
    // Categories
    async getCategories() {
        return await API.get('/admin/categories/');
    },
    
    async createCategory(data) {
        return await API.post('/admin/categories', data);
    },
    
    async updateCategory(id, data) {
        return await API.put(`/admin/categories/${id}`, data);
    },
    
    async deleteCategory(id) {
        return await API.delete(`/admin/categories/${id}`);
    },
    
    // Brands
    async getBrands() {
        return await API.get('/admin/brands/');
    },
    
    async createBrand(data) {
        return await API.post('/admin/brands', data);
    },
    
    async updateBrand(id, data) {
        return await API.put(`/admin/brands/${id}`, data);
    },
    
    async deleteBrand(id) {
        return await API.delete(`/admin/brands/${id}`);
    }
};

