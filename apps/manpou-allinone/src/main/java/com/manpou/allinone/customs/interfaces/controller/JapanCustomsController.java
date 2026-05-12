package com.manpou.allinone.customs.interfaces.controller;

import com.manpou.allinone.customs.application.dto.*;
import com.manpou.allinone.customs.application.usecase.JapanCustomsUseCase;

import java.util.List;
import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 日本清关记录 Controller。
 * 路径：/api/v1/japan-customs
 *
 * 状态流转：PENDING → IN_PROGRESS → CLEARED | FAILED
 *
 * 触发时机：DomesticCustomsRecord.status = CLEARED 时自动/手动创建。
 */
@RestController
@RequestMapping("/api/v1/japan-customs")
@RequiredArgsConstructor
public class JapanCustomsController {

    private final JapanCustomsUseCase japanCustomsUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('japan_customs:read')")
    public Result<Page<JapanCustomsPageQuery>> list(JapanCustomsQuery query) {
        return Result.ok(japanCustomsUseCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('japan_customs:read')")
    public Result<JapanCustomsPageQuery> get(@PathVariable Long id) {
        return Result.ok(japanCustomsUseCase.getById(id));
    }

    @PostMapping
      @AuditLog(module = "japan_customs", action = "CREATE", resourceType = "japan_customs", resourceId = "#_return")
    @Idempotent(ttl = 24 * 60 * 60)
    @PreAuthorize("hasAuthority('japan_customs:create')")
    public Result<Long> create(@Valid @RequestBody JapanCustomsCreateCmd cmd) {
        return Result.ok("创建成功", japanCustomsUseCase.create(cmd));
    }

    @PostMapping("/batch")
      @AuditLog(module = "japan_customs", action = "CREATE", resourceType = "japan_customs", resourceId = "#_return")
    @Idempotent(ttl = 24 * 60 * 60)
    @PreAuthorize("hasAuthority('japan_customs:create')")
    public Result<List<Long>> batchCreate(@Valid @RequestBody JapanCustomsBatchCreateCmd cmd) {
        return Result.ok("批量创建成功", japanCustomsUseCase.batchCreate(cmd));
    }

    @PutMapping("/{id}")
    @AuditLog(module = "japan_customs", action = "UPDATE", resourceType = "japan_customs", resourceId = "#id")
    @PreAuthorize("hasAuthority('japan_customs:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody JapanCustomsUpdateCmd cmd) {
        japanCustomsUseCase.update(id, cmd);
        return Result.ok("更新成功", null);
    }

    @PatchMapping("/{id}/start")
    @AuditLog(module = "japan_customs", action = "UPDATE", resourceType = "japan_customs", resourceId = "#id")
    @PreAuthorize("hasAuthority('japan_customs:update')")
    public Result<Void> start(@PathVariable Long id) {
        japanCustomsUseCase.startClearance(id);
        return Result.ok("开始清关", null);
    }

    @PatchMapping("/{id}/complete")
    @AuditLog(module = "japan_customs", action = "UPDATE", resourceType = "japan_customs", resourceId = "#id")
    @PreAuthorize("hasAuthority('japan_customs:update')")
    public Result<Void> complete(@PathVariable Long id,
                                  @Valid @RequestBody JapanCustomsCompleteCmd cmd) {
        japanCustomsUseCase.complete(id, cmd);
        return Result.ok("清关完成", null);
    }

    @PatchMapping("/{id}/fail")
    @AuditLog(module = "japan_customs", action = "UPDATE", resourceType = "japan_customs", resourceId = "#id")
    @PreAuthorize("hasAuthority('japan_customs:update')")
    public Result<Void> fail(@PathVariable Long id,
                              @Valid @RequestBody JapanCustomsFailCmd cmd) {
        japanCustomsUseCase.fail(id, cmd);
        return Result.ok("标记失败", null);
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "japan_customs", action = "DELETE", resourceType = "japan_customs", resourceId = "#id")
    @PreAuthorize("hasAuthority('japan_customs:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        japanCustomsUseCase.delete(id);
        return Result.ok("删除成功", null);
    }
}
