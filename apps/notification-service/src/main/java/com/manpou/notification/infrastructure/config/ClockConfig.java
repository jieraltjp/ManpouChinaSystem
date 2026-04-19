package com.manpou.notification.infrastructure.config;

import com.manpou.notification.common.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

/**
 * 时间配置。
 * 提供系统 Clock Bean，生产环境使用系统时钟。
 *
 * 测试环境替换方式：
 * 1. 在测试类中使用 @MockBean 替换此 Bean
 * 2. 使用 @TestConfiguration 注入自定义 Clock
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return new SystemClockImpl();
    }

    /**
     * 系统时钟实现。
     * 直接委托给 JDK 系统时间。
     */
    static final class SystemClockImpl implements Clock {

        @Override
        public java.time.Instant nowInstant() {
            return java.time.Instant.now();
        }

        @Override
        public java.time.LocalDateTime nowLocalDateTime() {
            return java.time.LocalDateTime.now();
        }

        @Override
        public java.time.LocalDateTime nowLocalDateTime(ZoneId zone) {
            return java.time.LocalDateTime.now(zone);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }
    }
}
