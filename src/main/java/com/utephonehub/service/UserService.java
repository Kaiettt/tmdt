package com.utephonehub.service;

import com.utephonehub.config.DatabaseConfig;
import com.utephonehub.entity.User;
import com.utephonehub.entity.Address;
import com.utephonehub.repository.UserRepository;
import com.utephonehub.repository.AddressRepository;
import com.utephonehub.util.PasswordUtil;
import com.utephonehub.util.JwtUtil;
import com.utephonehub.dto.request.LoginRequest;
import com.utephonehub.dto.request.RegisterRequest;
import com.utephonehub.dto.request.UpdateProfileRequest;
import com.utephonehub.dto.request.ChangePasswordRequest;
import com.utephonehub.dto.request.AddressRequest;
import com.utephonehub.dto.response.LoginResponse;
import com.utephonehub.dto.response.UserResponse;
import com.utephonehub.dto.response.AddressResponse;
import com.utephonehub.exception.BusinessException;
import com.utephonehub.exception.ValidationException;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * User Service
 * Xử lý business logic cho User
 */
public class UserService {
    
    private static final Logger logger = LogManager.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    
    public UserService() {
        this.userRepository = new UserRepository();
        this.addressRepository = new AddressRepository();
        this.passwordUtil = new PasswordUtil();
        this.jwtUtil = new JwtUtil();
        this.redisService = new RedisService();
    }
    
