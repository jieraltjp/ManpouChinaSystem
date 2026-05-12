package com.manpou.allinone.procurement.application.usecase;

import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.repository.FactoryRepository;
import com.manpou.allinone.order.domain.model.ProcurementSnapshot;
import com.manpou.allinone.order.domain.repository.ProcurementSnapshotRepository;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import com.manpou.allinone.procurement.application.assembler.ProcurementAssembler;
import com.manpou.allinone.procurement.application.dto.ProcurementCreateCmd;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementUpdateCmd;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 发注单用例服务。
 * 负责编排业务操作，不含领域逻辑。
 * 领域逻辑（状态机、价格计算）封装在 Procurement 实体中。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementUseCase {

    private final ProcurementRepository procurementRepository;
    private final ProcurementAssembler procurementAssembler;
    private final FactoryRepository factoryRepository;
    private final ProductRepository productRepository;
    private final ProcurementSnapshotRepository snapshotRepository;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<ProcurementPageQuery> pageQuery(ProcurementQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<Procurement> page;
        if (query.getStatus() != null) {
            page = procurementRepository.findByStatusAndDeletedIsFalse(query.getStatus(), pageRequest);
        } else if (query.getProductCode() != null && !query.getProductCode().isBlank()) {
            page = procurementRepository.findByProductCodeAndDeletedIsFalse(query.getProductCode(), pageRequest);
        } else if (query.getCustomerCompany() != null && !query.getCustomerCompany().isBlank()) {
            page = procurementRepository.findByCustomerCompanyAndDeletedIsFalse(query.getCustomerCompany(), pageRequest);
        } else {
            page = procurementRepository.findAllByDeletedIsFalse(pageRequest);
        }
        return page.map(procurementAssembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public ProcurementPageQuery getById(Long id) {
        Procurement entity = procurementRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Procurement", id));
        return procurementAssembler.toDto(entity);
    }

    /**
     * 创建发注单。
     * factoryId 为必填，且工厂必须存在且未被逻辑删除。
     */
    @Transactional
    @PreAuthorize("hasAuthority('procurement:create')")
    public Long create(ProcurementCreateCmd cmd) {
        // 校验关联工厂
        if (cmd.getFactoryId() == null) {
            throw BusinessException.invalidParam("关联工厂不能为空");
        }
        procurementAssembler.toEntity(cmd); // 触发 assembler 中 factoryId 校验

        Procurement entity = procurementAssembler.toEntity(cmd);
        if (entity.getLeadTimeDays() == null) {
            entity.setLeadTimeDays(30);
        }
        entity.calculateEstimatedPriceJpy();
        Procurement saved = procurementRepository.save(entity);

        // 自动填入快照（下单时刻的工厂+商品信息）
        createSnapshot(saved);

        log.info("[Procurement] created, traceId={}, id={}, productCode={}, factoryId={}",
                MDC.get(TraceFilter.TRACE_ID_KEY),
                saved.getId(),
                saved.getProductCode(),
                saved.getFactoryId());
        return saved.getId();
    }

    /**
     * 更新发注单（部分更新，含状态推进）。
     * factoryId 不允许修改。
     */
    @Transactional
    public void update(Long id, ProcurementUpdateCmd cmd) {
        Procurement entity = procurementRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Procurement", id));

        // factoryId 不允许修改
        if (cmd.getFactoryId() != null && !cmd.getFactoryId().equals(entity.getFactoryId())) {
            throw BusinessException.invalidParam("发注单关联工厂不允许修改");
        }

        procurementAssembler.copyToEntity(cmd, entity);
        if (cmd.getPriceRmb() != null || cmd.getExchangeRate() != null || cmd.getTaxPoint() != null) {
            entity.calculateEstimatedPriceJpy();
        }
        procurementRepository.save(entity);

        // productCode/factoryId 变更后刷新快照
        createSnapshot(entity);

        log.info("[Procurement] updated, traceId={}, id={}, productCode={}, status={}",
                MDC.get(TraceFilter.TRACE_ID_KEY), id, entity.getProductCode(), entity.getStatus());
    }

    /**
     * 逻辑删除。
     * 仅未定/発注待状态可删除。
     */
    @Transactional
    public void delete(Long id) {
        Procurement entity = procurementRepository.findByIdAndDeletedIsFalse(id)
                .orElseThrow(() -> BusinessException.notFound("Procurement", id));
        ShipmentStatus current = entity.getStatus();
        if (current != ShipmentStatus.未定 && current != ShipmentStatus.発注待) {
            throw BusinessException.invalidParam(
                    "仅未定/発注待状态可删除，当前状态：" + current);
        }
        entity.markDeleted();
        procurementRepository.save(entity);
        log.info("[Procurement] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 创建或更新发注单快照（下单时刻的工厂+商品信息）。
     * 由 create/update 调用，自动从当前工厂和商品实时数据填充。
     */
    private void createSnapshot(Procurement procurement) {
        Factory factory = null;
        if (procurement.getFactoryId() != null) {
            factory = factoryRepository.findByIdAndDeletedIsFalse(procurement.getFactoryId()).orElse(null);
        }

        Product product = null;
        if (procurement.getProductCode() != null && !procurement.getProductCode().isBlank()) {
            product = productRepository
                    .findByMasterCodeAndSubCodeIsNullAndDeletedIsFalse(procurement.getProductCode())
                    .orElse(null);
        }

        ProcurementSnapshot snapshot = snapshotRepository.findByProcurementId(procurement.getId())
                .orElse(new ProcurementSnapshot());

        snapshot.setProcurementId(procurement.getId());
        if (factory != null) {
            snapshot.setFactoryId(factory.getId());
            snapshot.setFactoryCode(factory.getFactoryCode());
            snapshot.setFactoryName(factory.getFactoryName());
            snapshot.setFactoryProvince(factory.getProvince());
            snapshot.setFactoryCity(factory.getCity());
            snapshot.setFactoryContactName(factory.getContactName());
            snapshot.setFactoryContactPhone(factory.getContactPhone());
        }
        if (product != null) {
            snapshot.setProductNameZh(product.getNameZh());
            snapshot.setProductNameJa(product.getNameJa());
            snapshot.setProductCategory(product.getCategory() != null ? product.getCategory().name() : null);
        }

        snapshotRepository.save(snapshot);
    }
}
