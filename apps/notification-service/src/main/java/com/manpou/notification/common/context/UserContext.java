package com.manpou.notification.common.context;

/**
 * 用户上下文接口（跨层共享）。
 *
 * 位置说明：
 * - 不属于 Domain（不是业务概念）
 * - 不属于 Infrastructure（被所有层引用）
 * - 放在 Common 表示跨层共享的基础组件
 *
 * 禁止在领域层直接引用 Infrastructure 层类型。
 */
public interface UserContext {

    /** 当前用户 ID。 */
    String getUserId();

    /** 当前租户 ID。 */
    String getTenantId();
}
