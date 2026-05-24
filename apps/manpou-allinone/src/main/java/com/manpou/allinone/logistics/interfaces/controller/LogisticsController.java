package com.manpou.allinone.logistics.interfaces.controller;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanCreateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanPageQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsPlanUpdateCmd;
import com.manpou.allinone.logistics.application.usecase.LogisticsPlanUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 调配计划 REST 接口。
 */
@RestController
@RequestMapping("/api/v1/logistics-plans")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsPlanUseCase logisticsPlanUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('logistics:read')")
    public Result<Page<LogisticsPlanPageQuery>> list(LogisticsPlanQuery query) {
        return Result.ok(logisticsPlanUseCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('logistics:read')")
    public Result<LogisticsPlanPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(logisticsPlanUseCase.getById(id));
    }

    @PostMapping
    @Idempotent(ttl = 86400)
    @PreAuthorize("hasAuthority('logistics:create')")
      @AuditLog(module = "logistics", action = "CREATE", resourceType = "logistics_plan", resourceId = "#_return")
    public Result<Long> create(@Valid @RequestBody LogisticsPlanCreateCmd cmd) {
        return Result.ok(logisticsPlanUseCase.create(cmd));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('logistics:update')")
    @AuditLog(module = "logistics", action = "UPDATE", resourceType = "logistics_plan", resourceId = "#id")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody LogisticsPlanUpdateCmd cmd) {
        logisticsPlanUseCase.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('logistics:delete')")
    @AuditLog(module = "logistics", action = "DELETE", resourceType = "logistics_plan", resourceId = "#id")
    public Result<Void> delete(@PathVariable("id") Long id) {
        logisticsPlanUseCase.delete(id);
        return Result.ok();
    }

    @PatchMapping("/batch/customs-clearance-no")
    @PreAuthorize("hasAuthority('logistics:update')")
    @AuditLog(module = "logistics", action = "BATCH_UPDATE_CUSTOMS_NO", resourceType = "logistics_plan", resourceId = "#ids")
    public Result<Integer> batchUpdateCustomsClearanceNo(
            @RequestBody java.util.Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        java.util.List<Long> ids = ((java.util.List<Number>) body.get("ids")).stream()
                .map(Number::longValue).toList();
        String customsClearanceNo = (String) body.get("customsClearanceNo");
        int count = logisticsPlanUseCase.batchUpdateCustomsClearanceNo(ids, customsClearanceNo);
        return Result.ok(count);
    }
}
