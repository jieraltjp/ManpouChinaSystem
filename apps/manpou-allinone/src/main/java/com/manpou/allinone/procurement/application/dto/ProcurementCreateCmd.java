package com.manpou.allinone.procurement.application.dto;

import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建发注单命令对象。
 * 与 docs/business/API-发注管理.md §1.1 完全对齐。
 */
@Data
public class ProcurementCreateCmd {

    @NotBlank(message = "商品代码不能为空")
    @Length(max = 32, message = "商品代码最多 32 字符")
    private String productCode;          // 商品代码

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须为正数")
    private Integer quantity;            // 订购数量

    @NotNull(message = "人民币单价不能为空")
    @PositiveOrZero(message = "人民币单价不能为负")
    private BigDecimal priceRmb;         // 人民币单价

    @NotNull(message = "汇率不能为空")
    @Positive(message = "汇率必须为正数")
    private BigDecimal exchangeRate;     // CNY→JPY 汇率

    @NotNull(message = "票点不能为空")
    private BigDecimal taxPoint;        // 票点（默认 1.1）

    private String billingMethod;        // 计费方式（可选）

    private LocalDate orderDate;        // 下单日

    private LocalDate factoryShipDate;   // 厂家出货日

    private LocalDate plannedShipDate;   // 计划出货日

    @Length(max = 64, message = "商品担当最多 64 字符")
    private String productLead;         // 商品担当

    @Length(max = 64, message = "日本担当最多 64 字符")
    private String japanLead;           // 日本担当

    @Length(max = 64, message = "中国担当最多 64 字符")
    private String chinaLead;          // 中国担当

    @Length(max = 128, message = "发送目的地最多 128 字符")
    private String destination;          // 发送目的地

    @Length(max = 128, message = "客户公司最多 128 字符")
    private String customerCompany;     // 客户公司

    private ShipmentStatus status;     // 状态（默认未定，可选）
}
