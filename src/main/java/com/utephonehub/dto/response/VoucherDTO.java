package com.utephonehub.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

public class VoucherDTO implements Serializable {
    private Long id;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private Integer maxUsage;
    private Long usedCount;

    public VoucherDTO(Long id, String code, String discountType, BigDecimal discountValue,
                      BigDecimal minOrderValue, Integer maxUsage, Long usedCount) {
        this.id = id;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderValue = minOrderValue;
        this.maxUsage = maxUsage;
        this.usedCount = usedCount;
    }

    public VoucherDTO(Long id) {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public Integer getMaxUsage() {
        return maxUsage;
    }

    public void setMaxUsage(Integer maxUsage) {
        this.maxUsage = maxUsage;
    }

    public Long getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Long usedCount) {
        this.usedCount = usedCount;
    }
}
