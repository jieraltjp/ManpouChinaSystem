package com.manpou.allinone.order.domain.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单总览视图实体（Phase1 步骤1~4）。
 * 映射 MySQL 视图 v_order_chain_v1（只读）。
 * 所有字段来自 LEFT JOIN 结果，无关联关系。
 */
@Entity
@Table(name = "v_order_chain_v1")
@Data
public class OrderChainView {

    @Id
    private Long demandId;

    // ====== 锚点：Demand（步骤1）======
    private String demandCode;
    private String demandType;
    private String demandProductCode;
    private String demandSubProductCode;
    private Integer demandQuantity;
    private String demandDestination;
    private String demandJapanLead;
    private String demandStatus;
    private Long linkedProcurementId;
    private String demandImageUrl;
    private LocalDateTime demandCreateTime;
    private LocalDateTime demandUpdateTime;

    // ====== 步骤2：Procurement ======
    private Long procurementId;
    private String procurementCode;
    private Long procurementFactoryId;
    private String procurementProductCode;
    private String procurementSubProductCode;
    private Integer procurementQuantity;
    private BigDecimal procurementPriceRmb;
    private BigDecimal procurementTaxPoint;
    private BigDecimal procurementExchangeRate;
    private String procurementBillingType;
    private BigDecimal procurementEstimatedPriceJpy;
    private LocalDate procurementOrderDate;
    private LocalDate procurementFactoryShipDate;
    private LocalDate procurementPlannedShipDate;
    private LocalDate procurementActualShipDate;
    private Integer procurementLeadTimeDays;
    private String procurementProductLead;
    private String procurementJapanLead;
    private String procurementChinaLead;
    private String procurementDestination;
    private String procurementCustomerCompany;
    private String procurementStatus;
    private LocalDateTime procurementCreateTime;

    // ====== 步骤2：关联工厂 ======
    private Long factoryId;
    private String factoryCode;
    private String factoryName;
    private String factoryProvince;
    private String factoryCity;
    private String factoryCounty;
    private String factoryContactName;
    private String factoryContactPhone;

    // ====== 步骤3：验货记录 ======
    private Long qcRecordId;
    private String qcCode;
    private String qcResult;
    private Integer qcInspectionCount;
    private Integer qcPassedCount;
    private Integer qcDefectiveCount;
    private Integer qcBoxCount;
    private BigDecimal qcBoxLengthCm;
    private BigDecimal qcBoxWidthCm;
    private BigDecimal qcBoxHeightCm;
    private BigDecimal qcNetWeightPerUnit;
    private BigDecimal qcGrossWeight;
    private LocalDate qcDate;
    private String qcStatus;
    private LocalDateTime qcCreateTime;

    // ====== 步骤4：调配计划 ======
    private Long logisticsPlanId;
    private String logisticsPlanCode;
    private String logisticsContainerNo;
    private String logisticsPlanType;
    private BigDecimal logisticsCargoLengthCm;
    private BigDecimal logisticsCargoWidthCm;
    private BigDecimal logisticsCargoHeightCm;
    private BigDecimal logisticsCargoVolumeCbm;
    private BigDecimal logisticsCargoWeightKg;
    private Boolean logisticsRequiresQc;
    private LocalDate logisticsEstimatedShipDate;
    private LocalDate logisticsActualShipDate;
    private String logisticsStatus;
    private LocalDateTime logisticsCreateTime;

    // ====== 商品基础信息 ======
    private String productNameZh;
    private String productNameJa;
    private String productCategory;

    // ====== 4步状态（Phase1）======
    @Column(name = "step1_status")
    private String step1Status;
    @Column(name = "step2_status")
    private String step2Status;
    @Column(name = "step3_status")
    private String step3Status;
    @Column(name = "step4_status")
    private String step4Status;
}
