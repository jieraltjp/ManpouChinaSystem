package com.manpou.allinone.product.interfaces.controller;

import com.manpou.allinone.product.application.dto.MasterCodeSuggestVO;
import com.manpou.allinone.product.application.dto.ProductCreateCmd;
import com.manpou.allinone.product.application.dto.ProductPageQuery;
import com.manpou.allinone.product.application.dto.ProductQuery;
import com.manpou.allinone.product.application.dto.ProductUpdateCmd;
import com.manpou.allinone.product.application.dto.SubCodeSuggestVO;
import com.manpou.allinone.product.application.usecase.ProductUseCase;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品 Controller。
 * 对应 docs/business/SPEC-B10-商品目录-产品管理.md §2.1 API 设计。
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
     * GET /api/v1/products?keyword=柜子&page=1&pageSize=20
     * GET /api/v1/products?masterCode=in041&page=1&pageSize=20
     * GET /api/v1/products?hsCode=9403200000
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
     * 根据主货号查询（步骤1商品选择器调用）。
     */
    @GetMapping("/code/{masterCode}")
    public Result<ProductPageQuery> getByMasterCode(@PathVariable String masterCode) {
        return Result.ok(productUseCase.getByMasterCode(masterCode));
    }

    /**
     * 主货号自动补全（步骤1补货需求页调用）。
     */
    @GetMapping("/suggest/master-codes")
    public Result<List<MasterCodeSuggestVO>> suggestMasterCodes(@RequestParam String keyword) {
        return Result.ok(productUseCase.suggestMasterCodes(keyword));
    }

    /**
     * 子货号候选项（按主货号过滤，步骤1补货需求页调用）。
     */
    @GetMapping("/suggest/sub-codes")
    public Result<List<SubCodeSuggestVO>> suggestSubCodes(@RequestParam String masterCode) {
        return Result.ok(productUseCase.suggestSubCodes(masterCode));
    }

    /**
     * 创建商品。
     */
    @PostMapping
    @Idempotent(ttl = 24 * 60 * 60)
    public Result<Long> create(@Valid @RequestBody ProductCreateCmd cmd) {
        Long id = productUseCase.create(cmd);
        return Result.ok("商品创建成功", id);
    }

    /**
     * 更新商品（部分更新）。
     */
    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ProductUpdateCmd cmd) {
        productUseCase.update(id, cmd);
        return Result.ok("商品更新成功", null);
    }

    /**
     * 删除商品（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productUseCase.delete(id);
        return Result.ok("商品删除成功", null);
    }
}
