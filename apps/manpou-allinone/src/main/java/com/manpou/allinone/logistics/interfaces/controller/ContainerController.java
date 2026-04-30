package com.manpou.allinone.logistics.interfaces.controller;

import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
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
    public Result<Page<ContainerPageQuery>> list(ContainerQuery query) {
        return Result.ok(useCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    public Result<ContainerPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(useCase.getById(id));
    }

    @PostMapping
    @Idempotent(ttl = 86400)
    public Result<Long> create(@Valid @RequestBody ContainerCreateCmd cmd) {
        return Result.ok(useCase.create(cmd));
    }

    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody ContainerUpdateCmd cmd) {
        useCase.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        useCase.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/plans/{planId}")
    public Result<Void> addPlan(@PathVariable("id") Long containerId,
                                @PathVariable("planId") Long planId) {
        useCase.addPlan(containerId, planId);
        return Result.ok();
    }
}
