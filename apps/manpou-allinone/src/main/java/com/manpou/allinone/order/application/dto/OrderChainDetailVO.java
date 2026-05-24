package com.manpou.allinone.order.application.dto;

import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.DomesticCustomsVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.FactoryVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.JapanCustomsVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.LogisticsPlanVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.ProcurementVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.QcRecordVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.SalesRecordVO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO.TaxRefundVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 订单总览详情 VO（全链路8步）。
 * 步骤3 厂家出货（多批次）、步骤4 验货记录（多记录）支持 List。
 */
@Data
@Builder
public class OrderChainDetailVO {

    private Long demandId;

    // 步骤1
    private ReplenishmentDemandVO demand;

    // 步骤2
    private ProcurementVO procurement;

    // 步骤2 工厂快照（来自 procurement_snapshot，下单时刻数据）
    private FactoryVO factory;

    // 步骤3：厂家出货（多批次）
    private List<ShipmentBatchVO> shipmentBatches;

    // 步骤4：验货记录（多记录）
    private List<QcRecordVO> qcRecords;

    // 步骤5
    private LogisticsPlanVO logisticsPlan;

    // 步骤6~9 Phase1 占位
    private DomesticCustomsVO domesticCustoms;
    private JapanCustomsVO japanCustoms;
    private TaxRefundVO taxRefund;
    private SalesRecordVO salesRecord;
}
