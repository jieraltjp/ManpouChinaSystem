package com.manpou.allinone.product.application.usecase;

import com.manpou.allinone.product.domain.model.CargoSize;
import com.manpou.allinone.product.domain.model.CargoSizeStatus;
import com.manpou.allinone.product.domain.model.ItemSize;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.repository.CargoSizeRepository;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import com.manpou.allinone.product.infrastructure.persistence.jpa.ItemSizeJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * item_size 旧系统数据导入服务。
 * 从 item_size 表读取数据，分两阶段处理：
 *   Phase 1：匹配 Product → 更新尺寸/包材/软删除
 *   Phase 2：未匹配的 → UPSERT cargo_size 表
 *
 * 映射规则（维度命名）：
 *   item_size.height → product.length_cm / cargo_size.length_cm（高→长）
 *   item_size.width  → product.width_cm  / cargo_size.width_cm（宽→宽）
 *   item_size.depth  → product.height_cm / cargo_size.height_cm（深→高）
 *   item_size.pack_height → package_height_cm
 *   item_size.pack_width  → package_width_cm
 *   item_size.pack_depth  → package_length_cm / pack_depth_cm（深→长）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemSizeImportService {

    private final ProductRepository productRepository;
    private final CargoSizeRepository cargoSizeRepository;
    private final ItemSizeJpaRepository itemSizeJpaRepository;
    private final TransactionTemplate tx;

    public record ImportReport(
            int total,
            int inserted,
            int updated,
            int softDeleted,
            int notFound,
            List<String> errors
    ) {
        public int getMatched() { return inserted + updated + softDeleted; }
    }

    /**
     * 手动触发导入。分两阶段：先 Product，再 CargoSize。
     */
    public ImportReport runImport() {
        log.info("========== item_size 导入开始（DB 数据源） ==========");
        List<String> errors = new ArrayList<>();

        List<ItemSize> records = itemSizeJpaRepository.findAll();
        log.info("共读取 {} 条 item_size 记录", records.size());

        // Phase 1：Product（更新/软删除），每条独立事务
        int[] updated = {0}, softDeleted = {0}, phase1Errors = {0};
        for (ItemSize item : records) {
            try {
                Integer r = tx.execute(status -> phase1ProcessRecord(item));
                if (r != null) {
                    switch (r) {
                        case 0 -> updated[0]++;
                        case 1 -> softDeleted[0]++;
                    }
                }
            } catch (Exception e) {
                phase1Errors[0]++;
                errors.add(String.format("record[%s] 错误(Phase1): %s", item.getCode(), e.getMessage()));
                log.error("Phase1 处理记录 {} 失败: {}", item.getCode(), e.getMessage());
            }
        }
        log.info("Phase1 完成（更新: {}，软删除: {}，失败: {}）", updated[0], softDeleted[0], phase1Errors[0]);

        // Phase 2：CargoSize（UPSERT），每条独立事务
        int[] cargoInserted = {0}, cargoUpdated = {0}, phase2Errors = {0};
        for (ItemSize item : records) {
            try {
                Integer r = tx.execute(status -> phase2ProcessRecord(item));
                if (r != null) {
                    switch (r) {
                        case 0 -> cargoInserted[0]++;
                        case 1 -> cargoUpdated[0]++;
                    }
                }
            } catch (Exception e) {
                phase2Errors[0]++;
                errors.add(String.format("record[%s] 错误(Phase2): %s", item.getCode(), e.getMessage()));
                log.error("Phase2 处理记录 {} 失败: {}", item.getCode(), e.getMessage());
            }
        }

        ImportReport report = new ImportReport(records.size(), cargoInserted[0],
                updated[0] + cargoUpdated[0], softDeleted[0],
                cargoInserted[0] + cargoUpdated[0], errors);

        log.info("========== item_size 导入完成 ==========");
        log.info("总计: {}, cargo_size新建: {}, product更新: {}, cargo_size更新: {}, product软删除: {}",
                report.total(), cargoInserted[0], updated[0], cargoUpdated[0], softDeleted[0]);
        if (!errors.isEmpty()) {
            log.warn("共 {} 条错误记录", errors.size());
        }

        return report;
    }

    private Integer phase1ProcessRecord(ItemSize item) {
        Phase1Result r = doPhase1(item);
        if (r == Phase1Result.NOT_FOUND) return null;
        return r.ordinal();
    }

    private Integer phase2ProcessRecord(ItemSize item) {
        Phase2Result r = doPhase2(item);
        if (r == Phase2Result.SKIPPED) return null;
        return r.ordinal();
    }

    private Phase1Result doPhase1(ItemSize item) {
        boolean isSoftDeleted = item.getShowFlag() != null && item.getShowFlag() == 1;

        Optional<Product> existing = findProduct(item.getCode());
        if (existing.isEmpty()) {
            return Phase1Result.NOT_FOUND;
        }

        Product product = existing.get();
        if (isSoftDeleted) {
            product.markDeleted();
            productRepository.save(product);
            log.debug("软删除: {}", item.getCode());
            return Phase1Result.SOFT_DELETED;
        }

        updateProductFields(product, item);
        productRepository.save(product);
        log.debug("更新: {} (h={}, w={}, d={})", item.getCode(), item.getHeight(), item.getWidth(), item.getDepth());
        return Phase1Result.UPDATED;
    }

    private Phase2Result doPhase2(ItemSize item) {
        String code = item.getCode();
        if (code == null) return Phase2Result.SKIPPED;

        // 尝试匹配 Product，Phase 1 已更新过 Product，这里只用于建立关联
        Optional<Product> matchedProduct = findProduct(code);

        return upsertCargoSize(item, matchedProduct);
    }

    /**
     * 匹配策略（按优先级）：
     * 0. master_code = item_code（全量存储）
     * 1. sub_code = item_code（全量匹配）
     * 2.-4. 按 code 拆分 master_code + sub_code 匹配
     */
    private Optional<Product> findProduct(String itemCode) {
        if (itemCode == null) return Optional.empty();

        // 优先级0：item_code 整体作为 master_code
        List<Product> byItemCode = productRepository.findAllByMasterCodeAndDeletedIsFalse(itemCode);
        Optional<Product> match0 = byItemCode.stream()
                .filter(p -> p.getSubCode() == null || p.getSubCode().isBlank()
                        || itemCode.equals(p.getSubCode()))
                .findFirst();
        if (match0.isPresent()) {
            log.trace("匹配0 (master全量): {}", itemCode);
            return match0;
        }

        // 解析 master_code + sub_code（用首个 "-" 拆分）
        int dashIdx = itemCode.indexOf('-');
        final String masterCode = dashIdx > 0 ? itemCode.substring(0, dashIdx) : itemCode;
        final String subCode = dashIdx > 0 ? itemCode.substring(dashIdx + 1) : null;

        List<Product> byMaster = productRepository.findAllByMasterCodeAndDeletedIsFalse(masterCode);

        // 优先级1：sub_code = item_code
        Optional<Product> match1 = byMaster.stream()
                .filter(p -> itemCode.equals(p.getSubCode()))
                .findFirst();
        if (match1.isPresent()) {
            log.trace("匹配1 (subCode全量): {}", itemCode);
            return match1;
        }

        // 优先级2：masterCode + subCode 组合
        if (subCode != null && !subCode.isBlank()) {
            Optional<Product> match2 = byMaster.stream()
                    .filter(p -> subCode.equals(p.getSubCode()))
                    .findFirst();
            if (match2.isPresent()) {
                log.trace("匹配2 (master+sub): {}-{}", masterCode, subCode);
                return match2;
            }
        }

        // 优先级3：无子货号
        Optional<Product> match3 = byMaster.stream()
                .filter(p -> p.getSubCode() == null || p.getSubCode().isBlank())
                .findFirst();
        if (match3.isPresent()) {
            log.trace("匹配3 (无subCode): {}", masterCode);
            return match3;
        }

        // 优先级4：sub_code 以 item_code + "-" 开头
        if (subCode != null && !subCode.isBlank()) {
            String prefix = itemCode + "-";
            Optional<Product> match4 = byItemCode.stream()
                    .filter(p -> p.getSubCode() != null && p.getSubCode().startsWith(prefix))
                    .findFirst();
            if (match4.isPresent()) {
                log.trace("匹配4 (subCode前缀): {} -> sub={}", itemCode, match4.get().getSubCode());
                return match4;
            }
        }

        return Optional.empty();
    }

    private void updateProductFields(Product p, ItemSize item) {
        // 维度映射: item_size.height → length_cm, item_size.depth → height_cm
        p.setLengthCm(item.getHeight());
        p.setWidthCm(item.getWidth());
        p.setHeightCm(item.getDepth());
        p.setNetWeightKg(item.getWeight());

        // 包材映射: pack_depth → package_length_cm, pack_height → package_height_cm
        p.setPackageLengthCm(item.getPackDepth());
        p.setPackageWidthCm(item.getPackWidth());
        p.setPackageHeightCm(item.getPackHeight());
        p.setPackageWeightKg(item.getPackWeightTotal());
        p.setUnitsPerPackage(item.getPackQty());

        // 自动计算体积
        if (item.getHeight() != null && item.getWidth() != null && item.getDepth() != null) {
            p.calculateVolume();
        }
        if (item.getPackDepth() != null && item.getPackWidth() != null && item.getPackHeight() != null) {
            p.calculatePackageVolume();
        }

        // 备注（追加）
        String remarks = item.getOther();
        if (remarks != null && !remarks.isBlank()) {
            String existing = p.getRemarks();
            if (existing != null && !existing.isBlank()) {
                p.setRemarks(existing + "；[item_size] " + remarks);
            } else {
                p.setRemarks("[item_size] " + remarks);
            }
        }
    }

    private Phase2Result upsertCargoSize(ItemSize item, Optional<Product> matchedProduct) {
        String code = item.getCode();
        Optional<CargoSize> existing = cargoSizeRepository.findByCode(code);
        CargoSize cs;
        if (existing.isPresent()) {
            cs = existing.get();
            if (cs.getStatus() == CargoSizeStatus.PROMOTED) {
                log.trace("cargo_size 已升格，跳过: {}", code);
                return Phase2Result.SKIPPED;
            }
        } else {
            cs = new CargoSize();
            cs.setCode(code);
            // 解析 master_code / sub_code
            int dashIdx = code.indexOf('-');
            if (dashIdx > 0) {
                cs.setMasterCode(code.substring(0, dashIdx));
                cs.setSubCode(code.substring(dashIdx + 1));
            } else {
                cs.setMasterCode(code);
            }
            cs.setLegacyId(item.getId());
            cs.setInputUser(item.getInputUser());
            cs.setShowFlag("0");
        }

        // 关联已匹配的 Product
        if (matchedProduct.isPresent() && cs.getProductId() == null) {
            cs.setProductId(matchedProduct.get().getId());
        }

        // 使用源数据的更新时间
        cs.setUpdateTime(item.getUpdateTime());

        cs.setLengthCm(item.getHeight());
        cs.setWidthCm(item.getWidth());
        cs.setHeightCm(item.getDepth());
        cs.setNetWeightKg(item.getWeight());
        cs.setPackHeightCm(item.getPackHeight());
        cs.setPackWidthCm(item.getPackWidth());
        cs.setPackDepthCm(item.getPackDepth());
        cs.setPackageWeightKg(item.getPackWeightTotal());
        cs.setUnitsPerPackage(item.getPackQty());

        String remarks = item.getOther();
        if (remarks != null && !remarks.isBlank()) {
            cs.setRemarks(remarks);
        }

        cargoSizeRepository.save(cs);
        return existing.isPresent() ? Phase2Result.UPDATED : Phase2Result.INSERTED;
    }

    private enum Phase1Result { UPDATED, SOFT_DELETED, NOT_FOUND }
    private enum Phase2Result { INSERTED, UPDATED, SKIPPED }
}