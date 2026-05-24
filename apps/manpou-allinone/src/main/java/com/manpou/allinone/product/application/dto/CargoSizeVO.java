package com.manpou.allinone.product.application.dto;

import com.manpou.allinone.product.domain.model.CargoSize;
import com.manpou.allinone.product.domain.model.CargoSizeStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CargoSizeVO {
    private Long id;
    private String masterCode;
    private String subCode;
    private String code;
    private Long legacyId;
    private LocalDateTime updateTime;
    private String inputUser;

    // 尺寸
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal netWeightKg;

    // 包材
    private BigDecimal packHeightCm;
    private BigDecimal packWidthCm;
    private BigDecimal packDepthCm;
    private BigDecimal packageWeightKg;
    private Integer unitsPerPackage;

    // 体积
    private BigDecimal volumeCbm;

    // 状态
    private CargoSizeStatus status;
    private Long productId;
    private String promotedBy;
    private LocalDateTime promotedAt;
    private String remarks;
    private String showFlag;

    private LocalDateTime createTime;
    private LocalDateTime updateTime2;

    public static CargoSizeVO from(CargoSize entity) {
        if (entity == null) return null;
        CargoSizeVO vo = new CargoSizeVO();
        vo.setId(entity.getId());
        vo.setMasterCode(entity.getMasterCode());
        vo.setSubCode(entity.getSubCode());
        vo.setCode(entity.getCode());
        vo.setLegacyId(entity.getLegacyId());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setInputUser(entity.getInputUser());
        vo.setLengthCm(entity.getLengthCm());
        vo.setWidthCm(entity.getWidthCm());
        vo.setHeightCm(entity.getHeightCm());
        vo.setNetWeightKg(entity.getNetWeightKg());
        vo.setPackHeightCm(entity.getPackHeightCm());
        vo.setPackWidthCm(entity.getPackWidthCm());
        vo.setPackDepthCm(entity.getPackDepthCm());
        vo.setPackageWeightKg(entity.getPackageWeightKg());
        vo.setUnitsPerPackage(entity.getUnitsPerPackage());
        vo.setVolumeCbm(entity.calculateVolume());
        vo.setStatus(entity.getStatus());
        vo.setProductId(entity.getProductId());
        vo.setPromotedBy(entity.getPromotedBy());
        vo.setPromotedAt(entity.getPromotedAt());
        vo.setRemarks(entity.getRemarks());
        vo.setShowFlag(entity.getShowFlag());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime2(entity.getUpdateTime());
        return vo;
    }
}
