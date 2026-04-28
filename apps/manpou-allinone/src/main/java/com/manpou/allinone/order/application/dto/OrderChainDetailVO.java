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

/**
 * 订单总览详情 VO（Phase1 步骤1~4）。
 * 全链路8步数据，Phase1 步骤5~8 返回 null。
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

    // 步骤3
    private QcRecordVO qcRecord;

    // 步骤4
    private LogisticsPlanVO logisticsPlan;

    // 步骤5~8 Phase1 占位
    private DomesticCustomsVO domesticCustoms;
    private JapanCustomsVO japanCustoms;
    private TaxRefundVO taxRefund;
    private SalesRecordVO salesRecord;
}
