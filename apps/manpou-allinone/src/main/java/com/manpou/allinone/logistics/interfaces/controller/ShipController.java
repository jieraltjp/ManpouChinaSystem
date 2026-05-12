package com.manpou.allinone.logistics.interfaces.controller;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.logistics.application.dto.ship.ShipCreateCmd;
import com.manpou.allinone.logistics.application.dto.ship.ShipQuery;
import com.manpou.allinone.logistics.application.dto.ship.ShipUpdateCmd;
import com.manpou.allinone.logistics.application.dto.ship.ShipVO;
import com.manpou.allinone.logistics.application.usecase.ShipUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 船只 REST 接口（v1.0.0，SPEC-B12）。
 */
@RestController
@RequestMapping("/api/v1/ships")
@RequiredArgsConstructor
public class ShipController {

    private final ShipUseCase useCase;

    @GetMapping
    @PreAuthorize("hasAuthority('ship:read')")
    public Result<Page<ShipVO>> list(ShipQuery query) {
        return Result.ok(useCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ship:read')")
    public Result<ShipVO> get(@PathVariable("id") Long id) {
        return Result.ok(useCase.getById(id));
    }

    @PostMapping
    @Idempotent(ttl = 86400)
    @PreAuthorize("hasAuthority('ship:create')")
    @AuditLog(module = "ship", action = "CREATE", resourceType = "ship", resourceId = "#_return")
    public Result<Long> create(@Valid @RequestBody ShipCreateCmd cmd) {
        return Result.ok(useCase.create(cmd));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ship:update')")
    @AuditLog(module = "ship", action = "UPDATE", resourceType = "ship", resourceId = "#id")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody ShipUpdateCmd cmd) {
        useCase.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ship:delete')")
    @AuditLog(module = "ship", action = "DELETE", resourceType = "ship", resourceId = "#id")
    public Result<Void> delete(@PathVariable("id") Long id) {
        useCase.delete(id);
        return Result.ok();
    }
}
