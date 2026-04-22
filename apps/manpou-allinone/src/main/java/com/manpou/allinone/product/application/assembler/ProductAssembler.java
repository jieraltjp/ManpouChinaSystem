package com.manpou.allinone.product.application.assembler;

import com.manpou.allinone.product.application.dto.ProductCreateCmd;
import com.manpou.allinone.product.application.dto.ProductPageQuery;
import com.manpou.allinone.product.application.dto.ProductUpdateCmd;
import com.manpou.allinone.product.domain.model.Product;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 * 对应 docs/business/SPEC-B10-商品目录-产品管理.md §2.1。
 */
@Component
public class ProductAssembler {

    public ProductPageQuery toDto(Product entity) {
        return ProductPageQuery.builder()
                .id(entity.getId())
                .masterCode(entity.getMasterCode())
                .subCode(entity.getSubCode())
                .nameJa(entity.getNameJa())
                .nameEn(entity.getNameEn())
                .nameZh(entity.getNameZh())
                .imageUrl(entity.getImageUrl())
                .colorName(entity.getColorName())
                .material(entity.getMaterial())
                .category(entity.getCategory())
                .origin(entity.getOrigin())
                .unit(entity.getUnit())
                .lengthCm(entity.getLengthCm())
                .widthCm(entity.getWidthCm())
                .heightCm(entity.getHeightCm())
                .volumeCbm(entity.getVolumeCbm())
                .netWeightKg(entity.getNetWeightKg())
                .grossWeightKg(entity.getGrossWeightKg())
                .unitPriceRmb(entity.getUnitPriceRmb())
                .taxPoint(entity.getTaxPoint())
                .taxRate(entity.getTaxRate())
                .hsCode(entity.getHsCode())
                .declarationElements(entity.getDeclarationElements())
                .unitsPerPackage(entity.getUnitsPerPackage())
                .packageLengthCm(entity.getPackageLengthCm())
                .packageWidthCm(entity.getPackageWidthCm())
                .packageHeightCm(entity.getPackageHeightCm())
                .packageVolumeCbm(entity.getPackageVolumeCbm())
                .packageWeightKg(entity.getPackageWeightKg())
                .warehouse(entity.getWarehouse())
                .requiresQc(entity.getRequiresQc())
                .remarks(entity.getRemarks())
                .lastUsedDate(entity.getLastUsedDate())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public Product toEntity(ProductCreateCmd cmd) {
        Product entity = new Product();
        copyToEntity(cmd, entity);
        return entity;
    }

    public void copyToEntity(ProductCreateCmd cmd, Product entity) {
        entity.setMasterCode(cmd.getMasterCode());
        if (cmd.getSubCode() != null) entity.setSubCode(cmd.getSubCode());
        if (cmd.getNameJa() != null) entity.setNameJa(cmd.getNameJa());
        if (cmd.getNameEn() != null) entity.setNameEn(cmd.getNameEn());
        if (cmd.getNameZh() != null) entity.setNameZh(cmd.getNameZh());
        if (cmd.getImageUrl() != null) entity.setImageUrl(cmd.getImageUrl());
        if (cmd.getColorName() != null) entity.setColorName(cmd.getColorName());
        if (cmd.getMaterial() != null) entity.setMaterial(cmd.getMaterial());
        if (cmd.getCategory() != null) entity.setCategory(cmd.getCategory());
        if (cmd.getOrigin() != null) entity.setOrigin(cmd.getOrigin());
        if (cmd.getUnit() != null) entity.setUnit(cmd.getUnit());
        if (cmd.getLengthCm() != null) entity.setLengthCm(cmd.getLengthCm());
        if (cmd.getWidthCm() != null) entity.setWidthCm(cmd.getWidthCm());
        if (cmd.getHeightCm() != null) entity.setHeightCm(cmd.getHeightCm());
        if (cmd.getNetWeightKg() != null) entity.setNetWeightKg(cmd.getNetWeightKg());
        if (cmd.getGrossWeightKg() != null) entity.setGrossWeightKg(cmd.getGrossWeightKg());
        if (cmd.getUnitPriceRmb() != null) entity.setUnitPriceRmb(cmd.getUnitPriceRmb());
        if (cmd.getTaxPoint() != null) entity.setTaxPoint(cmd.getTaxPoint());
        if (cmd.getTaxRate() != null) entity.setTaxRate(cmd.getTaxRate());
        if (cmd.getHsCode() != null) entity.setHsCode(cmd.getHsCode());
        if (cmd.getDeclarationElements() != null) entity.setDeclarationElements(cmd.getDeclarationElements());
        if (cmd.getUnitsPerPackage() != null) entity.setUnitsPerPackage(cmd.getUnitsPerPackage());
        if (cmd.getPackageLengthCm() != null) entity.setPackageLengthCm(cmd.getPackageLengthCm());
        if (cmd.getPackageWidthCm() != null) entity.setPackageWidthCm(cmd.getPackageWidthCm());
        if (cmd.getPackageHeightCm() != null) entity.setPackageHeightCm(cmd.getPackageHeightCm());
        if (cmd.getWarehouse() != null) entity.setWarehouse(cmd.getWarehouse());
        if (cmd.getRequiresQc() != null) entity.setRequiresQc(cmd.getRequiresQc());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getLastUsedDate() != null) entity.setLastUsedDate(cmd.getLastUsedDate());
        // 自动计算体积
        entity.calculateVolume();
        entity.calculatePackageVolume();
    }