    /**
     * Đăng ký user mới
     * @param request RegisterRequest
     * @return LoginResponse với access token
     */
    public LoginResponse register(RegisterRequest request) throws RuntimeException {
        logger.info("Registering new user: {} - {}", request.getUsername(), request.getEmail());
        
        // Validate input
        validateRegisterRequest(request);
        
        // Kiểm tra username đã tồn tại chưa
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trong hệ thống");
        }
        
        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }
        
        // Tạo user mới
        User user = new User();
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordUtil.hashPassword(request.getPassword()));
        user.setRole(User.UserRole.customer);
        user.setStatus(User.UserStatus.active);
        
        // Lưu user
        user = userRepository.save(user);
        
        // Tạo access token và refresh token
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().toString());
        
        // Store refresh token in Redis (7 days TTL)
        redisService.storeRefreshToken(user.getEmail(), refreshToken);
        
        logger.info("User registered successfully: {}", user.getEmail());
        
        return new LoginResponse(
            accessToken,
            refreshToken,
            convertToUserResponse(user)
        );
    }
    
    /**
     * Đăng nhập user
     * @param request LoginRequest
     * @return LoginResponse với access token
     */
    public LoginResponse login(LoginRequest request) throws RuntimeException {
        logger.info("User login attempt: {}", request.getUsername());
        
        // Validate input
        validateLoginRequest(request);
        
        // Tìm user theo username hoặc email
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            // Nếu không tìm thấy theo username, thử tìm theo email
            userOpt = userRepository.findByEmail(request.getUsername());
        }
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không đúng");
        }
        
        User user = userOpt.get();
        
        // Kiểm tra trạng thái user
        if (user.getStatus() != User.UserStatus.active) {
            throw new RuntimeException("Tài khoản đã bị khóa hoặc chưa được kích hoạt");
        }
        
        // Kiểm tra mật khẩu
        if (!passwordUtil.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không đúng");
        }
        
        // Tạo access token và refresh token với userId và role
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().name());
        
        // Store refresh token in Redis (7 days TTL)
        redisService.storeRefreshToken(user.getEmail(), refreshToken);
        
        logger.info("User logged in successfully: {} (userId: {}, role: {})", user.getEmail(), user.getId(), user.getRole());
        
        return new LoginResponse(
            accessToken,
            refreshToken,
            convertToUserResponse(user)
        );
    }
    
    /**
     * Find or create user from Google OAuth
     * @param email User email from Google
     * @param name User full name from Google
     * @param picture User profile picture URL from Google
     * @param googleId Google user ID
     * @return LoginResponse with access token
     */
    public LoginResponse findOrCreateOAuthUser(String email, String name, String picture, String googleId) {
        logger.info("OAuth login attempt for email: {}", email);
        
        // Check if user exists by email
        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;
        
        if (userOpt.isPresent()) {
            // User exists - just login
            user = userOpt.get();
            logger.info("Existing user found for OAuth login: {} (userId: {})", email, user.getId());
        } else {
            // Create new user from OAuth
            logger.info("Creating new user from OAuth: {}", email);
            
            // Generate unique username from email
            String username = generateUsernameFromEmail(email);
            
            user = new User();
            user.setUsername(username);
            user.setFullName(name != null && !name.isEmpty() ? name : email.split("@")[0]);
            user.setEmail(email);
            // OAuth users don't have password - set a random hash (they can't login with password)
            user.setPasswordHash(passwordUtil.hashPassword(java.util.UUID.randomUUID().toString()));
            user.setRole(User.UserRole.customer);
            user.setStatus(User.UserStatus.active);
            
            user = userRepository.save(user);
            logger.info("New OAuth user created: {} (userId: {})", email, user.getId());
        }
        
        // Generate tokens
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getRole().toString());
        
        // Store refresh token in Redis
        redisService.storeRefreshToken(user.getEmail(), refreshToken);
        
        logger.info("OAuth user logged in successfully: {} (userId: {}, role: {})", user.getEmail(), user.getId(), user.getRole());
        
        return new LoginResponse(
            accessToken,
            refreshToken,
            convertToUserResponse(user)
        );
    }
    
    /**
     * Generate unique username from email for OAuth users
     */
    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0].toLowerCase();
        String username = baseUsername;
        int suffix = 1;
        
        // Keep trying until we find a unique username
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }
        
        return username;
    }
    
    /**
     * Lấy thông tin user theo ID
     * @param id User ID
     * @return UserResponse
     */
    public UserResponse getUserById(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user");
        }
        
        return convertToUserResponse(userOpt.get());
    }
    
    /**
     * Lấy thông tin user theo email
     * @param email User email
     * @return UserResponse
     */
    public UserResponse getUserByEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user");
        }
        
        return convertToUserResponse(userOpt.get());
    }
    
    /**
     * Lấy tất cả users (Admin only)
     * @return List<UserResponse>
     */
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy users với pagination (Admin only)
     * @param page Số trang
     * @param size Kích thước trang
     * @return List<UserResponse>
     */
    public List<UserResponse> getUsers(int page, int size) {
        List<User> users = userRepository.findAll(page, size);
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Cập nhật trạng thái user (Admin only)
     * @param id User ID
     * @param status Trạng thái mới
     * @return true nếu cập nhật thành công
     */
    public boolean updateUserStatus(Long id, User.UserStatus status) {
        return userRepository.updateStatus(id, status);
    }
    
    /**
     * Xóa user (Admin only)
     * @param id User ID
     * @return true nếu xóa thành công
     */
    public boolean deleteUser(Long id) {
        return userRepository.deleteById(id);
    }
    
    /**
     * Đếm tổng số users
     * @return Tổng số users
     */
    public long countUsers() {
        return userRepository.count();
    }
    
    /**
     * Validate register request
     * @param request RegisterRequest
     */
    private void validateRegisterRequest(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("validation: Tên đăng nhập không được để trống");
        }
        
        if (request.getUsername().length() < 3 || request.getUsername().length() > 50) {
            throw new RuntimeException("validation: Tên đăng nhập phải từ 3-50 ký tự");
        }
        
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new RuntimeException("validation: Họ và tên không được để trống");
        }
        
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("validation: Email không được để trống");
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("validation: Mật khẩu không được để trống");
        }
        
        if (!passwordUtil.isValidPassword(request.getPassword())) {
            throw new RuntimeException("validation: Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số");
        }
    }
    
    /**
     * Validate login request
     * @param request LoginRequest
     */
    private void validateLoginRequest(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("validation: Tên đăng nhập hoặc email không được để trống");
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("validation: Mật khẩu không được để trống");
        }
    }
    
    /**
     * Convert User entity to UserResponse DTO
     * @param user User entity
     * @return UserResponse
     */
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole().name());
        response.setStatus(user.getStatus().name());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
    
    /**
     * Cập nhật thông tin cá nhân
     * @param email Email của user
     * @param request UpdateProfileRequest
     * @return UserResponse
     */
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        logger.info("Updating profile for user: {}", email);
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user");
        }
        
        User user = userOpt.get();
        
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        
        logger.info("Profile updated successfully for user: {}", email);
        return convertToUserResponse(user);
    }
    
    /**
     * Đổi mật khẩu
     * @param email Email của user
     * @param request ChangePasswordRequest
     */
    public void changePassword(String email, ChangePasswordRequest request) {
        logger.info("Changing password for user: {}", email);
        
        if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu cũ không được để trống");
        }
        
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu mới không được để trống");
        }
        
        if (!passwordUtil.isValidPassword(request.getNewPassword())) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số");
        }
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user");
        }
        
        User user = userOpt.get();
        
        // Verify old password
        if (!passwordUtil.verifyPassword(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu cũ không đúng");
        }
        
        // Update password
        user.setPasswordHash(passwordUtil.hashPassword(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        logger.info("Password changed successfully for user: {}", email);
    }
    
    /**
     * Lấy danh sách địa chỉ
     * @param email Email của user
     * @return List<AddressResponse>
     */
    public List<AddressResponse> getAddresses(String email) {
        logger.info("Getting addresses for user: {}", email);
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user");
        }
        
        User user = userOpt.get();
        List<Address> addresses = addressRepository.findByUser(user);
        
        return addresses.stream()
                .map(this::convertToAddressResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Thêm địa chỉ mới
     * @param email Email của user
     * @param request AddressRequest
     * @return AddressResponse
     */
    public AddressResponse addAddress(String email, AddressRequest request) {
        logger.info("Adding new address for user: {}", email);
        
        validateAddressRequest(request);
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user");
        }
        
        User user = userOpt.get();
        
        // Get existing addresses
        List<Address> existingAddresses = addressRepository.findByUser(user);
        
        // Auto set as default if this is the first address
        boolean shouldBeDefault = request.isDefault() || existingAddresses.isEmpty();
        
        // If this is set as default, unset other defaults
        if (shouldBeDefault) {
            for (Address addr : existingAddresses) {
                if (addr.getIsDefault()) {
                    addr.setIsDefault(false);
                    addressRepository.update(addr);
                }
            }
        }
        
        Address address = new Address();
        address.setUser(user);
        address.setRecipientName(request.getRecipientName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setStreetAddress(request.getStreetAddress());
        address.setProvince(request.getProvince());
        address.setProvinceCode(request.getProvinceCode());
        address.setWard(request.getWard());
        address.setWardCode(request.getWardCode());
        address.setIsDefault(shouldBeDefault);
        
        address = addressRepository.save(address);
        
        logger.info("Address added successfully for user: {} (default: {})", email, shouldBeDefault);
        return convertToAddressResponse(address);
    }
    
    /**
     * Cập nhật địa chỉ
     * @param email Email của user
     * @param addressId ID của địa chỉ
     * @param request AddressRequest
     * @return AddressResponse
     */
    public AddressResponse updateAddress(String email, int addressId, AddressRequest request) {
        logger.info("Updating address {} for user: {}", addressId, email);
        
        validateAddressRequest(request);
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user");
        }
        
        User user = userOpt.get();
        
        Optional<Address> addressOpt = addressRepository.findById((long) addressId);
        if (addressOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy địa chỉ");
        }
        
        Address address = addressOpt.get();
        
        // Check ownership
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật địa chỉ này");
        }
        
        // If this is set as default, unset other defaults
        if (request.isDefault() && !address.getIsDefault()) {
            List<Address> existingAddresses = addressRepository.findByUser(user);
            for (Address addr : existingAddresses) {
                if (addr.getIsDefault() && !addr.getId().equals(address.getId())) {
                    addr.setIsDefault(false);
                    addressRepository.update(addr);
                }
            }
        }
        
        address.setRecipientName(request.getRecipientName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setStreetAddress(request.getStreetAddress());
        address.setProvince(request.getProvince());
        address.setProvinceCode(request.getProvinceCode());
        address.setWard(request.getWard());
        address.setWardCode(request.getWardCode());
        address.setIsDefault(request.isDefault());
        address.setUpdatedAt(LocalDateTime.now());
        
        address = addressRepository.update(address);
        
        logger.info("Address updated successfully for user: {}", email);
        return convertToAddressResponse(address);
    }
    
    /**
     * Xóa địa chỉ
     * @param email Email của user
     * @param addressId ID của địa chỉ
     */
    public void deleteAddress(String email, int addressId) {
        logger.info("Deleting address {} for user: {}", addressId, email);
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user");
        }
        
        User user = userOpt.get();
        
        Optional<Address> addressOpt = addressRepository.findById((long) addressId);
        if (addressOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy địa chỉ");
        }
        
        Address address = addressOpt.get();
        
        // Check ownership
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa địa chỉ này");
        }
        
        boolean wasDefault = address.getIsDefault();
        
        // Delete the address
        addressRepository.delete(address);
        
        // If deleted address was default, set another address as default
        if (wasDefault) {
            List<Address> remainingAddresses = addressRepository.findByUser(user);
            if (!remainingAddresses.isEmpty()) {
                // Set first remaining address as default
                Address firstAddress = remainingAddresses.get(0);
                firstAddress.setIsDefault(true);
                addressRepository.update(firstAddress);
                logger.info("Set address {} as default after deleting default address", firstAddress.getId());
            }
        }
        
        logger.info("Address deleted successfully for user: {}", email);
    }
    
    /**
     * Validate address request
     * @param request AddressRequest
     */
    private void validateAddressRequest(AddressRequest request) {
        if (request.getRecipientName() == null || request.getRecipientName().trim().isEmpty()) {
            throw new RuntimeException("Tên người nhận không được để trống");
        }
        
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }
        
        if (request.getStreetAddress() == null || request.getStreetAddress().trim().isEmpty()) {
            throw new RuntimeException("Địa chỉ không được để trống");
        }
        
        if (request.getProvince() == null || request.getProvince().trim().isEmpty()) {
            throw new RuntimeException("Tỉnh/Thành phố không được để trống");
        }
        
        if (request.getWard() == null || request.getWard().trim().isEmpty()) {
            throw new RuntimeException("Xã/Phường không được để trống");
        }
    }
    
    /**
     * Check if email exists
     * @param email Email to check
     * @return true if email exists
     */
    public boolean isEmailExists(String email) {
        try {
            return userRepository.findByEmail(email).isPresent();
        } catch (Exception e) {
            logger.error("Error checking if email exists: {}", email, e);
            return false;
        }
    }
    
    /**
     * Reset password for user
     * @param email User email
     * @param newPassword New password
     * @return true if success
     */
    public boolean resetPassword(String email, String newPassword) {
        try {
            System.out.println("=== UserService.resetPassword() ===");
            System.out.println("Email: " + email);
            System.out.println("New password length: " + newPassword.length());
            
            // Find user by email
            Optional<User> optionalUser = userRepository.findByEmail(email);
            System.out.println("User found: " + optionalUser.isPresent());
            
            if (!optionalUser.isPresent()) {
                logger.warn("User not found with email: {}", email);
                return false;
            }
            
            User user = optionalUser.get();
            System.out.println("User ID: " + user.getId());
            
            // Hash new password
            String hashedPassword = passwordUtil.hashPassword(newPassword);
            System.out.println("Password hashed, length: " + hashedPassword.length());
            
            user.setPasswordHash(hashedPassword);
            user.setUpdatedAt(LocalDateTime.now());
            
            // Update user
            System.out.println("Saving user...");
            User updatedUser = userRepository.save(user);
            System.out.println("User saved: " + (updatedUser != null));
            
            logger.info("Password reset successfully for user: {}", email);
            return updatedUser != null;
            
        } catch (Exception e) {
            System.out.println("Exception in resetPassword: " + e.getMessage());
            e.printStackTrace();
            logger.error("Error resetting password for user: {}", email, e);
            return false;
        }
    }
    
    /**
     * Convert Address entity to AddressResponse DTO
     * @param address Address entity
     * @return AddressResponse
     */
    private AddressResponse convertToAddressResponse(Address address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId().intValue());
        response.setRecipientName(address.getRecipientName());
        response.setPhoneNumber(address.getPhoneNumber());
        response.setStreetAddress(address.getStreetAddress());
        response.setProvince(address.getProvince());
        response.setProvinceCode(address.getProvinceCode());
        response.setWard(address.getWard());
        response.setWardCode(address.getWardCode());
        response.setDefault(address.getIsDefault());
        return response;
    }
}