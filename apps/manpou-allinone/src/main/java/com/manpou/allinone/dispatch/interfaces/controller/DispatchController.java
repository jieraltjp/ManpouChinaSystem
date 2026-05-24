package com.manpou.allinone.dispatch.interfaces.controller;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.dispatch.application.dto.DispatchCreateCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchQuery;
import com.manpou.allinone.dispatch.application.dto.DispatchUpdateCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchVO;
import com.manpou.allinone.dispatch.application.usecase.DispatchUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dispatches")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchUseCase useCase;

    @GetMapping
    @PreAuthorize("hasAuthority('dispatch:read')")
    public Result<Page<DispatchVO>> list(DispatchQuery query) {
        return Result.ok(useCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('dispatch:read')")
    public Result<DispatchVO> get(@PathVariable("id") Long id) {
        return Result.ok(useCase.getById(id));
    }

    @PostMapping
    @Idempotent(ttl = 86400)
    @PreAuthorize("hasAuthority('dispatch:create')")
    @AuditLog(module = "dispatch", action = "CREATE", resourceType = "dispatch", resourceId = "#_return")
    public Result<Long> create(@Valid @RequestBody DispatchCreateCmd cmd) {
        return Result.ok(useCase.create(cmd));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('dispatch:update')")
    @AuditLog(module = "dispatch", action = "UPDATE", resourceType = "dispatch", resourceId = "#id")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody DispatchUpdateCmd cmd) {
        useCase.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('dispatch:delete')")
    @AuditLog(module = "dispatch", action = "DELETE", resourceType = "dispatch", resourceId = "#id")
    public Result<Void> delete(@PathVariable("id") Long id) {
        useCase.delete(id);
        return Result.ok();
    }
}