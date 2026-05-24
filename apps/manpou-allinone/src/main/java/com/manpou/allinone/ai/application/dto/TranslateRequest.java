package com.manpou.allinone.ai.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 翻译请求。
 * 对应 docs/business/SPEC-AI-01-AI翻译服务设计.md §3.1。
 */
@Data
public class TranslateRequest {

    @NotBlank(message = "sourceText 不能为空")
    @Size(max = 200, message = "sourceText 长度不能超过 200")
    private String sourceText;

    /** 源语言代码，默认 zh */
    private String sourceLang = "zh";

    /** 目标语言代码，默认 ja */
    private String targetLang = "ja";
}
