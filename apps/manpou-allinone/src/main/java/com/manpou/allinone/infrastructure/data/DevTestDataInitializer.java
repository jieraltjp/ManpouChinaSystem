package com.manpou.allinone.infrastructure.data;

import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.model.FactoryStatus;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.procurement.domain.model.BillingType;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.repository.ReplenishmentDemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 开发/测试数据初始化器。
 * 仅在 dev / test profile 下激活。
 * 提供 Step 1-2 业务流演示数据：
 * - 3 个工厂
 * - 5 条补货需求（混合状态）
 * - 5 条发注单（混合状态）
 *
 * 与 docs/business/SPEC-发注管理流程.md §3 完全对齐。
 */
@Slf4j
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DevTestDataInitializer implements CommandLineRunner {

    private final FactoryRepository factoryRepository;
    private final ReplenishmentDemandRepository demandRepository;
    private final ProcurementRepository procurementRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (factoryRepository.count() > 0) {
            log.info("[DevTestData] 数据已存在，跳过初始化");
            return;
        }

        // ===== Step 1: 工厂基础数据 =====
        Factory f1 = factoryOf("F-20260421-001", "金华市恒旺箱包厂", "浙江省金华市", "金东工业区",
                "李强", "13805791234", FactoryStatus.ACTIVE);
        Factory f2 = factoryOf("F-20260421-002", "永康市东亚五金制品厂", "浙江省金华市永康市", "永康镇工业园",
                "王芳", "13905798876", FactoryStatus.ACTIVE);
        Factory f3 = factoryOf("F-20260421-003", "广州鑫达皮具厂", "广东省广州市", "白云区狮岭镇",
                "陈明", "13603001234", FactoryStatus.INACTIVE);

        factoryRepository.save(f1);
        factoryRepository.save(f2);
        factoryRepository.save(f3);

        // ===== Step 2: 补货需求数据 =====
        ReplenishmentDemand d1 = demandOf("DM-20260421-001", DemandType.REPLENISHMENT,
                "odn012", "odn012-re", 120, "名古屋", "田中太郎", DemandStatus.PENDING,
                null, "常规补货，本月第三批次");
        ReplenishmentDemand d2 = demandOf("DM-20260421-002", DemandType.REPLENISHMENT,
                "odn012", "odn012-wh", 80, "久留米", "田中太郎", DemandStatus.PENDING,
                null, "白色款库存告急");
        ReplenishmentDemand d3 = demandOf("DM-20260421-003", DemandType.NEW_PURCHASE,
                "odn045", null, 200, "东京", "山本健一", DemandStatus.CONVERTED,
                1L, "新品试单");
        ReplenishmentDemand d4 = demandOf("DM-20260421-004", DemandType.REPLENISHMENT,
                "cpn101", null, 300, "大阪", "佐藤花子", DemandStatus.PENDING,
                null, "cpn101 大货补货");
        ReplenishmentDemand d5 = demandOf("DM-20260421-005", DemandType.NEW_PURCHASE,
                "odn088", "odn088-bk", 50, "福冈", "山本健一", DemandStatus.CANCELLED,
                null, "需求取消，无需采购");

        demandRepository.save(d1);
        demandRepository.save(d2);
        demandRepository.save(d3);
        demandRepository.save(d4);
        demandRepository.save(d5);

        // ===== Step 3: 发注单数据 =====
        procurementOf(f1, "odn012", "odn012-re", "PU面料", true,
                120, new BigDecimal("28.50"), new BigDecimal("21.5"),
                new BigDecimal("1.1000"), BillingType.ZHE_LU_KAI_PIAO,
                LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 25), "商品担当-张伟", "田中太郎", "李明",
                "久留米", "久留米贸易株式会社", ShipmentStatus.未定, null);

        procurementOf(f1, "odn012", "odn012-wh", "PU面料", true,
                80, new BigDecimal("28.50"), new BigDecimal("21.5"),
                new BigDecimal("1.1000"), BillingType.ZHE_LU_KAI_PIAO,
                LocalDate.of(2026, 4, 12), LocalDate.of(2026, 4, 22),
                LocalDate.of(2026, 4, 26), "商品担当-张伟", "田中太郎", "李明",
                "久留米", "久留米贸易株式会社", ShipmentStatus.未定, null);

        procurementOf(f2, "odn045", null, "金属配件+PU", false,
                200, new BigDecimal("45.00"), new BigDecimal("21.5"),
                new BigDecimal("1.1000"), BillingType.CHAO_HUI_TUI_SHUI,
                LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 18), "商品担当-王丽", "山本健一", "李明",
                "东京", "东京通商株式会社", ShipmentStatus.国内通関, "ODN045-出口报关");

        procurementOf(f2, "cpn101", null, "帆布", true,
                300, new BigDecimal("35.00"), new BigDecimal("21.5"),
                new BigDecimal("1.1000"), BillingType.ZHE_LU_KAI_PIAO,
                LocalDate.of(2026, 4, 8), LocalDate.of(2026, 4, 18),
                null, "商品担当-王丽", "佐藤花子", "李明",
                "大阪", "大阪商业采购", ShipmentStatus.発注待, null);

        procurementOf(f3, "odn088", "odn088-bk", "真皮+金属", false,
                50, new BigDecimal("88.00"), new BigDecimal("21.5"),
                new BigDecimal("1.1000"), BillingType.ZHE_LU_KAI_PIAO,
                LocalDate.of(2026, 3, 28), LocalDate.of(2026, 4, 8),
                LocalDate.of(2026, 4, 10), "商品担当-张伟", "山本健一", "李明",
                "福冈", "福冈零售", ShipmentStatus.完了, null);

        log.info("[DevTestData] 初始化完成: 3 工厂 / 5 需求 / 5 发注单");
    }

    private Factory factoryOf(String code, String name, String location,
                               String roughLocation, String contact, String phone,
                               FactoryStatus status) {
        Factory f = new Factory();
        f.setFactoryCode(code);
        f.setFactoryName(name);
        f.setLocation(location);
        f.setRoughLocation(roughLocation);
        f.setContactName(contact);
        f.setContactPhone(phone);
        f.setStatus(status);
        return f;
    }

    private ReplenishmentDemand demandOf(String code, DemandType type, String productCode,
                                          String subProduct, Integer qty, String destination,
                                          String lead, DemandStatus status,
                                          Long linkedProcId, String remarks) {
        ReplenishmentDemand d = new ReplenishmentDemand();
        d.setDemandCode(code);
        d.setDemandType(type);
        d.setProductCode(productCode);
        d.setSubProductCode(subProduct);
        d.setQuantity(qty);
        d.setDestination(destination);
        d.setJapanLead(lead);
        d.setStatus(status);
        d.setLinkedProcurementId(linkedProcId);
        d.setRemarks(remarks);
        return d;
    }

    private void procurementOf(Factory factory, String productCode, String subProduct,
                                 String material, boolean requiresQc, int qty,
                                 BigDecimal priceRmb, BigDecimal exRate, BigDecimal taxPoint,
                                 BillingType billing, LocalDate orderDate,
                                 LocalDate plannedShip, LocalDate actualShip,
                                 String productLead, String japanLead, String chinaLead,
                                 String destination, String customer,
                                 ShipmentStatus status, String customsRemarks) {
        Procurement p = new Procurement();
        p.setFactoryId(factory.getId());
        p.setProductCode(productCode);
        p.setSubProductCode(subProduct);
        p.setMaterial(material);
        p.setRequiresQc(requiresQc);
        p.setQuantity(qty);
        p.setPriceRmb(priceRmb);
        p.setExchangeRate(exRate);
        p.setTaxPoint(taxPoint);
        p.setBillingType(billing);
        p.setOrderDate(orderDate);
        p.setPlannedShipDate(plannedShip);
        p.setActualShipDate(actualShip);
        p.setProductLead(productLead);
        p.setJapanLead(japanLead);
        p.setChinaLead(chinaLead);
        p.setDestination(destination);
        p.setCustomerCompany(customer);
        p.setStatus(status);
        p.setCustomsRemarks(customsRemarks);
        p.calculateEstimatedPriceJpy();
        procurementRepository.save(p);
    }
}
