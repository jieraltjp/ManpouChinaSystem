package com.manpou.allinone.logistics.interfaces.controller;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import com.manpou.allinone.logistics.application.dto.ContainerCreateCmd;
import com.manpou.allinone.logistics.application.dto.ContainerPageQuery;
import com.manpou.allinone.logistics.application.dto.ContainerQuery;
import com.manpou.allinone.logistics.application.dto.ContainerUpdateCmd;
import com.manpou.allinone.logistics.application.usecase.ContainerUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 货柜 REST 接口（v1.5.0，SPEC-B00 Issue #8）。
 */
@RestController
@RequestMapping("/api/v1/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerUseCase useCase;

    @GetMapping
    @PreAuthorize("hasAuthority('container:read')")
    public Result<Page<ContainerPageQuery>> list(ContainerQuery query) {
        return Result.ok(useCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('container:read')")
    public Result<ContainerPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(useCase.getById(id));
    }

    @PostMapping
    @Idempotent(ttl = 86400)
    @PreAuthorize("hasAuthority('container:create')")
      @AuditLog(module = "logistics", action = "CREATE", resourceType = "container", resourceId = "#_return")
    public Result<Long> create(@Valid @RequestBody ContainerCreateCmd cmd) {
        return Result.ok(useCase.create(cmd));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('container:update')")
    @AuditLog(module = "logistics", action = "UPDATE", resourceType = "container", resourceId = "#id")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody ContainerUpdateCmd cmd) {
        useCase.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('container:delete')")
    @AuditLog(module = "logistics", action = "DELETE", resourceType = "container", resourceId = "#id")
    public Result<Void> delete(@PathVariable("id") Long id) {
        useCase.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/plans/{planId}")
    @PreAuthorize("hasAuthority('container:update')")
    public Result<Void> addPlan(@PathVariable("id") Long containerId,
                                @PathVariable("planId") Long planId) {
        useCase.addPlan(containerId, planId);
        return Result.ok();
    }
}
