package com.manpou.allinone.order.domain.view;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单总览视图（JPA 只读映射 v_order_chain_v2）。
 * 与 SPEC-B00 业务流程对齐，共 9 步：
 *   Step1: Demand → Step2: Procurement → Step3: ShipmentBatch
 *   Step4: QcRecord → Step5: LogisticsPlan → Step6: DomesticCustoms
 *   Step7: JapanCustoms → Step8: TaxRefund → Step9: SalesRecord
 * 工厂+商品信息来自 procurement_snapshot（下单时刻快照）。
 *
 * <p>只读，禁止在业务代码中调用 setter 修改字段值。
 */
@Entity
@Table(name = "v_order_chain_v2")
@Data
public class OrderChainView {

    // ====== 锚点：Demand（Step1 补货需求）======
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

    // ====== Step2：Procurement（发注单）======
    @Column(name = "procurement_id")
    private Long procurementId;

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

    @Column(name = "procurement_destination")
    private String procurementDestination;

    @Column(name = "procurement_status")
    private String procurementStatus;

    @Column(name = "procurement_return_reason")
    private String procurementReturnReason;

    @Column(name = "procurement_create_time")
    private LocalDateTime procurementCreateTime;

    @Column(name = "procurement_create_by")
    private String procurementCreateBy;

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

    // ====== 9步状态汇总（Step1~Step9）======
    @Column(name = "step1_status")
    private String step1Status;

    @Column(name = "step2_status")
    private String step2Status;

    @Column(name = "step3_status")
    private String step3Status;

    @Column(name = "step4_status")
    private String step4Status;

    @Column(name = "step5_status")
    private String step5Status;

    @Column(name = "step6_status")
    private String step6Status;

    @Column(name = "step7_status")
    private String step7Status;

    @Column(name = "step8_status")
    private String step8Status;

    @Column(name = "step9_status")
    private String step9Status;
}
