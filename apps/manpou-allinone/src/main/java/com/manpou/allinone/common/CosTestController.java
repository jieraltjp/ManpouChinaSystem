package com.manpou.allinone.common;

import com.manpou.allinone.common.config.CosConfig;
import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.common.service.CosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * COS 腾讯云对象存储测试接口（无需认证，仅供开发调试）。
 *
 * 使用说明：
 * 1. 确保环境变量 COS_SECRET_ID / COS_SECRET_KEY 已配置
 * 2. 确保 cos.enabled=true（默认）
 * 3. POST /api/v1/test/cos/status  查看配置状态
 * 4. POST /api/v1/test/cos/upload  上传测试
 * 5. GET  /api/v1/test/cos/list    列出已上传文件（TODO）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/cos")
@RequiredArgsConstructor
public class CosTestController {

    private final CosConfig cosConfig;
    private final CosService cosService;

    /**
     * 查看 COS 配置状态。
     * GET /api/v1/test/cos/status
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("enabled", cosConfig.isEnabled());
        info.put("bucket", cosConfig.getBucket());
        info.put("region", cosConfig.getRegion());
        info.put("domain", cosConfig.getDomain());
        info.put("prefix", cosConfig.getPrefix());
        info.put("maxFileSize", cosConfig.getMaxFileSize());
        info.put("secretIdSet", cosConfig.getSecretId() != null && !cosConfig.getSecretId().isBlank());
        info.put("secretKeySet", cosConfig.getSecretKey() != null && !cosConfig.getSecretKey().isBlank());
        log.info("[COS-Test] status called, enabled={}", cosConfig.isEnabled());
        return Result.ok(info);
    }

    /**
     * 上传测试文件。
     * POST /api/v1/test/cos/upload
     * Form: file (MultipartFile)
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        log.info("[COS-Test] upload called, filename={}, size={}", file.getOriginalFilename(), file.getSize());
        String url = cosService.upload(file);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url);
        result.put("filename", file.getOriginalFilename());
        result.put("size", file.getSize());
        result.put("contentType", file.getContentType());
        return Result.ok(result);
    }

    /**
     * 删除测试文件。
     * DELETE /api/v1/test/cos/delete?url=...
     */
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam("url") String url) {
        log.info("[COS-Test] delete called, url={}", url);
        cosService.delete(url);
        return Result.ok();
    }
}
