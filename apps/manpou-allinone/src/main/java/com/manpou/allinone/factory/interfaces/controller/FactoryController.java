package com.manpou.allinone.factory.interfaces.controller;

import com.manpou.allinone.common.result.Result;
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
    public Result<Page<FactoryPageQuery>> list(FactoryQuery query) {
        return Result.ok(factoryUseCase.pageQuery(query));
    }

    @GetMapping("/stats")
    public Result<FactoryStatsDTO> stats() {
        return Result.ok(factoryUseCase.stats());
    }

    @GetMapping("/{id}")
    public Result<FactoryPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(factoryUseCase.getById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody FactoryCreateCmd cmd) {
        Long id = factoryUseCase.create(cmd);
        return Result.ok("工厂创建成功", id);
    }

    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                                @Valid @RequestBody FactoryUpdateCmd cmd) {
        factoryUseCase.update(id, cmd);
        return Result.ok("工厂更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        factoryUseCase.delete(id);
        return Result.ok("工厂删除成功", null);
    }
}
