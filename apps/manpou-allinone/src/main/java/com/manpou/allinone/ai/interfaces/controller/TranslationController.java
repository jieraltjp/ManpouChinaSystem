package com.manpou.allinone.ai.interfaces.controller;

import com.manpou.allinone.ai.application.TranslationService;
import com.manpou.allinone.ai.application.TranslationService.AiServiceException;
import com.manpou.allinone.ai.application.dto.TranslateRequest;
import com.manpou.allinone.ai.application.dto.TranslateResponse;
import com.manpou.allinone.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * AI 翻译 Controller。
 * 对应 docs/business/SPEC-AI-01-AI翻译服务设计.md §3.1。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;

    /**
     * 健康检查（不调用外部 API）。
     */
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong - " + translationService.getClass().getSimpleName());
    }

    /**
     * 翻译接口（中译日 / 中译英）。
     * POST /api/v1/ai/translate
     * targetLang: ja（默认）| en
     */
    @PostMapping("/translate")
    @PreAuthorize("hasAuthority('product:read')")
    public Result<TranslateResponse> translate(@Valid @RequestBody TranslateRequest request) {
        try {
            TranslateResponse response = translationService.translate(request);
            return Result.ok(response);
        } catch (AiServiceException e) {
            log.warn("Translation failed: {}", e.getMessage());
            return Result.fail("ai.translate.failed", e.getMessage());
        }
    }
}
