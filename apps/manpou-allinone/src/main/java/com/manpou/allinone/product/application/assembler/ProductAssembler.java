package com.manpou.allinone.product.application.assembler;

import com.manpou.allinone.product.application.dto.ProductCreateCmd;
import com.manpou.allinone.product.application.dto.ProductPageQuery;
import com.manpou.allinone.product.application.dto.ProductUpdateCmd;
import com.manpou.allinone.product.domain.model.ProductExample;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 */
@Component
public class ProductAssembler {

    public ProductPageQuery toDto(ProductExample entity) {
        return ProductPageQuery.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public ProductExample toEntity(ProductCreateCmd cmd) {
        ProductExample entity = new ProductExample();
        entity.rename(cmd.getName());
        return entity;
    }

    public void copyToEntity(ProductUpdateCmd cmd, ProductExample entity) {
        if (cmd.getName() != null) entity.rename(cmd.getName());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
    }
}
