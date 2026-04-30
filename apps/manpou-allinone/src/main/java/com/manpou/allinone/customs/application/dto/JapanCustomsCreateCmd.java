package com.manpou.allinone.customs.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JapanCustomsCreateCmd {

    @NotBlank(message = "货柜号不能为空")
    private String containerNo;      // 货柜号（v1.4.0 必填，第一位）

    private Long domesticCustomsId;
    private Long logisticsPlanId;
    private Long procurementId;     // v1.4.0 改为可选参考
    private Long factoryId;         // v1.4.0 新增
    private String productCode;     // 货号（v1.4.0 新增）
    private String subProductCode;  // 子货号/颜色（v1.6.1 全链路追踪）

    private LocalDate arrivalDate;

    @Size(max = 128)
    private String customsBroker;

    @Size(max = 32)
    private String brokerPhone;

    @Size(max = 64)
    private String brokerContact;

    @Size(max = 64)
    private String arrivalPort;

    private BigDecimal declaredWeightKg;
    private BigDecimal declaredVolumeCbm;

    private String remarks;
}
