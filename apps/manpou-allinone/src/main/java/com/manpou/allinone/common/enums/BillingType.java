package com.manpou.allinone.common.enums;

/**
 * 报关类型枚举。
 * 对应业务流第二步下单的「报关类型」字段。
 *
 * 来源：业务流分析（浙鲁开票/超慧退税/不退税）
 */
public enum BillingType {
    ZHE_LU_KAI_PIAO,   // 浙鲁开票
    CHAO_HUI_TUI_SHUI,  // 超慧退税
    NO_REFUND,          // 不退税
    OTHER               // 其他
}
