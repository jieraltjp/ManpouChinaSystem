package com.manpou.allinone.procurement.application.dto;

import com.manpou.allinone.procurement.domain.model.BillingType;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建发注单命令对象。
 * 与 docs/business/SPEC-B02-发注单-步骤2.md §API设计 完全对齐。
 */
@Data
public class ProcurementCreateCmd {

    // ===== 关联 =====
    private Long factoryId;            // 关联工厂ID

    // ===== 商品信息 =====
    @NotNull(message = "商品代码不能为空")
    @Length(max = 32, message = "商品代码最多 32 字符")
    private String productCode;        // 主货号

    @Length(max = 64)
    private String subProductCode;    // 子货号/枝番（颜色）

    @Length(max = 64)
    private String material;           // 材质

    private Boolean requiresQc;       // 是否需要检测

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须为正数")
    private Integer quantity;         // 订购数量

    // ===== 价格信息 =====
    @NotNull(message = "人民币单价不能为空")
    @PositiveOrZero(message = "人民币单价不能为负")
    private BigDecimal priceRmb;     // 人民币单价

    @NotNull(message = "汇率不能为空")
    @Positive(message = "汇率必须为正数")
    private BigDecimal exchangeRate;  // CNY→JPY 汇率

    @NotNull(message = "票点不能为空")
    private BigDecimal taxPoint;     // 票点（默认 1.1）

    private BillingType billingType;  // 报关类型（v1.3.0 新增）

    // ===== 报关与说明 =====
    @Length(max = 512)
    private String customsRemarks;   // 报关备注

    private String instructionManual; // 说明书

    // ===== 日期 =====
    private LocalDate orderDate;      // 下单日
    private LocalDate factoryShipDate; // 厂家出货日
    private LocalDate plannedShipDate;  // 计划出货日（交货期）
    private LocalDate actualShipDate;   // 实际出货日
    private Integer leadTimeDays;      // 交货期天数（30/45/60，默认30）
    private String cartonNotes;       // 纸箱备注（v1.9.0 新增）

    // ===== 担当 =====
    @Length(max = 64)
    private String productLead;     // 商品担当

    @Length(max = 64)
    private String japanLead;       // 日本担当

    @Length(max = 64)
    private String chinaLead;       // 中国担当

    // ===== 发货信息 =====
    @Length(max = 128)
    private String destination;      // 发送目的地

    @Length(max = 128)
    private String customerCompany;  // 客户公司

    private ShipmentStatus status;   // 状态（默认未定）
}
