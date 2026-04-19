package com.manpou.warehouse.common.exception;

import jakarta.validation.constraints.*;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Map;

/**
 * 校验错误码映射器。
 * 从 JSR303 约束注解元数据推断语义化错误码，完全不解析消息文本。
 *
 * INTJ 审判：原实现硬编码中英文字符串（Hibernate Validator 升级即失效）。
 * 修复：从注解类名直接映射，与 Validator 版本和语言无关。
 *
 * 约束类型 → 错误码后缀：
 * - NotNull / NotBlank / NotEmpty  → required
 * - Size(max=N) / Max / DecimalMax  → max
 * - Min / DecimalMin                → min
 * - Pattern / Email / Digits        → format
 * - Positive / PositiveOrZero       → positive
 * - Negative / NegativeOrZero       → negative
 * - Past / Future                   → temporal
 * - 其他                            → invalid
 */
public enum ValidationErrorCodeMapper {

    INSTANCE;

    /** 注解类名（不含包名）→ 错误码后缀映射 */
    private static final Map<String, String> CONSTRAINT_TO_CODE = Map.ofEntries(
        Map.entry("NotNull", "required"),
        Map.entry("NotBlank", "required"),
        Map.entry("NotEmpty", "required"),
        Map.entry("Size", "max"),
        Map.entry("Max", "max"),
        Map.entry("DecimalMax", "max"),
        Map.entry("Min", "min"),
        Map.entry("DecimalMin", "min"),
        Map.entry("Pattern", "format"),
        Map.entry("Email", "format"),
        Map.entry("Digits", "format"),
        Map.entry("Positive", "positive"),
        Map.entry("PositiveOrZero", "positive"),
        Map.entry("Negative", "negative"),
        Map.entry("NegativeOrZero", "negative"),
        Map.entry("Past", "temporal"),
        Map.entry("Future", "temporal"),
        Map.entry("PastOrPresent", "temporal"),
        Map.entry("FutureOrPresent", "temporal"),
        Map.entry("AssertTrue", "assertion"),
        Map.entry("AssertFalse", "assertion")
    );

    public String map(FieldError error) {
        if (error == null) {
            return "validation.param.invalid";
        }
        String field = error.getField();
        String code = fromConstraintAnnotation(error);
        return "validation." + field + "." + code;
    }

    public String map(List<FieldError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "validation.param.invalid";
        }
        return map(errors.get(0));
    }

    /**
     * 从 FieldError 的约束注解类名推断错误码。
     * 不解析消息文本，与 Validator 版本/语言无关。
     */
    private String fromConstraintAnnotation(FieldError error) {
        var codes = error.getCodes();
        if (codes != null) {
            for (String code : codes) {
                // codes 格式：NotNull.domain.field / NotNull.field / NotNull.java.lang.String
                // 取最后一个段作为注解类名
                if (code != null && code.contains(".")) {
                    String annotation = code.substring(code.lastIndexOf('.') + 1);
                    String mapped = CONSTRAINT_TO_CODE.get(annotation);
                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
        }
        return "invalid";
    }
}
