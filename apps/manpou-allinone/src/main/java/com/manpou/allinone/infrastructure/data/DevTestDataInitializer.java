package com.manpou.allinone.infrastructure.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.model.PaymentTerms;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.procurement.domain.model.BillingType;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.replenishment.application.dto.SubProductItemDto;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import com.manpou.allinone.replenishment.domain.model.LinkedDemandItem;
import com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand;
import com.manpou.allinone.replenishment.domain.model.SubProductItem;
import com.manpou.allinone.replenishment.domain.repository.ReplenishmentDemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DevTestDataInitializer implements CommandLineRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FactoryRepository factoryRepository;
    private final ReplenishmentDemandRepository demandRepository;
    private final ProcurementRepository procurementRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (factoryRepository.existsByDeletedIsFalse()) {
            log.info("[DevTestData] 数据已存在，跳过初始化");
            return;
        }

        // ===== Step 1: 工厂基础数据 =====
        Factory f1 = factoryOf("F-20260421-001", "金华市恒旺箱包厂", "浙江省", "金华市", "",
                "金东工业区", "李强", "13805791234", "", "",
                CooperationStatus.ACTIVE, PaymentTerms.NET_30);
        Factory f2 = factoryOf("F-20260421-002", "永康市东亚五金制品厂", "浙江省", "金华市", "永康市",
                "永康镇工业园", "王芳", "13905798876", "", "",
                CooperationStatus.ACTIVE, PaymentTerms.NET_30);
        Factory f3 = factoryOf("F-20260421-003", "广州鑫达皮具厂", "广东省", "广州市", "",
                "白云区狮岭镇", "陈明", "13603001234", "", "",
                CooperationStatus.SUSPENDED, PaymentTerms.NET_30);

        factoryRepository.save(f1);
        factoryRepository.save(f2);
        factoryRepository.save(f3);

        // ===== Step 2: 补货需求数据（v1.6.0） =====
        ReplenishmentDemand d1 = demandOf("DM-20260421-001", DemandType.REPLENISHMENT,
                "odn012", List.of(new SubProductItemDto("odn012-re", 120, "名古屋")),
                "田中太郎", DemandStatus.PENDING, null, "常规补货，本月第三批次");
        ReplenishmentDemand d2 = demandOf("DM-20260421-002", DemandType.REPLENISHMENT,
                "odn012", List.of(new SubProductItemDto("odn012-wh", 80, "久留米")),
                "田中太郎", DemandStatus.PENDING, null, "白色款库存告急");
        ReplenishmentDemand d3 = demandOf("DM-20260421-003", DemandType.NEW_PURCHASE,
                "odn045", List.of(new SubProductItemDto(null, 200, "东京")),
                "山本健一", DemandStatus.CONVERTED,
                List.of(new LinkedDemandItem(1L, null)), "新品试单");
        ReplenishmentDemand d4 = demandOf("DM-20260421-004", DemandType.REPLENISHMENT,
                "cpn101", List.of(new SubProductItemDto(null, 300, "大阪")),
                "佐藤花子", DemandStatus.PENDING, null, "cpn101 大货补货");
        ReplenishmentDemand d5 = demandOf("DM-20260421-005", DemandType.NEW_PURCHASE,
                "odn088", List.of(new SubProductItemDto("odn088-bk", 50, "福冈")),
                "山本健一", DemandStatus.CANCELLED, null, "需求取消，无需采购");

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
                LocalDate.of(2026, 4, 20), "商品担当-张伟", "山本健一", "李明",
                "东京", "东京贸易株式会社", ShipmentStatus.完了, null);

        procurementOf(f2, "cpn101", null, "尼龙布+金属扣", false,
                300, new BigDecimal("35.00"), new BigDecimal("21.5"),
                new BigDecimal("1.1000"), BillingType.ZHE_LU_KAI_PIAO,
                LocalDate.of(2026, 4, 8), LocalDate.of(2026, 4, 18),
                LocalDate.of(2026, 4, 25), "商品担当-张伟", "佐藤花子", "李明",
                "大阪", "大阪商会", ShipmentStatus.発注待, null);

        procurementOf(f3, "odn088", "odn088-bk", "真皮", false,
                50, new BigDecimal("55.00"), new BigDecimal("21.5"),
                new BigDecimal("1.1000"), BillingType.NO_REFUND,
                LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 25),
                LocalDate.of(2026, 5, 5), "商品担当-张伟", "山本健一", "李明",
                "福冈", "福冈商店", ShipmentStatus.未定, null);
    }

    private ReplenishmentDemand demandOf(String code, DemandType type, String productCode,
                                         List<SubProductItemDto> subItems,
                                         String lead, DemandStatus status,
                                         List<LinkedDemandItem> linkedItems,
                                         String remarks) {
        ReplenishmentDemand d = new ReplenishmentDemand();
        d.setDemandCode(code);
        d.setDemandType(type);
        d.setProductCode(productCode);
        d.setSubProductItemsRaw(serializeSubProductItems(subItems));
        d.setJapanLead(lead);
        d.setStatus(status);
        d.setLinkedDemandItemsRaw(serializeLinkedDemandItems(linkedItems));
        d.setRemarks(remarks);
        return d;
    }

    private String serializeSubProductItems(List<SubProductItemDto> items) {
        if (items == null || items.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String serializeLinkedDemandItems(List<LinkedDemandItem> items) {
        if (items == null || items.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Factory factoryOf(String code, String name, String province, String city,
                              String county, String roughLocation,
                              String contactName, String phone, String wechat, String qq,
                              CooperationStatus coopStatus, PaymentTerms paymentTerms) {
        Factory f = new Factory();
        f.setFactoryCode(code);
        f.setFactoryName(name);
        f.setProvince(province);
        f.setCity(city);
        f.setCounty(county);
        f.setRoughLocation(roughLocation);
        f.setContactName(contactName);
        f.setContactPhone(phone);
        f.setContactWechat(wechat);
        f.setContactQq(qq);
        f.setCooperationStatus(coopStatus);
        f.setPaymentTerms(paymentTerms);
        return f;
    }

    private void procurementOf(Factory factory, String productCode, String subProduct,
                                String material, boolean requiresQc, int qty,
                                BigDecimal priceRmb, BigDecimal exRate, BigDecimal taxPoint,
                                BillingType billing, LocalDate orderDate,
                                LocalDate plannedShip, LocalDate actualShip,
                                String productLead, String japanLead, String chinaLead,
                                String destination, String customer,
                                ShipmentStatus status, Long linkedDemandId) {
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
        p.setLinkedDemandId(linkedDemandId);
        p.calculateEstimatedPriceJpy();
        procurementRepository.save(p);
    }
}
