package com.manpou.allinone.infrastructure.config;

import com.manpou.allinone.common.time.Clock;
import com.manpou.allinone.infrastructure.security.JwtContextHolder;
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
            // 优先从 JWT 上下文获取（Vite 代理直连 allinone 时有效）
            String username = JwtContextHolder.getUsername();
            if (username != null && !username.isBlank()) {
                return Optional.of(username);
            }
            // 兜底：从 X-User-Id 请求头获取（经 API Gateway 时有效）
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String userId = request.getHeader("X-User-Id");
                if (userId != null && !userId.isBlank()) {
                    return Optional.of(userId);
                }
            }
            return Optional.of("system");
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
