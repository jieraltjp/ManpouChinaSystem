package com.manpou.allinone.order.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单总览聚合响应。
 * 以 Procurement.id 为锚点，聚合全链路 8 步数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderOverviewPageVO {

    private Long procurementId;

    // 锚点
    private ProcurementVO procurement;
    private FactoryVO factory;

    // 步骤1：补货需求（linked_procurement_id 关联）
    private DemandVO demand;

    // 步骤2：发注单（锚点自身）

    // 步骤3：验货记录
    private QcRecordVO qcRecord;

    // 步骤4：调配计划
    private LogisticsPlanVO logisticsPlan;

    // 步骤5：国内报关
    private DomesticCustomsVO domesticCustoms;

    // 步骤6：日本清关
    private JapanCustomsVO japanCustoms;

    // 步骤7：退税
    private TaxRefundVO taxRefund;

    // 步骤8：运营销售
    private SalesRecordVO salesRecord;

    // 8步状态数组（供前端进度条渲染）
    private StepStatus[] stepStatuses;

    // ===== 子 VO =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcurementVO {
        private Long id;
        private String procurementCode;
        private Long factoryId;
        private String factoryName;
        private String productCode;
        private String subProductCode;
        private String material;
        private Boolean requiresQc;
        private Integer quantity;
        private java.math.BigDecimal priceRmb;
        private java.math.BigDecimal exchangeRate;
        private java.math.BigDecimal taxPoint;
        private String billingType;
        private java.math.BigDecimal estimatedPriceJpy;
        private java.time.LocalDate orderDate;
        private java.time.LocalDate factoryShipDate;
        private java.time.LocalDate plannedShipDate;
        private java.time.LocalDate actualShipDate;
        private String productLead;
        private String japanLead;
        private String chinaLead;
        private String destination;
        private String customerCompany;
        private String status;
        private java.time.LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FactoryVO {
        private Long id;
        private String factoryCode;
        private String factoryName;
        private String category;
        private String province;
        private String city;
        private String county;
        private String roughLocation;
        private String contactName;
        private String contactPhone;
        private String cooperationStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemandVO {
        private Long id;
        private String demandCode;
        private String demandType;
        private String productCode;
        private String subProductCode;
        private Integer quantity;
        private String destination;
        private String japanLead;
        private String status;
        private java.time.LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QcRecordVO {
        private Long id;
        private String qcCode;
        private Long procurementId;
        private String sellerName;
        private String productCode;
        private String subProductCode;
        private String result;
        private Integer inspectionCount;
        private Integer passedCount;
        private Integer defectiveCount;
        private Integer boxCount;
        private java.math.BigDecimal boxLengthCm;
        private java.math.BigDecimal boxWidthCm;
        private java.math.BigDecimal boxHeightCm;
        private java.math.BigDecimal netWeightPerUnit;
        private java.math.BigDecimal grossWeight;
        private java.time.LocalDate qcDate;
        private Long qcUserId;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogisticsPlanVO {
        private Long id;
        private String planCode;
        private Long procurementId;
        private Long factoryId;
        private String productCode;
        private String subProductCode;
        private String planType;
        private String status;
        private java.math.BigDecimal cargoLengthCm;
        private java.math.BigDecimal cargoWidthCm;
        private java.math.BigDecimal cargoHeightCm;
        private java.math.BigDecimal cargoVolumeCbm;
        private java.math.BigDecimal cargoWeightKg;
        private Integer quantity;
        private Boolean requiresQc;
        private java.time.LocalDate estimatedShipDate;
        private java.time.LocalDate actualShipDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomesticCustomsVO {
        private Long id;
        private String customsCode;
        private Long procurementId;
        private String productCode;
        private String status;
        private java.math.BigDecimal estimatedValueCny;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JapanCustomsVO {
        private Long id;
        private Long procurementId;
        private Long domesticCustomsId;
        private Long logisticsPlanId;
        private String customsEntryNo;
        private String status;
        private java.time.LocalDate arrivalDate;
        private java.time.LocalDate clearanceDate;
        private String customsBroker;
        private String brokerPhone;
        private String brokerContact;
        private java.math.BigDecimal importDutyPaid;
        private java.math.BigDecimal consumptionTaxPaid;
        private String arrivalPort;
        private java.math.BigDecimal declaredWeightKg;
        private java.math.BigDecimal declaredVolumeCbm;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxRefundVO {
        private Long id;
        private Long procurementId;
        private Long japanCustomsId;
        private String refundCode;
        private String status;
        private String billingType;
        private java.math.BigDecimal priceRmb;
        private Integer quantity;
        private java.math.BigDecimal taxPoint;
        private java.math.BigDecimal estimatedRefundRmb;
        private java.math.BigDecimal actualRefundRmb;
        private java.math.BigDecimal exchangeRate;
        private java.time.LocalDate refundDate;
        private String refundBank;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesRecordVO {
        private Long id;
        private String recordCode;
        private Long procurementId;
        private String productCode;
        private String subProductCode;
        private String salesChannel;
        private java.time.LocalDate listingDate;
        private Integer initialStock;
        private Integer currentStock;
        private Integer safetyStock;
        private Integer salesQuantity;
        private Integer returnedQuantity;
        private java.math.BigDecimal returnRate;
        private java.math.BigDecimal sellingPriceJpy;
        private String status;
        private String remarks;
    }

    /** 步骤状态枚举（前端用） */
    public enum StepStatus {
        NOT_STARTED,  // 无记录
        IN_PROGRESS,  // 有记录且非终态
        COMPLETED     // 终态
    }
}
