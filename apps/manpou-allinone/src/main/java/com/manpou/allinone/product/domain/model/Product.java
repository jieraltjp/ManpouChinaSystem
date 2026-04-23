package com.manpou.allinone.product.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 商品聚合根。
 * 对应 docs/business/SPEC-B10-商品目录-产品管理.md §2.1。
 */
@Entity
@Table(name = "product",
        uniqueConstraints = @UniqueConstraint(name = "uk_master_sub", columnNames = {"master_code", "sub_code"}),
        indexes = {
        @Index(name = "idx_master_code", columnList = "master_code"),
        @Index(name = "idx_hs_code", columnList = "hs_code"),
        @Index(name = "idx_hs_code_jp", columnList = "hs_code_jp"),
        @Index(name = "idx_name_zh", columnList = "name_zh"),
        @Index(name = "idx_create_time", columnList = "create_time")
})
@Access(AccessType.FIELD)
@Getter
@Setter
public class Product extends BaseEntity {

    // ===== 货号 =====
    @Column(name = "master_code", nullable = false, length = 32)
    private String masterCode;        // 主货号（如 odn012）

    @Column(name = "sub_code", length = 64)
    private String subCode;           // 子货号/色号（如 re=红色，可为空）

    @Column(name = "jan_code", length = 64)
    private String janCode;          // JANコード（出货单位标记）

    // ===== 多语言名称 =====
    @Column(name = "name_ja", length = 128)
    private String nameJa;            // 日文名称（日本用）

    @Column(name = "name_en", length = 255)
    private String nameEn;            // 英文名称（报关用）

    @Column(name = "name_zh", length = 255)
    private String nameZh;            // 中文名称（中国用）

    // ===== 图片 =====
    @Column(name = "image_url", length = 512)
    private String imageUrl;          // 商品图片 URL

    // ===== 基础属性 =====
    @Column(name = "color_name", length = 64)
    private String colorName;         // 颜色名称

    @Column(name = "material", length = 64)
    private String material;          // 材质

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private ProductCategory category; // OEM / ORDINARY / FACTORY_DIRECT

    @Column(name = "status", length = 32)
    private String status;           // 商品区分（通常/予約）

    @Column(name = "origin", length = 100)
    private String origin;           // 原产国

    @Column(name = "unit", length = 50)
    private String unit;              // 计量单位（个/台/套）

    @Column(name = "quantities")
    private Integer quantities;       // 数量

    @Column(name = "carton_qty")
    private Integer cartonQty;        // 箱数

    @Column(name = "amount_rmb", precision = 14, scale = 4)
    private BigDecimal amountRmb;     // 金额(RMB) = 单价 × 数量

    @Column(name = "material_ja", length = 255)
    private String materialJa;        // 材质（日文）

    // ===== 单品尺寸 =====
    @Column(name = "length_cm", precision = 8, scale = 2)
    private BigDecimal lengthCm;      // 长(cm)

    @Column(name = "width_cm", precision = 8, scale = 2)
    private BigDecimal widthCm;       // 宽(cm)

    @Column(name = "height_cm", precision = 8, scale = 2)
    private BigDecimal heightCm;      // 高(cm)

    @Column(name = "volume_cbm", precision = 10, scale = 6)
    private BigDecimal volumeCbm;     // 体积(m³)

    // ===== 重量 =====
    @Column(name = "net_weight_kg", precision = 10, scale = 4)
    private BigDecimal netWeightKg;   // 净重(kg)

    @Column(name = "gross_weight_kg", precision = 10, scale = 4)
    private BigDecimal grossWeightKg; // 毛重(kg)

    // ===== 价格 =====
    @Column(name = "unit_price_rmb", precision = 12, scale = 4)
    private BigDecimal unitPriceRmb;  // 含税单价(CNY)

    @Column(name = "tax_point", precision = 5, scale = 4)
    private BigDecimal taxPoint = new BigDecimal("1.1"); // 票点

    @Column(name = "tax_rate", precision = 5, scale = 4)
    private BigDecimal taxRate = new BigDecimal("0.1");  // 增值税率

    // ===== 报关 =====
    @Column(name = "hs_code", length = 20)
    private String hsCode;            // HS编码（中国）

    @Column(name = "hs_code_jp", length = 20)
    private String hsCodeJp;          // 日本HS编码（税番）

    @Column(name = "declaration_elements", columnDefinition = "TEXT")
    private String declarationElements; // 申报要素

    // ===== 外箱包装 =====
    @Column(name = "units_per_package")
    private Integer unitsPerPackage;  // 每箱数量

    @Column(name = "package_length_cm", precision = 8, scale = 2)
    private BigDecimal packageLengthCm;

    @Column(name = "package_width_cm", precision = 8, scale = 2)
    private BigDecimal packageWidthCm;

    @Column(name = "package_height_cm", precision = 8, scale = 2)
    private BigDecimal packageHeightCm;

    @Column(name = "package_volume_cbm", precision = 10, scale = 6)
    private BigDecimal packageVolumeCbm;

    @Column(name = "package_weight_kg", precision = 10, scale = 4)
    private BigDecimal packageWeightKg;

    // ===== 仓库/质检 =====
    @Column(name = "warehouse", length = 64)
    private String warehouse;         // 仓库归属

    @Column(name = "requires_qc")
    private Boolean requiresQc;      // 是否需要检测

    // ===== 其他 =====
    @Column(name = "remarks", length = 512)
    private String remarks;          // 备注

    @Column(name = "last_used_date")
    private java.time.LocalDate lastUsedDate; // 最近使用日期

    private static final BigDecimal CM3_TO_M3 = new BigDecimal("1000000");

    // ===== 领域方法 =====

    /** 计算单品体积(m³)：L × W × H / 1,000,000 */
    public void calculateVolume() {
        if (lengthCm != null && widthCm != null && heightCm != null) {
            this.volumeCbm = lengthCm
                    .multiply(widthCm)
                    .multiply(heightCm)
                    .divide(CM3_TO_M3, 6, RoundingMode.HALF_UP);
        }
    }

    /** 计算外箱体积(m³) */
    public void calculatePackageVolume() {
        if (packageLengthCm != null && packageWidthCm != null && packageHeightCm != null) {
            this.packageVolumeCbm = packageLengthCm
                    .multiply(packageWidthCm)
                    .multiply(packageHeightCm)
                    .divide(CM3_TO_M3, 6, RoundingMode.HALF_UP);
        }
    }

    /** 获取完整货号（masterCode-subCode）。 */
    public String getFullCode() {
        return subCode != null && !subCode.isBlank()
                ? masterCode + "-" + subCode
                : masterCode;
    }
}
