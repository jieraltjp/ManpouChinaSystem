package com.manpou.procurement.infrastructure.config;

import com.manpou.procurement.common.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

/**
 * JPA 审计配置。
 * 自动填充 createTime、updateTime、createBy、updateBy 字段。
 * 时间由 Clock 接口注入，支持单元测试。
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "dateTimeProvider")
public class JpaAuditConfig {

    private final Clock clock;

    public JpaAuditConfig(Clock clock) {
        this.clock = clock;
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return Optional.of("system");
            }
            HttpServletRequest request = attrs.getRequest();
            String userId = request.getHeader("X-User-Id");
            return Optional.ofNullable(userId != null ? userId : "system");
        };
    }

    /**
     * 时间提供者，委托给 Clock 接口。
     * 使得 @CreatedDate / @LastModifiedDate 可被 mock。
     */
    @Bean
    public DateTimeProvider dateTimeProvider() {
        return () -> {
            LocalDateTime now = clock.nowLocalDateTime();
            return Optional.of(now);
        };
    }
}
