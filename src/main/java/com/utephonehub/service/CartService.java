package com.utephonehub.service;

import com.utephonehub.entity.*;
import com.utephonehub.repository.CartRepository;
import com.utephonehub.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.*;

public class CartService {
    
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    
    public CartService() {
        this.cartRepository = new CartRepository();
        this.productRepository = new ProductRepository();
    }
    
    public Map<String, Object> getCart(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        
        Cart cart;
        if (cartOpt.isEmpty()) {
            // Create new cart if doesn't exist
            User user = new User();
            user.setId(userId);
            cart = cartRepository.createCartForUser(user);
        } else {
            cart = cartOpt.get();
        }
        
        return buildCartResponse(cart);
    }
    
    public Map<String, Object> addItemToCart(Long userId, Long productId, Integer quantity) {
        // Get or create cart
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        Cart cart;
        if (cartOpt.isEmpty()) {
            User user = new User();
            user.setId(userId);
            cart = cartRepository.createCartForUser(user);
        } else {
            cart = cartOpt.get();
        }
        
        // Get product
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId);
        }
        
        Product product = productOpt.get();
        
        // Check stock
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Số lượng yêu cầu vượt quá số lượng tồn kho");
        }
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartRepository.findCartItem(cart.getId(), productId);
        
        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            
            if (product.getStockQuantity() < newQuantity) {
                throw new RuntimeException("Số lượng yêu cầu vượt quá số lượng tồn kho");
            }
            
            item.setQuantity(newQuantity);
            cartRepository.saveCartItem(item);
        } else {
            // Add new item
            CartItem newItem = new CartItem(cart, product, quantity);
            cartRepository.saveCartItem(newItem);
        }
        
        // Refresh cart and return
        cart = cartRepository.findByUserId(userId).orElseThrow();
        return buildCartResponse(cart);
    }
    
    public Map<String, Object> updateCartItemQuantity(Long userId, Long productId, Integer quantity) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy giỏ hàng");
        }
        
        Cart cart = cartOpt.get();
        
        // Find cart item
        Optional<CartItem> itemOpt = cartRepository.findCartItem(cart.getId(), productId);
        if (itemOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng");
        }
        
        CartItem item = itemOpt.get();
        Product product = item.getProduct();
        
        // Check stock
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Số lượng yêu cầu vượt quá số lượng tồn kho");
        }
        
        item.setQuantity(quantity);
        cartRepository.saveCartItem(item);
        
        // Refresh cart and return
        cart = cartRepository.findByUserId(userId).orElseThrow();
        return buildCartResponse(cart);
    }
    
    public Map<String, Object> removeItemFromCart(Long userId, Long productId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy giỏ hàng");
        }
        
        Cart cart = cartOpt.get();
        
        // Find cart item
        Optional<CartItem> itemOpt = cartRepository.findCartItem(cart.getId(), productId);
        if (itemOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng");
        }
        
        CartItem item = itemOpt.get();
        cartRepository.deleteCartItem(item.getId());
        
        // Refresh cart and return
        cart = cartRepository.findByUserId(userId).orElse(cart);
        return buildCartResponse(cart);
    }
    
    private Map<String, Object> buildCartResponse(Cart cart) {
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                Product product = item.getProduct();
                BigDecimal lineTotal = product.getPrice().multiply(new BigDecimal(item.getQuantity()));
                totalPrice = totalPrice.add(lineTotal);
                
                // Get thumbnail URL from product directly (avoid lazy loading images)
                String thumbnailUrl = product.getThumbnailUrl();
                
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("cartItemId", item.getId());
                itemData.put("productId", product.getId());
                itemData.put("productName", product.getName());
                itemData.put("thumbnailUrl", thumbnailUrl);
                itemData.put("price", product.getPrice());
                itemData.put("quantity", item.getQuantity());
                itemData.put("lineTotal", lineTotal);
                
                items.add(itemData);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("totalItems", items.size());
        response.put("totalPrice", totalPrice);
        
        return response;
    }
}
