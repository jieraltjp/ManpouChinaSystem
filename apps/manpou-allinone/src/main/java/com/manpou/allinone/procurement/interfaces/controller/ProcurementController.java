package com.manpou.allinone.procurement.interfaces.controller;

import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
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
 * 职责：参数校验、调用 Application 层、返回标准化响应。
 * 禁止在此层写业务逻辑。
 * TODO Phase A: 替换为真实 ShippingOrder API（对应 docs/business/API-发注管理.md §1）。
 */
@RestController
@RequestMapping("/api/v1/procurements")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementUseCase procurementUseCase;

    /**
     * 分页查询发注单列表。
     */
    @GetMapping
    public Result<Page<ProcurementPageQuery>> list(ProcurementQuery query) {
        return Result.ok(procurementUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询单个发注单。
     */
    @GetMapping("/{id}")
    public Result<ProcurementPageQuery> get(@PathVariable Long id) {
        return Result.ok(procurementUseCase.getById(id));
    }

    /**
     * 创建发注单。
     * TODO Phase A: 替换为真实字段（productCode, quantity, priceRmb, exchangeRate, taxPoint 等）。
     */
    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    public Result<Long> create(@Valid @RequestBody ProcurementCreateCmd cmd) {
        Long id = procurementUseCase.create(cmd);
        return Result.ok("创建成功", id);
    }

    /**
     * 更新发注单（部分更新）。
     * TODO Phase A: 支持状态推进（moveTo 方法）。
     */
    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ProcurementUpdateCmd cmd) {
        procurementUseCase.update(id, cmd);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除发注单（逻辑删除）。
     * 仅未定/未定/発注待状态可删除。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        procurementUseCase.delete(id);
        return Result.ok("删除成功", null);
    }
}
