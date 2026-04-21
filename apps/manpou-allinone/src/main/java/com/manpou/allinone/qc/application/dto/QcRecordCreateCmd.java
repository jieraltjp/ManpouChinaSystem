package com.manpou.allinone.qc.application.dto;

import com.manpou.allinone.qc.domain.model.QcResult;
import com.manpou.allinone.qc.domain.model.QcType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class QcRecordCreateCmd {

    @NotNull(message = "关联采购单不能为空")
    private Long procurementId;           // 关联采购单

    @Length(max = 128)
    private String sellerName;              // 卖家名称

    @NotNull(message = "货号不能为空")
    @Length(max = 32)
    private String productCode;            // 货号

    @Length(max = 64)
    private String subProductCode;         // 子货号/颜色

    private Long qcUserId;                 // 验货负责人

    private QcType qcType;                // 验货方式

    private LocalDate qcDate;              // 验货日期

    private QcResult result;               // 验货结果

    @Positive
    private Integer inspectionCount;       // 检品数

    @Positive
    private Integer passedCount;           // 合格数量

    private Integer boxCount;              // 箱数

    private BigDecimal boxLengthCm;        // 箱子长(cm)

    private BigDecimal boxWidthCm;        // 箱子宽(cm)

    private BigDecimal boxHeightCm;       // 箱子高(cm)

    private BigDecimal netWeightPerUnit;  // 单个净重(kg)

    private BigDecimal grossWeight;        // 毛重(kg)

    private BigDecimal taxInclusivePrice;  // 含税价（元）

    @Length(max = 64)
    private String material;               // 材质

    private Boolean taxRefund;             // 是否退税

    @Length(max = 512)
    private String qcStandard;            // 验收标准

    @Length(max = 512)
    private String remarks;               // 备注

    private String images;                 // 缺陷照片URL列表（JSON数组）

    @Length(max = 128)
    private String destination;            // 目的地

    private Integer quantity;              // 订购数量

    private LocalDate orderDate;           // 下单日
}
