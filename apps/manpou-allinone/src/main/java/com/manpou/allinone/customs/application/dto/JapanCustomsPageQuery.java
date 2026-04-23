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
    private Long procurementId;
    private Long domesticCustomsId;
    private Long logisticsPlanId;
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
