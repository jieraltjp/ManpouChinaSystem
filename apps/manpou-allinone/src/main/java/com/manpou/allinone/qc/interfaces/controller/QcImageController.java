package com.manpou.allinone.qc.interfaces.controller;

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
        String url = cosService.upload(file);
        QcImage image = new QcImage(
                extractFilename(url),
                file.getOriginalFilename(),
                url,
                file.getSize(),
                file.getContentType()
        );
        if (qcRecordId != null) {
            image.setQcRecordId(qcRecordId);
        }
        qcImageRepository.save(image);
        log.info("[QC-Image] uploaded, id={}, url={}", image.getId(), url);
        return Result.ok(new ImageUploadResult(url, image.getFilename(), file.getSize()));
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
            String url = cosService.upload(file);
            QcImage image = new QcImage(
                    extractFilename(url),
                    file.getOriginalFilename(),
                    url,
                    file.getSize(),
                    file.getContentType()
            );
            if (qcRecordId != null) {
                image.setQcRecordId(qcRecordId);
            }
            qcImageRepository.save(image);
            results.add(new ImageUploadResult(url, image.getFilename(), file.getSize()));
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
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }
}