    public void copyToEntity(ProductUpdateCmd cmd, Product entity) {
        if (cmd.getMasterCode() != null) entity.setMasterCode(cmd.getMasterCode());
        if (cmd.getSubCode() != null) entity.setSubCode(cmd.getSubCode());
        if (cmd.getNameJa() != null) entity.setNameJa(cmd.getNameJa());
        if (cmd.getNameEn() != null) entity.setNameEn(cmd.getNameEn());
        if (cmd.getNameZh() != null) entity.setNameZh(cmd.getNameZh());
        if (cmd.getImageUrl() != null) entity.setImageUrl(cmd.getImageUrl());
        if (cmd.getColorName() != null) entity.setColorName(cmd.getColorName());
        if (cmd.getMaterial() != null) entity.setMaterial(cmd.getMaterial());
        if (cmd.getCategory() != null) entity.setCategory(cmd.getCategory());
        if (cmd.getOrigin() != null) entity.setOrigin(cmd.getOrigin());
        if (cmd.getUnit() != null) entity.setUnit(cmd.getUnit());
        if (cmd.getLengthCm() != null) entity.setLengthCm(cmd.getLengthCm());
        if (cmd.getWidthCm() != null) entity.setWidthCm(cmd.getWidthCm());
        if (cmd.getHeightCm() != null) entity.setHeightCm(cmd.getHeightCm());
        if (cmd.getNetWeightKg() != null) entity.setNetWeightKg(cmd.getNetWeightKg());
        if (cmd.getGrossWeightKg() != null) entity.setGrossWeightKg(cmd.getGrossWeightKg());
        if (cmd.getUnitPriceRmb() != null) entity.setUnitPriceRmb(cmd.getUnitPriceRmb());
        if (cmd.getTaxPoint() != null) entity.setTaxPoint(cmd.getTaxPoint());
        if (cmd.getTaxRate() != null) entity.setTaxRate(cmd.getTaxRate());
        if (cmd.getHsCode() != null) entity.setHsCode(cmd.getHsCode());
        if (cmd.getDeclarationElements() != null) entity.setDeclarationElements(cmd.getDeclarationElements());
        if (cmd.getUnitsPerPackage() != null) entity.setUnitsPerPackage(cmd.getUnitsPerPackage());
        if (cmd.getPackageLengthCm() != null) entity.setPackageLengthCm(cmd.getPackageLengthCm());
        if (cmd.getPackageWidthCm() != null) entity.setPackageWidthCm(cmd.getPackageWidthCm());
        if (cmd.getPackageHeightCm() != null) entity.setPackageHeightCm(cmd.getPackageHeightCm());
        if (cmd.getWarehouse() != null) entity.setWarehouse(cmd.getWarehouse());
        if (cmd.getRequiresQc() != null) entity.setRequiresQc(cmd.getRequiresQc());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getLastUsedDate() != null) entity.setLastUsedDate(cmd.getLastUsedDate());
        entity.calculateVolume();
        entity.calculatePackageVolume();
    }
}
