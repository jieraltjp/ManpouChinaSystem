package com.manpou.allinone.ai.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manpou.allinone.ai.application.dto.TranslateRequest;
import com.manpou.allinone.ai.application.dto.TranslateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * DeepSeek AI 翻译服务。
 * 对应 docs/business/SPEC-AI-01-AI翻译服务设计.md §4。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

    private final ObjectMapper objectMapper;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final float TEMPERATURE = 0.3f;

    /**
     * 中译日/中译英通用方法。
     */
    public TranslateResponse translate(TranslateRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException("AI 翻译 service not configured");
        }
        return callDeepseek(request);
    }

    /**
     * 商品名中译日（兼容旧调用）。
     */
    public TranslateResponse translateZhToJa(TranslateRequest request) {
        return translate(request);
    }

    private TranslateResponse callDeepseek(TranslateRequest request) {
        String prompt = buildPrompt(request.getSourceText(), request.getTargetLang());
        String jsonKey = "ja".equals(request.getTargetLang()) ? "nameJa" : "nameEn";

        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", TEMPERATURE
        );

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/chat/completions").openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            // 跳过 SSL 证书验证（开发环境）
            if (conn instanceof HttpsURLConnection httpsConn) {
                try {
                    SSLContext sslCtx = SSLContext.getInstance("TLS");
                    sslCtx.init(null, new TrustManager[]{DUMMY_TRUST_MANAGER}, new java.security.SecureRandom());
                    httpsConn.setSSLSocketFactory(sslCtx.getSocketFactory());
                    httpsConn.setHostnameVerifier((hostname, session) -> true);
                } catch (Exception e) {
                    log.warn("Failed to configure SSL bypass, using default: {}", e.getMessage());
                }
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String responseBody;
            try (InputStream is = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
                if (is == null) {
                    responseBody = "";
                } else {
                    responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            if (responseCode != 200) {
                log.error("DeepSeek API error: HTTP {} - {}", responseCode, responseBody);
                throw new AiServiceException("AI service error: HTTP " + responseCode);
            }

            return parseResponse(request.getSourceText(), responseBody, jsonKey);
        } catch (java.net.SocketTimeoutException e) {
            log.error("DeepSeek API timeout: {}", e.getMessage());
            throw new AiServiceException("Translation timeout, please retry");
        } catch (java.net.UnknownHostException e) {
            log.error("DeepSeek API unknown host: {}", e.getMessage());
            throw new AiServiceException("Cannot resolve api.deepseek.com, check network");
        } catch (IOException e) {
            log.error("DeepSeek API I/O error: {}", e.getMessage());
            throw new AiServiceException("Translation failed: " + e.getMessage());
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API error: {}", e.getMessage(), e);
            throw new AiServiceException("Translation failed");
        }
    }

    /** 跳过 SSL 验证的 TrustManager（仅开发环境使用） */
    private static final X509TrustManager DUMMY_TRUST_MANAGER = new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
    };

    private String buildPrompt(String text, String targetLang) {
        if ("en".equals(targetLang)) {
            return "Translate the Chinese product name below to English. Return ONLY valid JSON, no other text: {\"nameEn\":\"English translation\"}\n\nChinese: " + text;
        }
        return "将以下中文商品名称翻译成日文。只返回JSON，禁止其他文字：{\"nameJa\":\"日文翻译\"}\n\n中文：" + text;
    }

    private TranslateResponse parseResponse(String sourceText, String responseBody, String jsonKey) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode result = objectMapper.readTree(content);
            String targetText = result.path(jsonKey).asText();
            return TranslateResponse.builder()
                .sourceText(sourceText)
                .targetText(targetText)
                .nameJa("nameJa".equals(jsonKey) ? targetText : null)
                .nameEn("nameEn".equals(jsonKey) ? targetText : null)
                .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse DeepSeek response: {}", responseBody, e);
            throw new AiServiceException("Translation result parse failed");
        }
    }

    public static class AiServiceException extends RuntimeException {
        public AiServiceException(String message) {
            super(message);
        }
    }
}
