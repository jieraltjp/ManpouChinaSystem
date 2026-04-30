package com.manpou.allinone.sales.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 新品采购需求事件（NEW_PURCHASE 触发）。
 *
 * 触发条件（待产品经理确认）：
 * - 客户询价记录 → 新品意向
 * - 竞争对手分析 → 新品货号规划
 * - 销售预测 → 爆款提前备货
 *
 * 副作用：自动创建 ReplenishmentDemand（DemandType = NEW_PURCHASE）
 */
@Getter
public class NewPurchaseDemandNeededEvent extends ApplicationEvent {

    private final String productCode;
    private final String subProductCode;
    private final Integer requestedQuantity;
    private final String triggerSource;

    public NewPurchaseDemandNeededEvent(Object source, String productCode, String subProductCode,
            Integer requestedQuantity, String triggerSource) {
        super(source);
        this.productCode = productCode;
        this.subProductCode = subProductCode;
        this.requestedQuantity = requestedQuantity;
        this.triggerSource = triggerSource;
    }
}
