package com.manpou.allinone.product.application.dto;

import com.manpou.allinone.product.domain.model.ProductCategory;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分页查询响应 DTO。
 * 包含商品目录所有展示字段。
 * 对应 docs/business/SPEC-B10-商品目录-产品管理.md §2.1。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductPageQuery {

    // ===== 货号 =====
    private Long id;
    private String masterCode;
    private String subCode;
    private String janCode;

    // ===== 名称 =====
    private String nameJa;
    private String nameEn;
    private String nameZh;

    // ===== 图片 =====
    private String imageUrl;

    // ===== 基础属性 =====
    private String colorName;
    private String material;
    private ProductCategory category;
    private String status;
    private String origin;
    private String unit;
    private Integer quantities;
    private Integer cartonQty;
    private BigDecimal amountRmb;
    private String materialJa;

    // ===== 尺寸 =====
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal volumeCbm;

    // ===== 重量 =====
    private BigDecimal netWeightKg;
    private BigDecimal grossWeightKg;

    // ===== 价格 =====
    private BigDecimal unitPriceRmb;
    private BigDecimal taxPoint;
    private BigDecimal taxRate;

    // ===== 报关 =====
    private String hsCode;
    private String hsCodeJp;
    private String declarationElements;

    // ===== 外箱 =====
    private Integer unitsPerPackage;
    private BigDecimal packageLengthCm;
    private BigDecimal packageWidthCm;
    private BigDecimal packageHeightCm;
    private BigDecimal packageVolumeCbm;
    private BigDecimal packageWeightKg;

    // ===== 仓库/质检 =====
    private String warehouse;
    private Boolean requiresQc;

    // ===== 其他 =====
    private String remarks;
    private LocalDate lastUsedDate;

    // ===== 审计 =====
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ===== 关联工厂数量（表格列展示用）=====
    private Integer factoryCount;

    // ===== 关联工厂名称（逗号分隔，表格列展示用）=====
    private String factoryNames;
}
