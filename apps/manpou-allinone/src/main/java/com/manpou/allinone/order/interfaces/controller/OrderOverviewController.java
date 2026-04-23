package com.manpou.allinone.order.interfaces.controller;

import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.order.application.dto.OrderOverviewPageVO;
import com.manpou.allinone.order.application.usecase.OrderOverviewUseCase;
import com.manpou.allinone.procurement.application.assembler.ProcurementAssembler;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.procurement.application.dto.ProcurementQuery;
import com.manpou.allinone.procurement.domain.model.Procurement;
import com.manpou.allinone.procurement.domain.repository.ProcurementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 订单总览控制器。
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderOverviewController {

    private final OrderOverviewUseCase orderOverviewUseCase;
    private final ProcurementRepository procurementRepository;
    private final ProcurementAssembler procurementAssembler;

    @GetMapping("/{procurementId}/overview")
    public Result<OrderOverviewPageVO> getOverview(@PathVariable("procurementId") Long procurementId) {
        return Result.ok(orderOverviewUseCase.getOverview(procurementId));
    }

    @GetMapping("/selector")
    public Result<Page<ProcurementPageQuery>> selector(ProcurementQuery query) {
        PageRequest pageRequest = PageRequest.of(
                query.getPage(),
                Math.min(query.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<Procurement> page = procurementRepository.findAllByDeletedIsFalse(pageRequest);
        return Result.ok(page.map(procurementAssembler::toDto));
    }
}
