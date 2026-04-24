package com.manpou.allinone.replenishment.interfaces.controller;

import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.replenishment.application.dto.ConvertDemandCmd;
import com.manpou.allinone.replenishment.application.dto.ConvertDemandResponse;
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
     * 转采购（v2.0.0）。
     * POST /api/v1/demands/{id}/convert
     * 一条 Demand → 一条 Procurement（1:1）。
     */
    @PostMapping("/{id}/convert")
    public Result<ConvertDemandResponse> convertToProcurement(
            @PathVariable("id") Long id,
            @Valid @RequestBody ConvertDemandCmd cmd) {
        ConvertDemandResponse response = demandUseCase.convertToProcurement(id, cmd);
        return Result.ok("已转采购，生成发注单 #" + response.getLinkedProcurementId(), response);
    }

    /**
     * 撤销转换（v2.0.0）。
     * POST /api/v1/demands/{id}/revert
     */
    @PostMapping("/{id}/revert")
    public Result<Void> revertConversion(@PathVariable("id") Long id) {
        demandUseCase.revertConversion(id);
        return Result.ok("已撤销转换，需求单可重新转采购", null);
    }

    /**
     * 查看关联的采购单（v2.0.0）。
     * GET /api/v1/demands/{id}/procurement
     */
    @GetMapping("/{id}/procurement")
    public Result<ProcurementPageQuery> getLinkedProcurement(@PathVariable("id") Long id) {
        return Result.ok(demandUseCase.getLinkedProcurement(id));
    }
}
