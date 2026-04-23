package com.manpou.allinone.notification.interfaces.controller;

import com.manpou.allinone.notification.application.dto.NotificationCreateCmd;
import com.manpou.allinone.notification.application.dto.NotificationPageQuery;
import com.manpou.allinone.notification.application.dto.NotificationQuery;
import com.manpou.allinone.notification.application.dto.NotificationUpdateCmd;
import com.manpou.allinone.notification.application.usecase.NotificationUseCase;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * Notification Controller。
 * 职责：参数校验、调用 Application 层、返回标准化响应。
 * 禁止在此层写业务逻辑。
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    /**
     * 分页查询Notification列表。
     */
    @GetMapping
    public Result<Page<NotificationPageQuery>> list(NotificationQuery query) {
        return Result.ok(notificationUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询单个Notification。
     */
    @GetMapping("/{id}")
    public Result<NotificationPageQuery> get(@PathVariable Long id) {
        return Result.ok(notificationUseCase.getById(id));
    }

    /**
     * 创建Notification。
     * 使用 @Idempotent 注解实现幂等性，防止网络重试导致重复创建。
     * 客户端需在请求头携带 X-Idempotency-Key: {uuid}
     */
    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    public Result<Long> create(@Valid @RequestBody NotificationCreateCmd cmd) {
        Long id = notificationUseCase.create(cmd);
        return Result.ok("创建成功", id);
    }

    /**
     * 更新Notification。
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody NotificationUpdateCmd cmd) {
        notificationUseCase.update(id, cmd);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除Notification（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationUseCase.delete(id);
        return Result.ok("删除成功", null);
    }
}
