package com.manpou.allinone.logistics.interfaces.controller;

import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolCreateCmd;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolPageQuery;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolQuery;
import com.manpou.allinone.logistics.application.dto.ConsolidationPoolUpdateCmd;
import com.manpou.allinone.logistics.application.usecase.ConsolidationPoolUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 拼柜池 REST 接口（v1.5.0，SPEC-B00 Issue #8）。
 */
@RestController
@RequestMapping("/api/v1/consolidation-pools")
@RequiredArgsConstructor
public class ConsolidationPoolController {

    private final ConsolidationPoolUseCase useCase;

    @GetMapping
    public Result<Page<ConsolidationPoolPageQuery>> list(ConsolidationPoolQuery query) {
        return Result.ok(useCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    public Result<ConsolidationPoolPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(useCase.getById(id));
    }

    @PostMapping
    @Idempotent(ttl = 86400)
    public Result<Long> create(@Valid @RequestBody ConsolidationPoolCreateCmd cmd) {
        return Result.ok(useCase.create(cmd));
    }

    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody ConsolidationPoolUpdateCmd cmd) {
        useCase.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        useCase.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/plans/{planId}")
    public Result<Void> addPlan(@PathVariable("id") Long poolId,
                                @PathVariable("planId") Long planId) {
        useCase.addPlan(poolId, planId);
        return Result.ok();
    }

    @DeleteMapping("/{id}/plans/{planId}")
    public Result<Void> removePlan(@PathVariable("id") Long poolId,
                                   @PathVariable("planId") Long planId) {
        useCase.removePlan(poolId, planId);
        return Result.ok();
    }
}
