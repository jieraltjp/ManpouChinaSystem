package com.manpou.allinone.procurement.application.dto;

import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新发注单命令对象（部分更新）。
 * 与 docs/business/API-发注管理.md §1.4 完全对齐。
 */
@Data
public class ProcurementUpdateCmd {

    @Length(max = 32, message = "商品代码最多 32 字符")
    private String productCode;

    @Positive(message = "数量必须为正数")
    private Integer quantity;

    @PositiveOrZero(message = "人民币单价不能为负")
    private BigDecimal priceRmb;

    @Positive(message = "汇率必须为正数")
    private BigDecimal exchangeRate;

    private BigDecimal taxPoint;

    private String billingMethod;

    private LocalDate orderDate;

    private LocalDate factoryShipDate;

    private LocalDate plannedShipDate;

    @Length(max = 64)
    private String productLead;

    @Length(max = 64)
    private String japanLead;

    @Length(max = 64)
    private String chinaLead;

    @Length(max = 128)
    private String destination;

    @Length(max = 128)
    private String customerCompany;

    private ShipmentStatus status;     // 状态推进
}
