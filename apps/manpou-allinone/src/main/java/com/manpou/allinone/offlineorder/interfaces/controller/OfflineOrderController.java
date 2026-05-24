package com.manpou.allinone.offlineorder.interfaces.controller;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.offlineorder.application.dto.OfflineOrderCreateCmd;
import com.manpou.allinone.offlineorder.application.dto.OfflineOrderPageVO;
import com.manpou.allinone.offlineorder.application.dto.OfflineOrderQuery;
import com.manpou.allinone.offlineorder.application.dto.OfflineOrderUpdateCmd;
import com.manpou.allinone.offlineorder.application.usecase.OfflineOrderUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/offline-orders")
@RequiredArgsConstructor
public class OfflineOrderController {

    private final OfflineOrderUseCase useCase;

    @GetMapping
    @PreAuthorize("hasAuthority('offline_order:read')")
    public Result<Page<OfflineOrderPageVO>> list(OfflineOrderQuery query) {
        return Result.ok(useCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('offline_order:read')")
    public Result<OfflineOrderPageVO> get(@PathVariable("id") Long id) {
        return Result.ok(useCase.getById(id));
    }

    @PostMapping
    @AuditLog(module = "offline_order", action = "CREATE", resourceType = "offline_order", resourceId = "#_return")
    @PreAuthorize("hasAuthority('offline_order:create')")
    public Result<Long> create(@Valid @RequestBody OfflineOrderCreateCmd cmd) {
        Long id = useCase.create(cmd);
        return Result.ok("线下订单创建成功", id);
    }

    @PatchMapping("/{id}")
    @AuditLog(module = "offline_order", action = "UPDATE", resourceType = "offline_order", resourceId = "#id")
    @PreAuthorize("hasAuthority('offline_order:update')")
    public Result<Void> update(@PathVariable("id") Long id,
                                @Valid @RequestBody OfflineOrderUpdateCmd cmd) {
        useCase.update(id, cmd);
        return Result.ok("线下订单更新成功", null);
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "offline_order", action = "DELETE", resourceType = "offline_order", resourceId = "#id")
    @PreAuthorize("hasAuthority('offline_order:delete')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        useCase.delete(id);
        return Result.ok("线下订单删除成功", null);
    }
}