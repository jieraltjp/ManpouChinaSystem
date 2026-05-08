package com.manpou.allinone.procurement.interfaces.controller;

import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchCreateCmd;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchPageQuery;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchQuery;
import com.manpou.allinone.procurement.application.dto.ShipmentBatchUpdateCmd;
import com.manpou.allinone.procurement.application.usecase.ShipmentBatchUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 出货批次 Controller（SPEC-B11 §10.1）。
 */
@RestController
@RequestMapping("/api/v1/shipment-batches")
@RequiredArgsConstructor
public class ShipmentBatchController {

    private final ShipmentBatchUseCase shipmentBatchUseCase;

    /**
     * 分页查询出货批次。
     * GET /api/v1/shipment-batches?procurementId=1&status=待验货&page=1&pageSize=20
     */
    @GetMapping
    @PreAuthorize("hasAuthority('shipment:read')")
    public Result<Page<ShipmentBatchPageQuery>> list(ShipmentBatchQuery query) {
        return Result.ok(shipmentBatchUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('shipment:read')")
    public Result<ShipmentBatchPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(shipmentBatchUseCase.getById(id));
    }

    /**
     * 创建出货批次。
     * POST /api/v1/shipment-batches
     */
    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    @PreAuthorize("hasAuthority('shipment:create')")
    public Result<Long> create(@Valid @RequestBody ShipmentBatchCreateCmd cmd) {
        Long id = shipmentBatchUseCase.create(cmd);
        return Result.ok("出货批次创建成功", id);
    }

    /**
     * 更新出货批次（部分更新，含状态推进）。
     * PATCH /api/v1/shipment-batches/{id}
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('shipment:update')")
    public Result<Void> update(@PathVariable("id") Long id,
                              @Valid @RequestBody ShipmentBatchUpdateCmd cmd) {
        shipmentBatchUseCase.update(id, cmd);
        return Result.ok("出货批次更新成功", null);
    }

    /**
     * 删除出货批次（逻辑删除）。
     * DELETE /api/v1/shipment-batches/{id}
     * 仅待验货状态可删除。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('shipment:delete')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        shipmentBatchUseCase.delete(id);
        return Result.ok("出货批次删除成功", null);
    }

    /**
     * 关联验货记录到出货批次（SPEC-B11 §10.1 link-qc API）。
     * POST /api/v1/shipment-batches/{id}/link-qc
     */
    @PostMapping("/{id}/link-qc")
    @PreAuthorize("hasAuthority('shipment:update')")
    public Result<Void> linkQc(@PathVariable("id") Long id,
                                @RequestBody Map<String, Long> body) {
        Long qcRecordId = body.get("qcRecordId");
        if (qcRecordId == null) {
            return Result.fail("INVALID_PARAM", "qcRecordId不能为空");
        }
        shipmentBatchUseCase.linkQc(id, qcRecordId);
        return Result.ok("验货记录已关联到出货批次", null);
    }
}
