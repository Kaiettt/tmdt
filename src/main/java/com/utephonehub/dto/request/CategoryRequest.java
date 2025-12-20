package com.utephonehub.dto.request;

/**
 * DTO cho request tạo/cập nhật category
 */
public class CategoryRequest {
    private String name;
    private String description;
    private Long parentId;
    
    public CategoryRequest() {
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
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
