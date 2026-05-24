package com.manpou.allinone.product.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cargo_size")
@Getter
@Setter
public class CargoSize extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "master_code", nullable = false, length = 32)
    private String masterCode;

    @Column(name = "sub_code", length = 64)
    private String subCode;

    @Column(name = "code", nullable = false, unique = true, length = 96)
    private String code;

    @Column(name = "legacy_id")
    private Long legacyId;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "input_user", length = 64)
    private String inputUser;

    // 尺寸字段
    @Column(name = "length_cm", precision = 8, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 8, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 8, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "net_weight_kg", precision = 10, scale = 4)
    private BigDecimal netWeightKg;

    // 包材字段
    @Column(name = "pack_height_cm", precision = 8, scale = 2)
    private BigDecimal packHeightCm;

    @Column(name = "pack_width_cm", precision = 8, scale = 2)
    private BigDecimal packWidthCm;

    @Column(name = "pack_depth_cm", precision = 8, scale = 2)
    private BigDecimal packDepthCm;

    @Column(name = "package_weight_kg", precision = 10, scale = 4)
    private BigDecimal packageWeightKg;

    @Column(name = "units_per_package")
    private Integer unitsPerPackage;

    // 状态
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CargoSizeStatus status = CargoSizeStatus.PENDING;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "promoted_by", length = 64)
    private String promotedBy;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    @Column(name = "remarks", length = 512)
    private String remarks;

    @Column(name = "show_flag", nullable = false, length = 10)
    private String showFlag = "0";

    // 体积计算
    public BigDecimal calculateVolume() {
        if (lengthCm != null && widthCm != null && heightCm != null
                && lengthCm.compareTo(BigDecimal.ZERO) > 0
                && widthCm.compareTo(BigDecimal.ZERO) > 0
                && heightCm.compareTo(BigDecimal.ZERO) > 0) {
            return lengthCm.multiply(widthCm).multiply(heightCm)
                    .divide(java.math.BigDecimal.valueOf(1000000), 6, java.math.RoundingMode.HALF_UP);
        }
        return null;
    }

    public BigDecimal calculatePackageVolume() {
        if (packHeightCm != null && packWidthCm != null && packDepthCm != null
                && packHeightCm.compareTo(BigDecimal.ZERO) > 0
                && packWidthCm.compareTo(BigDecimal.ZERO) > 0
                && packDepthCm.compareTo(BigDecimal.ZERO) > 0) {
            return packHeightCm.multiply(packWidthCm).multiply(packDepthCm)
                    .divide(java.math.BigDecimal.valueOf(1000000), 6, java.math.RoundingMode.HALF_UP);
        }
        return null;
    }

    /** 升格为商品 */
    public void promote(Long productId, String operatorName) {
        this.status = CargoSizeStatus.PROMOTED;
        this.productId = productId;
        this.promotedBy = operatorName;
        this.promotedAt = LocalDateTime.now();
    }

    /** 废弃 */
    public void discard() {
        this.status = CargoSizeStatus.DISCARDED;
    }

    /** 是否为软删除 */
    public boolean isSoftDeleted() {
        return "1".equals(showFlag);
    }

    /** 软删除 */
    public void markDeleted() {
        this.showFlag = "1";
    }
}
