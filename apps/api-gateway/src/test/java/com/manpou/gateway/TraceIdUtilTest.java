package com.manpou.gateway;

import org.junit.jupiter.api.Test;

import static com.manpou.gateway.filter.TraceIdUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TraceId 工具单元测试。
 */
class TraceIdUtilTest {

    @Test
    void extract_trace_id_from_valid_traceparent() {
        // Given: W3C traceparent 标准格式
        String traceparent = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";

        // When
        String traceId = extractOrGenerate(traceparent);

        // Then: 提取出 32 字符的 traceId
        assertThat(traceId).isEqualTo("0af7651916cd43dd8448eb211c80319c");
        assertThat(traceId).hasSize(32);
    }

    @Test
    void generate_trace_id_when_no_traceparent() {
        // When
        String traceId = extractOrGenerate(null);

        // Then: 生成 32 字符十六进制
        assertThat(traceId).hasSize(32);
        assertThat(traceId).matches("[a-f0-9]{32}");
    }

    @Test
    void generate_trace_id_when_traceparent_invalid() {
        // When
        String traceId = extractOrGenerate("invalid-traceparent");

        // Then: 回退到生成
        assertThat(traceId).hasSize(32);
    }

    @Test
    void trace_id_is_hexadecimal() {
        for (int i = 0; i < 100; i++) {
            String traceId = extractOrGenerate(null);
            assertThat(traceId)
                .matches("[a-f0-9]{32}")
                .doesNotContain("-");
        }
    }
}
