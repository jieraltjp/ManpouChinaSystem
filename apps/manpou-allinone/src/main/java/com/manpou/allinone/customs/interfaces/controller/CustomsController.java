package com.manpou.allinone.customs.interfaces.controller;

import com.manpou.allinone.customs.application.dto.CustomsCreateCmd;
import com.manpou.allinone.customs.application.dto.CustomsPageQuery;
import com.manpou.allinone.customs.application.dto.CustomsQuery;
import com.manpou.allinone.customs.application.dto.CustomsUpdateCmd;
import com.manpou.allinone.customs.application.usecase.CustomsUseCase;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * Customs Controller。
 * 职责：参数校验、调用 Application 层、返回标准化响应。
 * 禁止在此层写业务逻辑。
 */
@RestController
@RequestMapping("/api/v1/customs")
@RequiredArgsConstructor
public class CustomsController {

    private final CustomsUseCase customsUseCase;

    /**
     * 分页查询Customs列表。
     */
    @GetMapping
    public Result<Page<CustomsPageQuery>> list(CustomsQuery query) {
        return Result.ok(customsUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询单个Customs。
     */
    @GetMapping("/{id}")
    public Result<CustomsPageQuery> get(@PathVariable Long id) {
        return Result.ok(customsUseCase.getById(id));
    }

    /**
     * 创建Customs。
     * 使用 @Idempotent 注解实现幂等性，防止网络重试导致重复创建。
     * 客户端需在请求头携带 X-Idempotency-Key: {uuid}
     */
    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    public Result<Long> create(@Valid @RequestBody CustomsCreateCmd cmd) {
        Long id = customsUseCase.create(cmd);
        return Result.ok("创建成功", id);
    }

    /**
     * 更新Customs。
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CustomsUpdateCmd cmd) {
        customsUseCase.update(id, cmd);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除Customs（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customsUseCase.delete(id);
        return Result.ok("删除成功", null);
    }
}
