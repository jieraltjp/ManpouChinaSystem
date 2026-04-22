package com.manpou.allinone.product.application.dto;

import com.manpou.allinone.product.domain.model.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建商品命令对象。
 * 对应 docs/business/SPEC-B10-商品目录-产品管理.md §2.1。
 */
@Data
public class ProductCreateCmd {

    // ===== 货号 =====
    @NotBlank(message = "主货号不能为空")
    @Size(max = 32, message = "主货号最多 32 字符")
    private String masterCode;        // 主货号

    @Size(max = 64, message = "子货号最多 64 字符")
    private String subCode;           // 子货号/色号

    // ===== 多语言名称 =====
    @Size(max = 128)
    private String nameJa;            // 日文名称

    @Size(max = 255)
    private String nameEn;            // 英文名称

    @Size(max = 255)
    private String nameZh;            // 中文名称

    // ===== 图片 =====
    @Size(max = 512)
    private String imageUrl;          // 商品图片 URL

    // ===== 基础属性 =====
    @Size(max = 64)
    private String colorName;         // 颜色名称

    @Size(max = 64)
    private String material;          // 材质

    private ProductCategory category; // OEM / ORDINARY / FACTORY_DIRECT

    @Size(max = 100)
    private String origin;           // 原产国

    @Size(max = 50)
    private String unit;             // 计量单位

    // ===== 单品尺寸 =====
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    // ===== 重量 =====
    private BigDecimal netWeightKg;   // 净重
    private BigDecimal grossWeightKg; // 毛重

    // ===== 价格 =====
    private BigDecimal unitPriceRmb;  // 含税单价
    private BigDecimal taxPoint;       // 票点（默认 1.1）
    private BigDecimal taxRate;       // 增值税率（默认 0.1）

    // ===== 报关 =====
    @Size(max = 20)
    private String hsCode;            // HS编码

    private String declarationElements; // 申报要素

    // ===== 外箱包装 =====
    private Integer unitsPerPackage;
    private BigDecimal packageLengthCm;
    private BigDecimal packageWidthCm;
    private BigDecimal packageHeightCm;

    // ===== 仓库/质检 =====
    @Size(max = 64)
    private String warehouse;         // 仓库归属

    private Boolean requiresQc;     // 是否需要检测

    // ===== 其他 =====
    @Length(max = 512)
    private String remarks;          // 备注

    private LocalDate lastUsedDate;  // 最近使用日期
}
