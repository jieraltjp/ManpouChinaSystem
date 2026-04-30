package com.manpou.allinone.customs.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class JapanCustomsPageQuery {

    private Long id;
    private String customsEntryNo;
    private String containerNo;    // 货柜号（v1.4.0 核心字段）
    private Long domesticCustomsId;
    private Long procurementId;    // v1.4.0 可选参考
    private Long logisticsPlanId;
    private String productCode;    // 货号（v1.4.0 新增）
    private String subProductCode; // 子货号/颜色（v1.6.1 全链路追踪）
    private String status;
    private LocalDate arrivalDate;
    private String customsBroker;
    private String brokerPhone;
    private String brokerContact;
    private BigDecimal importDutyPaid;
    private BigDecimal consumptionTaxPaid;
    private LocalDate clearanceDate;
    private String arrivalPort;
    private BigDecimal declaredWeightKg;
    private BigDecimal declaredVolumeCbm;
    private String remarks;
    private String createBy;
    private String createTime;
    private String updateTime;
}
