package com.manpou.allinone.logistics.interfaces.controller;

import com.manpou.allinone.logistics.application.dto.LogisticsCreateCmd;
import com.manpou.allinone.logistics.application.dto.LogisticsPageQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsQuery;
import com.manpou.allinone.logistics.application.dto.LogisticsUpdateCmd;
import com.manpou.allinone.logistics.application.usecase.LogisticsUseCase;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * Logistics Controller。
 * 职责：参数校验、调用 Application 层、返回标准化响应。
 * 禁止在此层写业务逻辑。
 */
@RestController
@RequestMapping("/api/v1/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsUseCase logisticsUseCase;

    /**
     * 分页查询Logistics列表。
     */
    @GetMapping
    public Result<Page<LogisticsPageQuery>> list(LogisticsQuery query) {
        return Result.ok(logisticsUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询单个Logistics。
     */
    @GetMapping("/{id}")
    public Result<LogisticsPageQuery> get(@PathVariable Long id) {
        return Result.ok(logisticsUseCase.getById(id));
    }

    /**
     * 创建Logistics。
     * 使用 @Idempotent 注解实现幂等性，防止网络重试导致重复创建。
     * 客户端需在请求头携带 X-Idempotency-Key: {uuid}
     */
    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    public Result<Long> create(@Valid @RequestBody LogisticsCreateCmd cmd) {
        Long id = logisticsUseCase.create(cmd);
        return Result.ok("创建成功", id);
    }

    /**
     * 更新Logistics。
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody LogisticsUpdateCmd cmd) {
        logisticsUseCase.update(id, cmd);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除Logistics（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        logisticsUseCase.delete(id);
        return Result.ok("删除成功", null);
    }
}
