package com.manpou.allinone.order.domain.view;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单总览视图（JPA 只读映射 v_order_chain_v1）。
 * Phase 1 步骤 1~4：Demand → Procurement → QcRecord → LogisticsPlan。
 * 工厂+商品信息来自 procurement_snapshot（下单时刻快照）。
 *
 * <p>只读，禁止在业务代码中调用 setter 修改字段值。
 */
@Entity
@Table(name = "v_order_chain_v1")
@Data
public class OrderChainView {

    // ====== 锚点：Demand（步骤1）======
    @Id
    @Column(name = "demand_id")
    private Long demandId;

    @Column(name = "demand_code")
    private String demandCode;

    @Column(name = "demand_type")
    private String demandType;

    @Column(name = "demand_product_code")
    private String demandProductCode;

    @Column(name = "demand_sub_product_code")
    private String demandSubProductCode;

    @Column(name = "demand_quantity")
    private Integer demandQuantity;

    @Column(name = "demand_destination")
    private String demandDestination;

    @Column(name = "demand_japan_lead")
    private String demandJapanLead;

    @Column(name = "demand_status")
    private String demandStatus;

    @Column(name = "linked_procurement_id")
    private Long linkedProcurementId;

    @Column(name = "demand_image_url")
    private String demandImageUrl;

    @Column(name = "demand_create_time")
    private LocalDateTime demandCreateTime;

    @Column(name = "demand_update_time")
    private LocalDateTime demandUpdateTime;

    // ====== 步骤2：Procurement ======
    @Column(name = "procurement_id")
    private Long procurementId;

    @Column(name = "procurement_code")
    private String procurementCode;

    @Column(name = "procurement_factory_id")
    private Long procurementFactoryId;

    @Column(name = "procurement_product_code")
    private String procurementProductCode;

    @Column(name = "procurement_sub_product_code")
    private String procurementSubProductCode;

    @Column(name = "procurement_quantity")
    private Integer procurementQuantity;

    @Column(name = "procurement_price_rmb", precision = 12, scale = 4)
    private BigDecimal procurementPriceRmb;

    @Column(name = "procurement_tax_point", precision = 5, scale = 4)
    private BigDecimal procurementTaxPoint;

    @Column(name = "procurement_exchange_rate", precision = 10, scale = 4)
    private BigDecimal procurementExchangeRate;

    @Column(name = "procurement_billing_type")
    private String procurementBillingType;

    @Column(name = "procurement_estimated_price_jpy", precision = 14, scale = 2)
    private BigDecimal procurementEstimatedPriceJpy;

    @Column(name = "procurement_order_date")
    private LocalDate procurementOrderDate;

    @Column(name = "procurement_factory_ship_date")
    private LocalDate procurementFactoryShipDate;

    @Column(name = "procurement_planned_ship_date")
    private LocalDate procurementPlannedShipDate;

    @Column(name = "procurement_actual_ship_date")
    private LocalDate procurementActualShipDate;

    @Column(name = "procurement_lead_time_days")
    private Integer procurementLeadTimeDays;

    @Column(name = "procurement_product_lead")
    private String procurementProductLead;

    @Column(name = "procurement_japan_lead")
    private String procurementJapanLead;

    @Column(name = "procurement_china_lead")
    private String procurementChinaLead;

    @Column(name = "procurement_destination")
    private String procurementDestination;

    @Column(name = "procurement_customer_company")
    private String procurementCustomerCompany;

    @Column(name = "procurement_status")
    private String procurementStatus;

    @Column(name = "procurement_return_reason")
    private String procurementReturnReason;

    @Column(name = "procurement_create_time")
    private LocalDateTime procurementCreateTime;

    // ====== 工厂快照（来自 procurement_snapshot，下单时刻不变）======
    @Column(name = "snapshot_factory_id")
    private Long snapshotFactoryId;

    @Column(name = "snapshot_factory_code")
    private String snapshotFactoryCode;

    @Column(name = "snapshot_factory_name")
    private String snapshotFactoryName;

    @Column(name = "snapshot_factory_province")
    private String snapshotFactoryProvince;

    @Column(name = "snapshot_factory_city")
    private String snapshotFactoryCity;

    @Column(name = "snapshot_factory_contact_name")
    private String snapshotFactoryContactName;

    @Column(name = "snapshot_factory_contact_phone")
    private String snapshotFactoryContactPhone;

