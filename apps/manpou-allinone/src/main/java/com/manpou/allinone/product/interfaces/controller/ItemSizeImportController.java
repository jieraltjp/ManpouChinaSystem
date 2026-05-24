package com.manpou.allinone.product.interfaces.controller;

import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.product.application.usecase.ItemSizeImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * item_size 旧系统数据导入 Controller。
 * 提供手动触发接口，供运维/开发在确认数据后执行导入。
 */
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Slf4j
public class ItemSizeImportController {

    private final ItemSizeImportService itemSizeImportService;

    /**
     * 手动触发 item_size 导入。
     * POST /api/v1/internal/item-size/import
     * 权限：product:create（导入为高权限操作）
     */
    @PostMapping("/item-size/import")
    @PreAuthorize("hasAuthority('product:create')")
    public Result<Map<String, Object>> triggerImport() {
        log.info("手动触发 item_size 导入...");
        ItemSizeImportService.ImportReport report = itemSizeImportService.runImport();

        Map<String, Object> result = Map.of(
                "total", report.total(),
                "inserted", report.inserted(),
                "updated", report.updated(),
                "softDeleted", report.softDeleted(),
                "notFound", report.notFound(),
                "errors", report.errors(),
                "matched", report.getMatched()
        );

        if (report.errors().isEmpty()) {
            return Result.ok("item_size 导入成功", result);
        } else {
            return Result.ok("item_size 导入完成（含错误）", result);
        }
    }
}
