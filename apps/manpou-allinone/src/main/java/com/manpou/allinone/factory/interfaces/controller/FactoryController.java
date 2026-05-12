package com.manpou.allinone.factory.interfaces.controller;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import com.manpou.allinone.factory.application.dto.FactoryCreateCmd;
import com.manpou.allinone.factory.application.dto.FactoryPageQuery;
import com.manpou.allinone.factory.application.dto.FactoryQuery;
import com.manpou.allinone.factory.application.dto.FactoryStatsDTO;
import com.manpou.allinone.factory.application.dto.FactoryUpdateCmd;
import com.manpou.allinone.factory.application.usecase.FactoryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/factories")
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryUseCase factoryUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('factory:read')")
    public Result<Page<FactoryPageQuery>> list(FactoryQuery query) {
        return Result.ok(factoryUseCase.pageQuery(query));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('factory:read')")
    public Result<FactoryStatsDTO> stats() {
        return Result.ok(factoryUseCase.stats());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('factory:read')")
    public Result<FactoryPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(factoryUseCase.getById(id));
    }

    @PostMapping
      @AuditLog(module = "factory", action = "CREATE", resourceType = "factory", resourceId = "#_return")
    @PreAuthorize("hasAuthority('factory:create')")
    public Result<Long> create(@Valid @RequestBody FactoryCreateCmd cmd) {
        Long id = factoryUseCase.create(cmd);
        return Result.ok("工厂创建成功", id);
    }

    @PatchMapping("/{id}")
    @AuditLog(module = "factory", action = "UPDATE", resourceType = "factory", resourceId = "#id")
    @PreAuthorize("hasAuthority('factory:update')")
    public Result<Void> update(@PathVariable("id") Long id,
                                @Valid @RequestBody FactoryUpdateCmd cmd) {
        factoryUseCase.update(id, cmd);
        return Result.ok("工厂更新成功", null);
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "factory", action = "DELETE", resourceType = "factory", resourceId = "#id")
    @PreAuthorize("hasAuthority('factory:delete')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        factoryUseCase.delete(id);
        return Result.ok("工厂删除成功", null);
    }
}