    // ====== 商品快照（来自 procurement_snapshot，下单时刻不变）======
    @Column(name = "snapshot_product_name_zh")
    private String snapshotProductNameZh;

    @Column(name = "snapshot_product_name_ja")
    private String snapshotProductNameJa;

    @Column(name = "snapshot_product_category")
    private String snapshotProductCategory;

    // ====== 步骤3：验货记录 ======
    @Column(name = "qc_record_id")
    private Long qcRecordId;

    @Column(name = "qc_code")
    private String qcCode;

    @Column(name = "qc_result")
    private String qcResult;

    @Column(name = "qc_inspection_count")
    private Integer qcInspectionCount;

    @Column(name = "qc_passed_count")
    private Integer qcPassedCount;

    @Column(name = "qc_defective_count")
    private Integer qcDefectiveCount;

    @Column(name = "qc_box_count")
    private Integer qcBoxCount;

    @Column(name = "qc_box_length_cm", precision = 8, scale = 2)
    private BigDecimal qcBoxLengthCm;

    @Column(name = "qc_box_width_cm", precision = 8, scale = 2)
    private BigDecimal qcBoxWidthCm;

    @Column(name = "qc_box_height_cm", precision = 8, scale = 2)
    private BigDecimal qcBoxHeightCm;

    @Column(name = "qc_net_weight_per_unit", precision = 10, scale = 4)
    private BigDecimal qcNetWeightPerUnit;

    @Column(name = "qc_gross_weight", precision = 10, scale = 4)
    private BigDecimal qcGrossWeight;

    @Column(name = "qc_date")
    private LocalDate qcDate;

    @Column(name = "qc_status")
    private String qcStatus;

    @Column(name = "qc_create_time")
    private LocalDateTime qcCreateTime;

    // ====== 步骤4：调配计划 ======
    @Column(name = "logistics_plan_id")
    private Long logisticsPlanId;

    @Column(name = "logistics_plan_code")
    private String logisticsPlanCode;

    @Column(name = "logistics_container_no")
    private String logisticsContainerNo;

    @Column(name = "logistics_plan_type")
    private String logisticsPlanType;

    @Column(name = "logistics_cargo_length_cm", precision = 8, scale = 2)
    private BigDecimal logisticsCargoLengthCm;

    @Column(name = "logistics_cargo_width_cm", precision = 8, scale = 2)
    private BigDecimal logisticsCargoWidthCm;

    @Column(name = "logistics_cargo_height_cm", precision = 8, scale = 2)
    private BigDecimal logisticsCargoHeightCm;

    @Column(name = "logistics_cargo_volume_cbm", precision = 10, scale = 6)
    private BigDecimal logisticsCargoVolumeCbm;

    @Column(name = "logistics_cargo_weight_kg", precision = 10, scale = 4)
    private BigDecimal logisticsCargoWeightKg;

    @Column(name = "logistics_requires_qc")
    private Boolean logisticsRequiresQc;

    @Column(name = "logistics_estimated_ship_date")
    private LocalDate logisticsEstimatedShipDate;

    @Column(name = "logistics_actual_ship_date")
    private LocalDate logisticsActualShipDate;

    @Column(name = "logistics_status")
    private String logisticsStatus;

    @Column(name = "logistics_create_time")
    private LocalDateTime logisticsCreateTime;

    // ====== 4步状态 ======
    @Column(name = "step1_status")
    private String step1Status;

    @Column(name = "step2_status")
    private String step2Status;

    @Column(name = "step3_status")
    private String step3Status;

    @Column(name = "step4_status")
    private String step4Status;

    // ====== Phase1 步骤5~8 占位（NULL）======
    @Column(name = "step5_status")
    private String step5Status;

    @Column(name = "step6_status")
    private String step6Status;

    @Column(name = "step7_status")
    private String step7Status;

    @Column(name = "step8_status")
    private String step8Status;

    // ====== Phase1 步骤5~8 字段占位 ======
    @Column(name = "domestic_customs_id")
    private Long domesticCustomsId;

    @Column(name = "domestic_customs_status")
    private String domesticCustomsStatus;

    @Column(name = "japan_customs_id")
    private Long japanCustomsId;

    @Column(name = "japan_customs_status")
    private String japanCustomsStatus;

    @Column(name = "tax_refund_id")
    private Long taxRefundId;

    @Column(name = "tax_refund_status")
    private String taxRefundStatus;

    @Column(name = "sales_record_id")
    private Long salesRecordId;

    @Column(name = "sales_status")
    private String salesStatus;
}
