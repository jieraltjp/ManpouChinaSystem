package com.manpou.allinone.replenishment.interfaces.controller;

import com.manpou.common.result.Result;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandCreateCmd;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandPageQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandUpdateCmd;
import com.manpou.allinone.replenishment.application.usecase.ReplenishmentDemandUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/demands")
@RequiredArgsConstructor
public class ReplenishmentDemandController {

    private final ReplenishmentDemandUseCase demandUseCase;

    @GetMapping
    public Result<Page<ReplenishmentDemandPageQuery>> list(ReplenishmentDemandQuery query) {
        return Result.ok(demandUseCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    public Result<ReplenishmentDemandPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(demandUseCase.getById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ReplenishmentDemandCreateCmd cmd) {
        Long id = demandUseCase.create(cmd);
        return Result.ok("需求单创建成功", id);
    }

    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                                @Valid @RequestBody ReplenishmentDemandUpdateCmd cmd) {
        demandUseCase.update(id, cmd);
        return Result.ok("需求单更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        demandUseCase.delete(id);
        return Result.ok("需求单删除成功", null);
    }

    /**
     * 需求单转为采购单。
     * POST /api/v1/demands/{id}/convert
     */
    @PostMapping("/{id}/convert")
    public Result<Void> convertToProcurement(
            @PathVariable("id") Long id,
            @RequestParam("procurementId") Long procurementId) {
        demandUseCase.convertToProcurement(id, procurementId);
        return Result.ok("需求单已转为采购单", null);
    }

    /**
     * 撤销转换。
     * 将需求单状态回退为 PENDING，清除 linkedProcurementId。
     * POST /api/v1/demands/{id}/revert
     */
    @PostMapping("/{id}/revert")
    public Result<Void> revertConversion(@PathVariable("id") Long id) {
        demandUseCase.revertConversion(id);
        return Result.ok("已撤销转换，需求单可重新转采购", null);
    }
}
