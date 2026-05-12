package com.manpou.allinone.replenishment.interfaces.controller;

import java.util.List;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.procurement.application.dto.ProcurementPageQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandCreateCmd;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandPageQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandQuery;
import com.manpou.allinone.replenishment.application.dto.ReplenishmentDemandUpdateCmd;
import com.manpou.allinone.replenishment.application.usecase.ReplenishmentDemandUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/demands")
@RequiredArgsConstructor
public class ReplenishmentDemandController {

    private final ReplenishmentDemandUseCase demandUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('demand:read')")
    public Result<Page<ReplenishmentDemandPageQuery>> list(ReplenishmentDemandQuery query) {
        return Result.ok(demandUseCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('demand:read')")
    public Result<ReplenishmentDemandPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(demandUseCase.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('demand:create')")
    @AuditLog(module = "replenishment", action = "CREATE", resourceType = "demand", resourceId = "#_return")
    public Result<Long> create(@Valid @RequestBody ReplenishmentDemandCreateCmd cmd) {
        Long id = demandUseCase.create(cmd);
        return Result.ok("需求单创建成功", id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('demand:update')")
    @AuditLog(module = "replenishment", action = "UPDATE", resourceType = "demand", resourceId = "#id")
    public Result<Void> update(@PathVariable("id") Long id,
                                @Valid @RequestBody ReplenishmentDemandUpdateCmd cmd) {
        demandUseCase.update(id, cmd);
        return Result.ok("需求单更新成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('demand:delete')")
    @AuditLog(module = "replenishment", action = "DELETE", resourceType = "demand", resourceId = "#id")
    public Result<Void> delete(@PathVariable("id") Long id) {
        demandUseCase.delete(id);
        return Result.ok("需求单删除成功", null);
    }

    /**
     * 关联到发注单（v2.2.0）。
     * POST /api/v1/demands/{id}/link?procurementId={procurementId}
     */
    @PostMapping("/{id}/link")
    @PreAuthorize("hasAuthority('demand:update')")
    @AuditLog(module = "replenishment", action = "UPDATE", resourceType = "demand", resourceId = "#id")
    public Result<Void> linkToProcurement(
            @PathVariable("id") Long id,
            @RequestParam("procurementId") Long procurementId) {
        demandUseCase.linkToProcurement(id, procurementId);
        return Result.ok("需求单已关联发注单", null);
    }

    /**
     * 取消关联（v2.2.0）。
     * POST /api/v1/demands/{id}/unlink
     */
    @PostMapping("/{id}/unlink")
    @PreAuthorize("hasAuthority('demand:update')")
    @AuditLog(module = "replenishment", action = "UPDATE", resourceType = "demand", resourceId = "#id")
    public Result<Void> unlinkProcurement(@PathVariable("id") Long id) {
        demandUseCase.unlinkProcurement(id);
        return Result.ok("已取消关联", null);
    }

    /**
     * 查看关联的采购单（v2.0.0）。
     * GET /api/v1/demands/{id}/procurement
     */
    @GetMapping("/{id}/procurement")
    @PreAuthorize("hasAuthority('demand:read')")
    public Result<ProcurementPageQuery> getLinkedProcurement(@PathVariable("id") Long id) {
        return Result.ok(demandUseCase.getLinkedProcurement(id));
    }

    /**
     * 目的地建议（去重，已录入的目的地列表）。
     */
    @GetMapping("/suggest/destinations")
    @PreAuthorize("hasAuthority('demand:read')")
    public Result<List<String>> suggestDestinations() {
        return Result.ok(demandUseCase.findDistinctDestinations());
    }

    /**
     * 日本担当建议（去重，已录入的担当列表）。
     */
    @GetMapping("/suggest/japan-leads")
    @PreAuthorize("hasAuthority('demand:read')")
    public Result<List<String>> suggestJapanLeads() {
        return Result.ok(demandUseCase.findDistinctJapanLeads());
    }
}
