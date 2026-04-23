package com.manpou.allinone.logistics.interfaces.controller;

import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
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
    public Result<Page<LogisticsPlanPageQuery>> list(LogisticsPlanQuery query) {
        return Result.ok(logisticsPlanUseCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    public Result<LogisticsPlanPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(logisticsPlanUseCase.getById(id));
    }

    @PostMapping
    @Idempotent(ttl = 86400)
    public Result<Long> create(@Valid @RequestBody LogisticsPlanCreateCmd cmd) {
        return Result.ok(logisticsPlanUseCase.create(cmd));
    }

    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody LogisticsPlanUpdateCmd cmd) {
        logisticsPlanUseCase.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        logisticsPlanUseCase.delete(id);
        return Result.ok();
    }
}
