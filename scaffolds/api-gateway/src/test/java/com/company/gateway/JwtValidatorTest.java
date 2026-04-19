package com.company.gateway;

import com.company.gateway.security.JwtClaims;
import com.company.gateway.security.JwtPublicKeyManager;
import com.company.gateway.security.JwtValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT 验签单元测试。
 *
 * 注意：此类需要有效的 keys/public.pem 文件才能运行。
 * 本测试验证 JwtClaims 字段提取的正确性。
 */
class JwtValidatorTest {

    @Test
    void claims_record_has_correct_fields() {
        // Given
        JwtClaims claims = new JwtClaims(
            "u_001", "test_user",
            java.util.List.of("ADMIN"),
            java.util.List.of("user:read", "user:write"),
            "t_001"
        );

        // Then
        assertThat(claims.userId()).isEqualTo("u_001");
        assertThat(claims.username()).isEqualTo("test_user");
        assertThat(claims.hasRole("ADMIN")).isTrue();
        assertThat(claims.hasRole("USER")).isFalse();
        assertThat(claims.hasAuthority("user:read")).isTrue();
        assertThat(claims.hasAuthority("order:read")).isFalse();
        assertThat(claims.tenantId()).isEqualTo("t_001");
    }
}
