package com.utephonehub.service;

import com.utephonehub.entity.Address;
import com.utephonehub.entity.User;
import com.utephonehub.repository.AddressRepository;

import java.util.*;

public class AddressService {
    
    private final AddressRepository addressRepository;
    
    public AddressService() {
        this.addressRepository = new AddressRepository();
    }
    
    public List<Map<String, Object>> getUserAddresses(Long userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Address address : addresses) {
            result.add(buildAddressResponse(address));
        }
        
        return result;
    }
    
    public Map<String, Object> createAddress(Long userId, String recipientName, String phoneNumber,
                                            String streetAddress, String province, String provinceCode,
                                            String ward, String wardCode, Boolean isDefault) {
        
        // Create address
        Address address = new Address();
        
        User user = new User();
        user.setId(userId);
        address.setUser(user);
        
        address.setRecipientName(recipientName);
        address.setPhoneNumber(phoneNumber);
        address.setStreetAddress(streetAddress);
        address.setProvince(province != null ? province : "N/A");
        address.setProvinceCode(provinceCode);
        address.setWard(ward != null && !ward.isEmpty() ? ward : "N/A"); // Ward is required - use placeholder if not provided
        address.setWardCode(wardCode);
        address.setIsDefault(isDefault != null ? isDefault : false);
        
        Address saved = addressRepository.save(address);
        
        return buildAddressResponse(saved);
    }
    
    public Map<String, Object> updateAddress(Long userId, Long addressId, String recipientName, 
                                            String phoneNumber, String streetAddress, 
                                            String province, String provinceCode,
                                            String ward, String wardCode, Boolean isDefault) {
        
        // Check if address exists and belongs to user
        Optional<Address> addressOpt = addressRepository.findByIdAndUserId(addressId, userId);
        if (addressOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy địa chỉ với ID: " + addressId);
        }
        
        Address address = addressOpt.get();
        
        // Update fields
        if (recipientName != null) {
            address.setRecipientName(recipientName);
        }
        if (phoneNumber != null) {
            address.setPhoneNumber(phoneNumber);
        }
        if (streetAddress != null) {
            address.setStreetAddress(streetAddress);
        }
        if (province != null) {
            address.setProvince(province);
        }
        if (provinceCode != null) {
            address.setProvinceCode(provinceCode);
        }
        if (ward != null) {
            address.setWard(ward);
        }
        if (wardCode != null) {
            address.setWardCode(wardCode);
        }
        if (isDefault != null) {
            address.setIsDefault(isDefault);
        }
        
        Address updated = addressRepository.save(address);
        
        return buildAddressResponse(updated);
    }
    
    public void deleteAddress(Long userId, Long addressId) {
        // Check if address exists and belongs to user
        Optional<Address> addressOpt = addressRepository.findByIdAndUserId(addressId, userId);
        if (addressOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy địa chỉ với ID: " + addressId);
        }
        
        Address address = addressOpt.get();
        
        // Check if it's the default address
        if (address.getIsDefault() != null && address.getIsDefault()) {
            throw new RuntimeException("Không thể xóa địa chỉ mặc định. Vui lòng chọn địa chỉ mặc định khác trước.");
        }
        
        addressRepository.delete(address);
    }
    
    private Map<String, Object> buildAddressResponse(Address address) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", address.getId());
        response.put("recipientName", address.getRecipientName());
        response.put("phoneNumber", address.getPhoneNumber());
        response.put("streetAddress", address.getStreetAddress());
        response.put("province", address.getProvince());
        response.put("city", address.getProvince()); // Alias for backward compatibility
        response.put("ward", address.getWard());
        response.put("wardCode", address.getWardCode());
        response.put("provinceCode", address.getProvinceCode());
        response.put("isDefault", address.getIsDefault());
        return response;
    }
}
