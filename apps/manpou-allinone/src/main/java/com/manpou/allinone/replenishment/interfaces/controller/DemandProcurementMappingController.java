package com.manpou.allinone.replenishment.interfaces.controller;

import com.manpou.allinone.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import com.manpou.allinone.replenishment.application.dto.DemandProcurementMappingCreateCmd;
import com.manpou.allinone.replenishment.application.dto.DemandProcurementMappingPageQuery;
import com.manpou.allinone.replenishment.application.dto.DemandProcurementMappingQuery;
import com.manpou.allinone.replenishment.application.usecase.DemandProcurementMappingUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 需求-采购分配映射 Controller（SPEC-B11 §10.2）。
 */
@RestController
@RequestMapping("/api/v1/demand-mappings")
@RequiredArgsConstructor
public class DemandProcurementMappingController {

    private final DemandProcurementMappingUseCase mappingUseCase;

    /**
     * 分页查询分配映射。
     * GET /api/v1/demand-mappings?demandId=1&page=1&pageSize=20
     */
    @GetMapping
    @PreAuthorize("hasAuthority('demand:read')")
    public Result<Page<DemandProcurementMappingPageQuery>> list(DemandProcurementMappingQuery query) {
        return Result.ok(mappingUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('demand:read')")
    public Result<DemandProcurementMappingPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(mappingUseCase.getById(id));
    }

    /**
     * 创建分配映射（含子货号一致性校验）。
     * POST /api/v1/demand-mappings
     */
    @PostMapping
    @PreAuthorize("hasAuthority('demand:create')")
    public Result<Long> create(@Valid @RequestBody DemandProcurementMappingCreateCmd cmd) {
        Long id = mappingUseCase.create(cmd);
        return Result.ok("分配映射创建成功", id);
    }

    /**
     * 取消分配。
     * DELETE /api/v1/demand-mappings/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('demand:delete')")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        mappingUseCase.cancel(id);
        return Result.ok("分配映射已取消", null);
    }
}
