package com.utephonehub.service;

import com.utephonehub.entity.*;
import com.utephonehub.repository.ProductRepository;
import com.utephonehub.repository.ReviewRepository;

import java.util.*;

public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    
    public ReviewService() {
        this.reviewRepository = new ReviewRepository();
        this.productRepository = new ProductRepository();
    }
    
    public Map<String, Object> getProductReviews(Long productId, int page, int limit) {
        // Check if product exists
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId);
        }
        
        List<Review> reviews = reviewRepository.findByProductId(productId, page, limit);
        long totalReviews = reviewRepository.countByProductId(productId);
        
        List<Map<String, Object>> reviewList = new ArrayList<>();
        for (Review review : reviews) {
            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("id", review.getId());
            reviewData.put("rating", review.getRating());
            reviewData.put("comment", review.getComment());
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", review.getUser().getId());
            userData.put("fullName", review.getUser().getFullName());
            reviewData.put("user", userData);
            
            int likeCount = review.getLikes() != null ? review.getLikes().size() : 0;
            reviewData.put("likeCount", likeCount);
            reviewData.put("createdAt", review.getCreatedAt());
            
            reviewList.add(reviewData);
        }
        
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) totalReviews / limit);
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("totalItems", totalReviews);
        pagination.put("totalPages", totalPages);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pagination", pagination);
        
        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviewList);
        result.put("metadata", metadata);
        
        return result;
    }
    
    public Map<String, Object> createReview(Long userId, Long productId, Integer rating, String comment) {
        // Check if product exists
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId);
        }
        
        // Check if user has already reviewed this product
        if (reviewRepository.hasUserReviewedProduct(userId, productId)) {
            throw new RuntimeException("Người dùng đã đánh giá sản phẩm này trước đó");
        }
        
        // NOTE: Người dùng đã đăng nhập có thể đánh giá KHÔNG CẦN mua hàng
        // Đã bỏ kiểm tra hasUserPurchasedProduct()
        
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Đánh giá phải từ 1 đến 5 sao");
        }
        
        // Get current user info
        User currentUser = new User();
        currentUser.setId(userId);
        
        // Create review
        Review review = new Review();
        review.setUser(currentUser);
        
        Product product = productOpt.get();
        review.setProduct(product);
        
        review.setRating(rating);
        review.setComment(comment);
        
        Review savedReview = reviewRepository.save(review);
        
        // Build response - need to fetch user info separately
        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("id", savedReview.getId());
        reviewData.put("rating", savedReview.getRating());
        reviewData.put("comment", savedReview.getComment());
        reviewData.put("likeCount", 0);
        reviewData.put("createdAt", savedReview.getCreatedAt());
        
        return reviewData;
    }
    
    public Map<String, Object> updateReview(Long userId, Long reviewId, Integer rating, String comment) {
        // Check if review exists
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy đánh giá với ID: " + reviewId);
        }
        
        Review review = reviewOpt.get();
        
        // Check if user owns this review
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa đánh giá này");
        }
        
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Đánh giá phải từ 1 đến 5 sao");
        }
        
        // Get user info BEFORE closing EntityManager
        Long reviewUserId = review.getUser().getId();
        String reviewUserFullName = review.getUser().getFullName();
        int currentLikeCount = review.getLikes() != null ? review.getLikes().size() : 0;
        
        // Update review
        review.setRating(rating);
        review.setComment(comment);
        
        Review updatedReview = reviewRepository.save(review);
        
        // Build response using data collected before save
        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("id", updatedReview.getId());
        reviewData.put("rating", updatedReview.getRating());
        reviewData.put("comment", updatedReview.getComment());
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", reviewUserId);
        userData.put("fullName", reviewUserFullName);
        reviewData.put("user", userData);
        
        reviewData.put("likeCount", currentLikeCount);
        reviewData.put("createdAt", updatedReview.getCreatedAt());
        reviewData.put("updatedAt", updatedReview.getUpdatedAt());
        
        return reviewData;
    }
    
    public void deleteReview(Long userId, Long reviewId) {
        // Check if review exists
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy đánh giá với ID: " + reviewId);
        }
        
        Review review = reviewOpt.get();
        
        // Check if user owns this review
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này");
        }
        
        // Delete review (cascade will delete likes)
        reviewRepository.delete(reviewId);
    }
    
    public Map<String, Object> likeReview(Long userId, Long reviewId) {
        // Check if review exists
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy đánh giá với ID: " + reviewId);
        }
        
        // Check if user has already liked this review
        Optional<ReviewLike> existingLike = reviewRepository.findReviewLike(reviewId, userId);
        if (existingLike.isPresent()) {
            throw new RuntimeException("Người dùng đã thích bài đánh giá này");
        }
        
        // Create like
        ReviewLike reviewLike = new ReviewLike();
        
        User user = new User();
        user.setId(userId);
        reviewLike.setUser(user);
        
        Review review = reviewOpt.get();
        reviewLike.setReview(review);
        
        reviewRepository.saveReviewLike(reviewLike);
        
        // Get updated like count
        int likeCount = reviewRepository.getLikeCount(reviewId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("likeCount", likeCount);
        
        return result;
    }
    
    public Map<String, Object> unlikeReview(Long userId, Long reviewId) {
        // Check if review exists
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy đánh giá với ID: " + reviewId);
        }
        
        // Check if user has liked this review
        Optional<ReviewLike> existingLike = reviewRepository.findReviewLike(reviewId, userId);
        if (existingLike.isEmpty()) {
            throw new RuntimeException("Người dùng chưa thích bài đánh giá này");
        }
        
        // Delete like
        reviewRepository.deleteReviewLike(existingLike.get().getId());
        
        // Get updated like count
        int likeCount = reviewRepository.getLikeCount(reviewId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("likeCount", likeCount);
        
        return result;
    }
    
    public Map<String, Object> getProductReviewStats(Long productId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get review count
        long reviewCount = reviewRepository.countByProductId(productId);
        stats.put("reviewCount", reviewCount);
        
        // Get average rating
        if (reviewCount > 0) {
            Double averageRating = reviewRepository.getAverageRating(productId);
            stats.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
        } else {
            stats.put("averageRating", 0.0);
        }
        
        return stats;
    }
}
