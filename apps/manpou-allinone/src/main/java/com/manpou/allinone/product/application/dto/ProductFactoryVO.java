package com.manpou.allinone.product.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.factory.domain.model.CooperationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductFactoryVO {

    // product_factory fields
    private Long productId;
    private Long factoryId;
    private String supplierSku;
    private Integer moq;
    private Integer leadTimeDays;
    private BigDecimal unitPriceRmb;
    private Boolean isPreferred;

    // factory fields (joined)
    private String factoryCode;
    private String factoryName;
    private String province;
    private String city;
    private String contactName;
    private String contactPhone;
    private CooperationStatus cooperationStatus;
}
