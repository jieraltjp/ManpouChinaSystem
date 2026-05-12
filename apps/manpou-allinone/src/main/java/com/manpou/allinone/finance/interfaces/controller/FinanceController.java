package com.manpou.allinone.finance.interfaces.controller;

import com.manpou.allinone.finance.application.dto.FinanceCreateCmd;
import com.manpou.allinone.finance.application.dto.FinancePageQuery;
import com.manpou.allinone.finance.application.dto.FinanceQuery;
import com.manpou.allinone.finance.application.dto.FinanceUpdateCmd;
import com.manpou.allinone.finance.application.usecase.FinanceUseCase;
import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * Finance Controller。
 * 职责：参数校验、调用 Application 层、返回标准化响应。
 * 禁止在此层写业务逻辑。
 */
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceUseCase financeUseCase;

    /**
     * 分页查询Finance列表。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('sales:read')")
    public Result<Page<FinancePageQuery>> list(FinanceQuery query) {
        return Result.ok(financeUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询单个Finance。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:read')")
    public Result<FinancePageQuery> get(@PathVariable Long id) {
        return Result.ok(financeUseCase.getById(id));
    }

    /**
     * 创建Finance。
     * 使用 @Idempotent 注解实现幂等性，防止网络重试导致重复创建。
     * 客户端需在请求头携带 X-Idempotency-Key: {uuid}
     */
    @PostMapping
      @AuditLog(module = "finance", action = "CREATE", resourceType = "finance", resourceId = "#_return")
    @Idempotent(ttl = 24 * 60 * 60)
    @PreAuthorize("hasAuthority('sales:create')")
    public Result<Long> create(@Valid @RequestBody FinanceCreateCmd cmd) {
        Long id = financeUseCase.create(cmd);
        return Result.ok("创建成功", id);
    }

    /**
     * 更新Finance。
     */
    @PutMapping("/{id}")
    @AuditLog(module = "finance", action = "UPDATE", resourceType = "finance", resourceId = "#id")
    @PreAuthorize("hasAuthority('sales:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody FinanceUpdateCmd cmd) {
        financeUseCase.update(id, cmd);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除Finance（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @AuditLog(module = "finance", action = "DELETE", resourceType = "finance", resourceId = "#id")
    @PreAuthorize("hasAuthority('sales:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        financeUseCase.delete(id);
        return Result.ok("删除成功", null);
    }
}
