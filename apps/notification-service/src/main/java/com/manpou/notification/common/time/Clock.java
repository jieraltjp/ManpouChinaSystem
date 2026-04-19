package com.manpou.notification.common.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 时间抽象接口。
 *
 * INTJ 铁律：禁止直接调用 new Date() / LocalDateTime.now() / Instant.now()
 * 时间必须作为依赖注入，代码不应知道"现在几点"，只应知道逻辑。
 *
 * 优势：
 * - 单元测试：注入 mock clock，控制时间流逝
 * - 多时区：注入不同时区的 clock，不改业务代码
 * - 事件溯源：注入固定 clock，重放历史事件
 *
 * @see SystemClock
 */
public interface Clock {

    /**
     * 返回当前时刻的 Instant。
     */
    Instant nowInstant();

    /**
     * 返回当前时刻的 LocalDateTime（系统默认时区）。
     */
    LocalDateTime nowLocalDateTime();

    /**
     * 返回当前时刻的 LocalDateTime（指定时区）。
     */
    LocalDateTime nowLocalDateTime(ZoneId zone);

    /**
     * 返回系统默认时区。
     */
    ZoneId getZone();
}
