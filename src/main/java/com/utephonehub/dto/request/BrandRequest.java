package com.utephonehub.dto.request;

/**
 * DTO cho request tạo/cập nhật brand
 */
public class BrandRequest {
    private String name;
    private String description;
    private String logoUrl;
    
    public BrandRequest() {
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getLogoUrl() {
        return logoUrl;
    }
    
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
