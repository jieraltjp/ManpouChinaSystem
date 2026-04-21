package com.manpou.allinone.warehouse.interfaces.controller;

import com.manpou.allinone.warehouse.application.dto.WarehouseCreateCmd;
import com.manpou.allinone.warehouse.application.dto.WarehousePageQuery;
import com.manpou.allinone.warehouse.application.dto.WarehouseQuery;
import com.manpou.allinone.warehouse.application.dto.WarehouseUpdateCmd;
import com.manpou.allinone.warehouse.application.usecase.WarehouseUseCase;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * Warehouse Controller。
 * 职责：参数校验、调用 Application 层、返回标准化响应。
 * 禁止在此层写业务逻辑。
 */
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseUseCase warehouseUseCase;

    /**
     * 分页查询Warehouse列表。
     */
    @GetMapping
    public Result<Page<WarehousePageQuery>> list(WarehouseQuery query) {
        return Result.ok(warehouseUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询单个Warehouse。
     */
    @GetMapping("/{id}")
    public Result<WarehousePageQuery> get(@PathVariable Long id) {
        return Result.ok(warehouseUseCase.getById(id));
    }

    /**
     * 创建Warehouse。
     * 使用 @Idempotent 注解实现幂等性，防止网络重试导致重复创建。
     * 客户端需在请求头携带 X-Idempotency-Key: {uuid}
     */
    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    public Result<Long> create(@Valid @RequestBody WarehouseCreateCmd cmd) {
        Long id = warehouseUseCase.create(cmd);
        return Result.ok("创建成功", id);
    }

    /**
     * 更新Warehouse。
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody WarehouseUpdateCmd cmd) {
        warehouseUseCase.update(id, cmd);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除Warehouse（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseUseCase.delete(id);
        return Result.ok("删除成功", null);
    }
}
