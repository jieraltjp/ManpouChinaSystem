package com.manpou.allinone.procurement.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发注单分页查询响应 DTO。
 * 与 docs/business/API-发注管理.md §1.2 响应字段对齐。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcurementPageQuery {

    private Long id;
    private String productCode;         // 商品代码
    private Integer quantity;           // 订购数量
    private BigDecimal priceRmb;        // 人民币单价
    private BigDecimal exchangeRate;    // 汇率
    private BigDecimal taxPoint;        // 票点
    private BigDecimal estimatedPriceJpy; // 估算批发价 JPY
    private String billingMethod;        // 计费方式
    private LocalDate orderDate;        // 下单日
    private LocalDate factoryShipDate;  // 厂家出货日
    private LocalDate plannedShipDate;  // 计划出货日
    private String productLead;         // 商品担当
    private String japanLead;           // 日本担当
    private String chinaLead;          // 中国担当
    private String destination;         // 发送目的地
    private String customerCompany;     // 客户公司
    private ShipmentStatus status;     // 状态
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
