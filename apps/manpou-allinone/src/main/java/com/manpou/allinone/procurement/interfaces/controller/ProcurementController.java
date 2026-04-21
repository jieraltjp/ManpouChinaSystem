package com.manpou.allinone.procurement.interfaces.controller;

import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.common.result.Result;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementUpdateCmd;
import com.manpou.allinone.procurement.application.usecase.ProcurementUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 发注单 Controller。
 * 与 docs/business/API-发注管理.md §1 完全对齐。
 * 职责：参数校验、调用 Application 层、返回标准化响应。
 * 禁止在此层写业务逻辑。
 */
@RestController
@RequestMapping("/api/v1/procurements")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementUseCase procurementUseCase;

    /**
     * 分页查询发注单列表。
     * GET /api/v1/procurements?status=未定&productCode=de077&customerCompany=永康&page=1&pageSize=20
     */
    @GetMapping
    public Result<Page<ProcurementPageQuery>> list(ProcurementQuery query) {
        return Result.ok(procurementUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询单个发注单。
     */
    @GetMapping("/{id}")
    public Result<ProcurementPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(procurementUseCase.getById(id));
    }

    /**
     * 创建发注单。
     * POST /api/v1/procurements
     * 计算字段 estimatedPriceJpy 由后端自动计算并存储。
     */
    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    public Result<Long> create(@Valid @RequestBody ProcurementCreateCmd cmd) {
        Long id = procurementUseCase.create(cmd);
        return Result.ok("发注单创建成功", id);
    }

    /**
     * 更新发注单（部分更新，含状态推进）。
     * PATCH /api/v1/procurements/{id}
     * 状态推进规则见 docs/business/SPEC-发注管理流程.md §5。
     */
    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody ProcurementUpdateCmd cmd) {
        procurementUseCase.update(id, cmd);
        return Result.ok("发注单更新成功", null);
    }

    /**
     * 删除发注单（逻辑删除）。
     * DELETE /api/v1/procurements/{id}
     * 仅未定/発注待状态可删除。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        procurementUseCase.delete(id);
        return Result.ok("发注单删除成功", null);
    }
}
