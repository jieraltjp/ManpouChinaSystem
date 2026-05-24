package com.manpou.allinone.ai.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 翻译响应。
 * 对应 docs/business/SPEC-AI-01-AI翻译服务设计.md §3.1。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslateResponse {

    private String sourceText;

    private String targetText;

    /** 日文商品名（targetLang=ja 时填充） */
    private String nameJa;

    /** 英文商品名（targetLang=en 时填充） */
    private String nameEn;
}
