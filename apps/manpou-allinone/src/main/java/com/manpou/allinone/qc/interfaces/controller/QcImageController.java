package com.manpou.allinone.qc.interfaces.controller;

import com.manpou.allinone.common.config.CosConfig;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.service.CosService;
import com.manpou.allinone.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import com.manpou.allinone.qc.domain.model.QcImage;
import com.manpou.allinone.qc.domain.repository.QcImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/qc/images")
@RequiredArgsConstructor
public class QcImageController {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_PER_RECORD = 9;

    private final CosService cosService;
    private final QcImageRepository qcImageRepository;
    private final CosConfig cosConfig;

    // --- 内部 DTO ---

    public record ImageUploadResult(String url, String filename, Long size) {}

    // --- API ---

    /**
     * 上传单张图片。
     * POST /api/v1/qc/images/upload
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('qc:create')")
    public Result<ImageUploadResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "qcRecordId", required = false) Long qcRecordId) {
        validateFile(file);
        if (qcRecordId != null) {
            long count = qcImageRepository.countByQcRecordIdAndIsDeletedFalse(qcRecordId);
            if (count >= MAX_PER_RECORD) {
                throw BusinessException.invalidParam("每条验货记录最多上传 " + MAX_PER_RECORD + " 张图片");
            }
        }
        String key = cosService.upload(file);
        String displayUrl = buildDisplayUrl(key);
        QcImage image = new QcImage(
                extractFilename(key),            // basename: xxx.jpg（用于 COS delete 时提取 key）
                file.getOriginalFilename(),
                displayUrl,                     // 展示 URL（含 query param）
                file.getSize(),
                file.getContentType()
        );
        if (qcRecordId != null) {
            image.setQcRecordId(qcRecordId);
        }
        qcImageRepository.save(image);
        log.info("[QC-Image] uploaded, id={}, key={}", image.getId(), key);
        return Result.ok(new ImageUploadResult(displayUrl, image.getFilename(), file.getSize()));
    }

    /**
     * 批量上传（最多 9 张）。
     * POST /api/v1/qc/images/upload-multiple
     */
    @PostMapping("/upload-multiple")
    @PreAuthorize("hasAuthority('qc:create')")
    public Result<List<ImageUploadResult>> uploadMultiple(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "qcRecordId", required = false) Long qcRecordId) {
        if (files == null || files.length == 0) {
            throw BusinessException.invalidParam("至少上传一张图片");
        }
        if (files.length > MAX_PER_RECORD) {
            throw BusinessException.invalidParam("最多上传 " + MAX_PER_RECORD + " 张图片");
        }
        if (qcRecordId != null) {
            long existing = qcImageRepository.countByQcRecordIdAndIsDeletedFalse(qcRecordId);
            if (existing + files.length > MAX_PER_RECORD) {
                throw BusinessException.invalidParam("每条验货记录最多上传 " + MAX_PER_RECORD + " 张图片");
            }
        }
        List<ImageUploadResult> results = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);
            String key = cosService.upload(file);
            String displayUrl = buildDisplayUrl(key);
            QcImage image = new QcImage(
                    extractFilename(key),            // basename: xxx.jpg（用于 COS delete 时提取 key）
                    file.getOriginalFilename(),
                    displayUrl,                     // 展示 URL（含 query param）
                    file.getSize(),
                    file.getContentType()
            );
            if (qcRecordId != null) {
                image.setQcRecordId(qcRecordId);
            }
            qcImageRepository.save(image);
            results.add(new ImageUploadResult(displayUrl, image.getFilename(), file.getSize()));
        }
        return Result.ok(results);
    }

    /**
     * 删除图片（软删 + COS 删除）。
     * DELETE /api/v1/qc/images?id=123
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('qc:delete')")
    public Result<Void> delete(@RequestParam("id") Long id) {
        QcImage image = qcImageRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("QcImage", id));
        cosService.delete(image.getUrl());
        image.softDelete(null);
        qcImageRepository.save(image);
        log.info("[QC-Image] deleted, id={}", id);
        return Result.ok();
    }

    /**
     * 按验货记录查询图片列表。
     * GET /api/v1/qc/images?qcRecordId=123
     */
    @GetMapping
    @PreAuthorize("hasAuthority('qc:read')")
    public Result<List<QcImage>> list(@RequestParam("qcRecordId") Long qcRecordId) {
        List<QcImage> images = qcImageRepository.findByQcRecordIdAndIsDeletedFalse(qcRecordId);
        return Result.ok(images);
    }

    /**
     * 临时清理接口：修复含 ?response-content-disposition=inline 的脏 URL。
     * 修复完成后删除此接口。
     */
    @PostMapping("/cleanup-urls")
    @PreAuthorize("hasAuthority('qc:delete')")
    public Result<Integer> cleanupDirtyUrls() {
        List<QcImage> all = qcImageRepository.findAll();
        int count = 0;
        for (QcImage img : all) {
            String url = img.getUrl();
            if (url != null && url.contains("?response-content-disposition=inline")) {
                String cleanUrl = url.replace("?response-content-disposition=inline", "");
                img.setUrl(cleanUrl);
                // filename 去掉路径前缀，只保留 basename
                String fn = img.getFilename();
                if (fn != null && fn.contains("/")) {
                    fn = fn.substring(fn.lastIndexOf('/') + 1);
                }
                if (fn != null && fn.contains("?")) {
                    fn = fn.substring(0, fn.indexOf('?'));
                }
                img.setFilename(fn);
                qcImageRepository.save(img);
                count++;
            }
        }
        log.info("[QC-Image] cleanup done, fixed {} records", count);
        return Result.ok(count);
    }

    // --- 内部方法 ---

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.invalidParam("文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw BusinessException.invalidParam("文件大小不能超过 5MB");
        }
        String type = file.getContentType();
        if (type == null || !ALLOWED_TYPES.contains(type.toLowerCase())) {
            throw BusinessException.invalidParam("仅支持 JPG/PNG/WEBP 格式");
        }
    }

    private String extractFilename(String url) {
        if (url == null) return null;
        // 防腐：先剥离 query string（?response-content-disposition=inline 等）
        int q = url.indexOf('?');
        if (q >= 0) url = url.substring(0, q);
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }

    /** 构造带 inline disposition 的展示 URL */
    private String buildDisplayUrl(String key) {
        return cosConfig.getDomain() + "/" + key + "?response-content-disposition=inline";
    }
}
