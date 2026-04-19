package com.manpou.product.interfaces.controller;

import com.manpou.product.application.dto.ExampleCreateCmd;
import com.manpou.product.application.dto.ExamplePageQuery;
import com.manpou.product.application.dto.ExampleQuery;
import com.manpou.product.application.dto.ExampleUpdateCmd;
import com.manpou.product.application.usecase.ExampleUseCase;
import com.manpou.product.common.annotation.Idempotent;
import com.manpou.product.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 示例 Controller。
 * 职责：参数校验、调用 Application 层、返回标准化响应。
 * 禁止在此层写业务逻辑。
 */
@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleUseCase exampleUseCase;

    /**
     * 分页查询示例列表。
     */
    @GetMapping
    public Result<Page<ExamplePageQuery>> list(ExampleQuery query) {
        return Result.ok(exampleUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询单个示例。
     */
    @GetMapping("/{id}")
    public Result<ExamplePageQuery> get(@PathVariable Long id) {
        return Result.ok(exampleUseCase.getById(id));
    }

    /**
     * 创建示例。
     * 使用 @Idempotent 注解实现幂等性，防止网络重试导致重复创建。
     * 客户端需在请求头携带 X-Idempotency-Key: {uuid}
     */
    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    public Result<Long> create(@Valid @RequestBody ExampleCreateCmd cmd) {
        Long id = exampleUseCase.create(cmd);
        return Result.ok("创建成功", id);
    }

    /**
     * 更新示例。
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ExampleUpdateCmd cmd) {
        exampleUseCase.update(id, cmd);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除示例（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        exampleUseCase.delete(id);
        return Result.ok("删除成功", null);
    }
}
