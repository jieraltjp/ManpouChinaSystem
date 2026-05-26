package com.manpou.allinone.product.interfaces.controller;

import com.manpou.allinone.product.application.dto.CustomsQueryResultVO;
import com.manpou.allinone.product.application.dto.MasterCodeSuggestVO;
import com.manpou.allinone.product.application.dto.ProductCategoryVO;
import org.springframework.security.access.prepost.PreAuthorize;
import com.manpou.allinone.product.application.dto.ProductCompleteCmd;
import com.manpou.allinone.product.application.dto.ProductCreateCmd;
import com.manpou.allinone.product.application.dto.ProductPageQuery;
import com.manpou.allinone.product.application.dto.ProductQuery;
import com.manpou.allinone.product.application.dto.ProductUpdateCmd;
import com.manpou.allinone.product.application.dto.ProductFactoryVO;
import com.manpou.allinone.product.application.dto.SubCodeSuggestVO;
import com.manpou.allinone.product.application.usecase.ProductUseCase;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.common.annotation.AuditLog;
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
    @PreAuthorize("hasAuthority('product:read')")
    public Result<Page<ProductPageQuery>> list(ProductQuery query) {
        return Result.ok(productUseCase.pageQuery(query));
    }

    /**
     * 根据 ID 查询单个商品。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:read')")
    public Result<ProductPageQuery> get(@PathVariable Long id) {
        return Result.ok(productUseCase.getById(id));
    }

    /**
     * 根据主货号查询（步骤1商品选择器调用）。
     */
    @GetMapping("/code/{masterCode}")
    @PreAuthorize("hasAuthority('product:read')")
    public Result<ProductPageQuery> getByMasterCode(@PathVariable String masterCode) {
        return Result.ok(productUseCase.getByMasterCode(masterCode));
    }

    /**
     * 主货号自动补全（步骤1补货需求页调用）。
     */
    @GetMapping("/suggest/master-codes")
    @PreAuthorize("hasAuthority('product:read')")
    public Result<List<MasterCodeSuggestVO>> suggestMasterCodes(@RequestParam String keyword) {
        return Result.ok(productUseCase.suggestMasterCodes(keyword));
    }

    /**
     * 子货号候选项（按主货号过滤，步骤1补货需求页调用）。
     */
    @GetMapping("/suggest/sub-codes")
    @PreAuthorize("hasAuthority('product:read')")
    public Result<List<SubCodeSuggestVO>> suggestSubCodes(@RequestParam String masterCode) {
        return Result.ok(productUseCase.suggestSubCodes(masterCode));
    }

    /**
     * 获取商品关联的工厂列表（含工厂详情）。
     * GET /api/v1/products/{id}/factories
     */
    @GetMapping("/{id}/factories")
    @PreAuthorize("hasAuthority('product:read')")
    public Result<List<ProductFactoryVO>> getProductFactories(@PathVariable Long id) {
        return Result.ok(productUseCase.getProductFactories(id));
    }

    /**
     * 报关批量查询：多货号查询单价、税率、仓库、重量、HS编码。
     * POST /api/v1/products/customs-query
     * Body: ["odn012", "abc345", "xyz789"]
     */
    @PostMapping("/customs-query")
    @PreAuthorize("hasAuthority('product:read')")
    public Result<List<CustomsQueryResultVO>> customsQuery(@RequestBody List<String> masterCodes) {
        return Result.ok(productUseCase.customsQuery(masterCodes));
    }

    /**
     * 批量获取商品类别（解决 N+1：替代逐个 GET /code/{masterCode} 调用）。
     * POST /api/v1/products/batch-categories
     */
    @PostMapping("/batch-categories")
    @PreAuthorize("hasAuthority('product:read')")
    public Result<List<ProductCategoryVO>> batchGetCategories(@RequestBody List<String> masterCodes) {
        return Result.ok(productUseCase.batchGetCategories(masterCodes));
    }

    /**
     * 创建商品。
     */
    @PostMapping
      @AuditLog(module = "product", action = "CREATE", resourceType = "product", resourceId = "#_return")
    @Idempotent(ttl = 24 * 60 * 60)
    @PreAuthorize("hasAuthority('product:create')")
    public Result<Long> create(@Valid @RequestBody ProductCreateCmd cmd) {
        Long id = productUseCase.create(cmd);
        return Result.ok("商品创建成功", id);
    }

    /**
     * 更新商品（部分更新）。
     */
    @PatchMapping("/{id}")
    @AuditLog(module = "product", action = "UPDATE", resourceType = "product", resourceId = "#id")
    @PreAuthorize("hasAuthority('product:update')")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ProductUpdateCmd cmd) {
        productUseCase.update(id, cmd);
        return Result.ok("商品更新成功", null);
    }

    /**
     * 删除商品（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @AuditLog(module = "product", action = "DELETE", resourceType = "product", resourceId = "#id")
    @PreAuthorize("hasAuthority('product:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        productUseCase.delete(id);
        return Result.ok("商品删除成功", null);
    }

    // ---- SPEC-B15 货物尺寸管理：不完整品 ----

    @GetMapping("/incomplete")
    @PreAuthorize("hasAuthority('cargo_size:read')")
    public Result<Page<Product>> listIncomplete(
            @RequestParam(defaultValue = "no_name") String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(productUseCase.listIncomplete(type, keyword, page, size));
    }

    @PutMapping("/{id}/complete")
    @AuditLog(module = "product", action = "UPDATE", resourceType = "product", resourceId = "#id")
    @PreAuthorize("hasAuthority('product:update')")
    public Result<Product> complete(@PathVariable Long id,
                                     @Valid @RequestBody ProductCompleteCmd cmd) {
        return Result.ok(productUseCase.complete(id, cmd));
    }
}
