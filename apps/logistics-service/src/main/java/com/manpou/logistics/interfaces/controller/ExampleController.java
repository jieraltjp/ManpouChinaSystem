package com.manpou.logistics.interfaces.controller;

import com.manpou.logistics.application.dto.ExampleCreateCmd;
import com.manpou.logistics.application.dto.ExamplePageQuery;
import com.manpou.logistics.application.dto.ExampleQuery;
import com.manpou.logistics.application.dto.ExampleUpdateCmd;
import com.manpou.logistics.application.usecase.ExampleUseCase;
import com.manpou.logistics.common.annotation.Idempotent;
import com.manpou.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ExampleController.class);

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
        log.info("[AUDIT] {} updated id={}", "logistics", id);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除示例（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        exampleUseCase.delete(id);
        log.info("[AUDIT] {} deleted id={}", "logistics", id);
        return Result.ok("删除成功", null);
    }
}
