package com.manpou.allinone.product.interfaces.controller;

import com.manpou.allinone.product.application.dto.ProductCreateCmd;
import com.manpou.allinone.product.application.dto.ProductPageQuery;
import com.manpou.allinone.product.application.dto.ProductQuery;
import com.manpou.allinone.product.application.dto.ProductUpdateCmd;
import com.manpou.allinone.product.application.usecase.ProductUseCase;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductUseCase productUseCase;

    /**
     * 分页查询商品列表。
     */
    @GetMapping
    public Result<Page<ProductPageQuery>> list(ProductQuery query) {
        return Result.ok(productUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询单个商品。
     */
    @GetMapping("/{id}")
    public Result<ProductPageQuery> get(@PathVariable Long id) {
        return Result.ok(productUseCase.getById(id));
    }

    /**
     * 创建商品。
     * 使用 @Idempotent 注解实现幂等性，防止网络重试导致重复创建。
     * 客户端需在请求头携带 X-Idempotency-Key: {uuid}
     */
    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    public Result<Long> create(@Valid @RequestBody ProductCreateCmd cmd) {
        Long id = productUseCase.create(cmd);
        return Result.ok("创建成功", id);
    }

    /**
     * 更新商品。
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ProductUpdateCmd cmd) {
        productUseCase.update(id, cmd);
        return Result.ok("更新成功", null);
    }

    /**
     * 删除商品（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productUseCase.delete(id);
        return Result.ok("删除成功", null);
    }
}
