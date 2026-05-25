package com.manpou.allinone.legacyprocurement.interfaces.controller;

import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementCreateCmd;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementPageVO;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementQuery;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementStatsDTO;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementUpdateCmd;
import com.manpou.allinone.legacyprocurement.application.usecase.LegacyProcurementUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/legacy-procurements")
@RequiredArgsConstructor
public class LegacyProcurementController {

    private final LegacyProcurementUseCase useCase;

    @GetMapping
    @PreAuthorize("hasAuthority('legacy_procurement:read')")
    public Result<Page<LegacyProcurementPageVO>> list(LegacyProcurementQuery query) {
        return Result.ok(useCase.pageQuery(query));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('legacy_procurement:read')")
    public Result<LegacyProcurementStatsDTO> stats() {
        return Result.ok(useCase.stats());
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('legacy_procurement:read')")
    public Result<List<LegacyProcurementPageVO>> overdueAll() {
        return Result.ok(useCase.overdueAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('legacy_procurement:read')")
    public Result<LegacyProcurementPageVO> get(@PathVariable("id") Integer id) {
        return Result.ok(useCase.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('legacy_procurement:create')")
    public Result<LegacyProcurementPageVO> create(@RequestBody LegacyProcurementCreateCmd cmd) {
        return Result.ok(useCase.create(cmd));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('legacy_procurement:update')")
    public Result<LegacyProcurementPageVO> update(@PathVariable("id") Integer id,
                                                   @RequestBody LegacyProcurementUpdateCmd cmd) {
        return Result.ok(useCase.update(id, cmd));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('legacy_procurement:delete')")
    public Result<Void> delete(@PathVariable("id") Integer id) {
        useCase.softDelete(id);
        return Result.ok();
    }
}