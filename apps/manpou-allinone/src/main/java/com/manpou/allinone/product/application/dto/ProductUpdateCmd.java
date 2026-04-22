package com.manpou.allinone.product.application.dto;

import com.manpou.allinone.product.domain.model.ProductCategory;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新商品命令对象。
 * 所有字段可选，不传的字段不更新。
 * 对应 docs/business/SPEC-B10-商品目录-产品管理.md §2.1。
 */
@Data
public class ProductUpdateCmd {

    @Length(max = 32)
    private String masterCode;

    @Length(max = 64)
    private String subCode;

    @Length(max = 128)
    private String nameJa;

    @Length(max = 255)
    private String nameEn;

    @Length(max = 255)
    private String nameZh;

    @Length(max = 512)
    private String imageUrl;

    @Length(max = 64)
    private String colorName;

    @Length(max = 64)
    private String material;

    private ProductCategory category;

    @Length(max = 100)
    private String origin;

    @Length(max = 50)
    private String unit;

    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    private BigDecimal netWeightKg;
    private BigDecimal grossWeightKg;

    private BigDecimal unitPriceRmb;
    private BigDecimal taxPoint;
    private BigDecimal taxRate;

    @Length(max = 20)
    private String hsCode;

    private String declarationElements;

    private Integer unitsPerPackage;
    private BigDecimal packageLengthCm;
    private BigDecimal packageWidthCm;
    private BigDecimal packageHeightCm;

    @Length(max = 64)
    private String warehouse;

    private Boolean requiresQc;

    @Length(max = 512)
    private String remarks;

    private LocalDate lastUsedDate;
}
