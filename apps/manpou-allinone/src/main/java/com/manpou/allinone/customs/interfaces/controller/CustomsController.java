package com.manpou.allinone.customs.interfaces.controller;

import com.manpou.allinone.customs.application.dto.CustomsBatchCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsPageQuery;
import com.manpou.allinone.customs.application.dto.CustomsQuery;
import com.manpou.allinone.customs.application.dto.CustomsUpdateCmd;
import com.manpou.allinone.customs.application.usecase.CustomsUseCase;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 国内报关记录接口。
 */
@RestController
@RequestMapping("/api/v1/customs")
@RequiredArgsConstructor
public class CustomsController {

    private final CustomsUseCase customsUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('customs:read')")
    public Result<Page<CustomsPageQuery>> list(CustomsQuery query) {
        return Result.ok(customsUseCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customs:read')")
    public Result<CustomsPageQuery> get(@PathVariable Long id) {
        return Result.ok(customsUseCase.getById(id));
    }

    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    @PreAuthorize("hasAuthority('customs:create')")
    public Result<Long> create(@Valid @RequestBody CustomsCreateCmd cmd) {
        Long id = customsUseCase.create(cmd);
        return Result.ok("创建成功", id);
    }

    /**
     * 批量创建国内报关记录（v1.4.0）。
     * 根据 logisticsPlanIds 查询 LogisticsPlan 实体，自动填充 productCode/subProductCode 等字段。
     * 一个 LogisticsPlan 对应一条 DomesticCustomsRecord。
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('customs:create')")
    public Result<List<Long>> batchCreate(@Valid @RequestBody CustomsBatchCreateCmd cmd) {
        List<Long> ids = customsUseCase.batchCreate(cmd);
        return Result.ok("批量创建成功，共 " + ids.size() + " 条", ids);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customs:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CustomsUpdateCmd cmd) {
        customsUseCase.update(id, cmd);
        return Result.ok("更新成功", null);
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('customs:update')")
    public Result<Void> submit(@PathVariable Long id) {
        customsUseCase.submit(id);
        return Result.ok("提交成功", null);
    }

    @PatchMapping("/{id}/clear")
    @PreAuthorize("hasAuthority('customs:update')")
    public Result<Void> clear(@PathVariable Long id) {
        customsUseCase.clear(id);
        return Result.ok("通关完成", null);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('customs:update')")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        customsUseCase.reject(id, body.get("reason"));
        return Result.ok("驳回成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('customs:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        customsUseCase.delete(id);
        return Result.ok("删除成功", null);
    }
}
