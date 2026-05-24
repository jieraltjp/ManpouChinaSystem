package com.manpou.allinone.order.interfaces.controller;

import com.manpou.allinone.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import com.manpou.allinone.order.application.dto.DemandOverviewVO;
import com.manpou.allinone.order.application.dto.OrderChainDetailVO;
import com.manpou.allinone.order.application.dto.OrderChainVO;
import com.manpou.allinone.order.application.dto.OrderDemandSelectorDTO;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO;
import com.manpou.allinone.order.application.dto.OrderProcurementSelectorDTO;
import com.manpou.allinone.order.application.usecase.OrderChainUseCase;
import com.manpou.allinone.order.application.usecase.OrderOverviewUseCase;
import com.manpou.allinone.procurement.application.assembler.ProcurementAssembler;
import com.manpou.allinone.procurement.application.dto.ProcurementQuery;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.repository.ReplenishmentDemandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 订单总览控制器（Demand + Procurement 双入口）。
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderOverviewController {

    private final OrderOverviewUseCase orderOverviewUseCase;
    private final OrderChainUseCase orderChainUseCase;
    private final ProcurementRepository procurementRepository;
    private final ReplenishmentDemandRepository demandRepository;
    private final ProcurementAssembler procurementAssembler;

    // ===== Procurement 锚点（已有）=====

    @GetMapping("/procurement/{procurementId}/overview")
    @PreAuthorize("hasAuthority('procurement:read')")
    public Result<OrderOverviewPageVO> getOverview(@PathVariable("procurementId") Long procurementId) {
        return Result.ok(orderOverviewUseCase.getOverview(procurementId));
    }

    @GetMapping("/procurement/selector")
    @PreAuthorize("hasAuthority('procurement:read')")
    public Result<Page<OrderProcurementSelectorDTO>> selector(ProcurementQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<Procurement> page = procurementRepository.findAllByDeletedIsFalse(pageRequest);
        return Result.ok(page.map(procurementAssembler::toOrderProcurementSelectorDto));
    }

    // ===== Demand 锚点（新增）=====

    @GetMapping("/demands")
    @PreAuthorize("hasAuthority('procurement:read')")
    public Result<Page<OrderDemandSelectorDTO>> listDemands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        PageRequest pageRequest = PageRequest.of(
                page,
                Math.min(pageSize, 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<com.manpou.allinone.replenishment.domain.model.ReplenishmentDemand> demandPage;
        if (status != null && !status.isBlank()) {
            DemandStatus demandStatus = DemandStatus.valueOf(status.toUpperCase());
            demandPage = demandRepository.findByStatusAndDeletedIsFalse(demandStatus, pageRequest);
        } else {
            demandPage = demandRepository.findAllByDeletedIsFalse(pageRequest);
        }

        return Result.ok(demandPage.map(orderOverviewUseCase::toDemandSelectorDto));
    }

    @GetMapping("/demands/{demandId}/overview")
    @PreAuthorize("hasAuthority('procurement:read')")
    public Result<DemandOverviewVO> getDemandOverview(@PathVariable Long demandId) {
        return Result.ok(orderOverviewUseCase.getDemandOverview(demandId));
    }

    // ===== v_order_chain 视图端点（Phase1 步骤1~4）=====

    /**
     * 订单总览列表（一行 = 一个 Demand，左 JOIN 串联步骤1~4）。
     * 排序由 OrderChainUseCase 在内存中按 demandCreateTime DESC 实现。
     */
    @GetMapping("/chain")
    @PreAuthorize("hasAuthority('procurement:read')")
    public Result<Page<OrderChainVO>> listChain(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String demandStatus,
            @RequestParam(required = false) String keyword) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(pageSize, 100));
        return Result.ok(orderChainUseCase.getChainList(demandStatus, keyword, pageRequest));
    }

    /**
     * 订单总览详情（指定 Demand 的全链路步骤1~9数据）。
     */
    @GetMapping("/chain/{demandId}")
    @PreAuthorize("hasAuthority('procurement:read')")
    public Result<OrderChainDetailVO> getChainDetail(@PathVariable Long demandId) {
        return Result.ok(orderChainUseCase.getChainDetail(demandId));
    }

    /**
     * 删除 Demand 及其关联的全链路数据（级联软删除）。
     */
    @DeleteMapping("/chain/{demandId}")
    @PreAuthorize("hasAuthority('procurement:delete')")
    public Result<Void> deleteChain(@PathVariable Long demandId) {
        orderChainUseCase.deleteChain(demandId);
        return Result.ok();
    }
}
