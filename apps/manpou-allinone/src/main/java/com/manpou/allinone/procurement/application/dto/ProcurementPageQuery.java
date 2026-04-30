package com.manpou.allinone.procurement.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.procurement.domain.model.BillingType;
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
 * 与 docs/business/SPEC-B02-发注单-步骤2.md §API设计 完全对齐。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcurementPageQuery {

    // ===== 关联 =====
    private Long id;
    private Long factoryId;              // 关联工厂ID
    private String factoryName;          // 关联工厂名称（只读，来自 factory 表 JOIN）
    private Long batchCount;             // 出货批次数量（Phase2：batchCount>0 → 已出货）

    // ===== 商品信息 =====
    private String productCode;         // 主货号
    private String subProductCode;     // 子货号/枝番（颜色）
    private String material;            // 材质
    private Boolean requiresQc;        // 是否需要检测
    private Integer quantity;          // 订购数量

    // ===== 价格信息 =====
    private BigDecimal priceRmb;       // 人民币单价
    private BigDecimal exchangeRate;   // 汇率
    private BigDecimal taxPoint;       // 票点
    private BillingType billingType;   // 报关类型（v1.3.0 新增）
    private BigDecimal estimatedPriceJpy; // 估算批发价 JPY

    // ===== 报关与说明 =====
    private String customsRemarks;     // 报关备注
    private String instructionManual;  // 说明书

    // ===== 日期 =====
    private LocalDate orderDate;       // 下单日
    private LocalDate factoryShipDate; // 厂家出货日
    private LocalDate plannedShipDate; // 计划出货日（交货期）
    private LocalDate actualShipDate;  // 实际出货日（v1.3.0 新增）
    private Integer leadTimeDays;      // 交货期天数（30/45/60）
    private String cartonNotes;       // 纸箱备注（v1.9.0 新增）
    private LocalDate afterSalesDeadline; // 售后截止日（v1.10.0 新增）

    // ===== 担当 =====
    private String productLead;        // 商品担当
    private String japanLead;          // 日本担当
    private String chinaLead;          // 中国担当

    // ===== 发货信息 =====
    private String destination;        // 发送目的地
    private String customerCompany;    // 客户公司

    // ===== 状态 =====
    private ShipmentStatus status;    // 状态

    // ===== 审计 =====
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
