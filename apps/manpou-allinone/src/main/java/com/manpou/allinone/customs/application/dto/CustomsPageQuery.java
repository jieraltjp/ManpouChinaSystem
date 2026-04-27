package com.manpou.allinone.customs.application.dto;

import com.manpou.allinone.customs.domain.model.DomesticCustomsStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 国内报关记录分页查询响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomsPageQuery {

    private Long id;
    private String customsCode;
    private String containerNo;    // 货柜号（v1.3.0）
    private Long procurementId;
    private Long logisticsPlanId;
    private Long factoryId;
    private String productCode;
    private String subProductCode;
    private Integer quantity;
    private BigDecimal estimatedValueCny;
    private DomesticCustomsStatus status;
    private String remarks;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
