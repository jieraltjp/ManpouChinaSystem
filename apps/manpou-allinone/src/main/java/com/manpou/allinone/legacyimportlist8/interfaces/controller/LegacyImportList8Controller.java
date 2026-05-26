package com.manpou.allinone.legacyimportlist8.interfaces.controller;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.legacyimportlist8.application.dto.CustomsQueryResultVO;
import com.manpou.allinone.legacyimportlist8.application.dto.LegacyImportList8Query;
import com.manpou.allinone.legacyimportlist8.application.dto.LegacyImportList8UpdateCmd;
import com.manpou.allinone.legacyimportlist8.application.dto.LegacyImportList8VO;
import com.manpou.allinone.legacyimportlist8.application.usecase.LegacyImportList8UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/legacy-import-list8")
@RequiredArgsConstructor
public class LegacyImportList8Controller {

    private final LegacyImportList8UseCase useCase;

    @GetMapping
    @PreAuthorize("hasAuthority('offline_order:read')")
    public Result<Page<LegacyImportList8VO>> list(LegacyImportList8Query query) {
        return Result.ok(useCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('offline_order:read')")
    public Result<LegacyImportList8VO> get(@PathVariable("id") Integer id) {
        return Result.ok(useCase.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('offline_order:create')")
    @AuditLog(module = "legacy_import_list8", action = "CREATE", resourceType = "legacy_import_list8", resourceId = "#_return")
    public Result<LegacyImportList8VO> create(@RequestBody LegacyImportList8UpdateCmd cmd) {
        return Result.ok(useCase.create(cmd));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('offline_order:update')")
    @AuditLog(module = "legacy_import_list8", action = "UPDATE", resourceType = "legacy_import_list8", resourceId = "#id")
    public Result<LegacyImportList8VO> update(@PathVariable("id") Integer id,
                                               @RequestBody LegacyImportList8UpdateCmd cmd) {
        return Result.ok(useCase.update(id, cmd));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('offline_order:delete')")
    @AuditLog(module = "legacy_import_list8", action = "DELETE", resourceType = "legacy_import_list8", resourceId = "#id")
    public Result<Void> delete(@PathVariable("id") Integer id) {
        useCase.delete(id);
        return Result.ok();
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('offline_order:read')")
    public Result<Long> count() {
        return Result.ok(useCase.count());
    }

    /**
     * 报关批量查询：根据货号列表精准查询。
     * POST /api/v1/legacy-import-list8/customs-query
     */
    @PostMapping("/customs-query")
    @PreAuthorize("hasAuthority('offline_order:read')")
    public Result<List<CustomsQueryResultVO>> customsQuery(@RequestBody List<String> codes) {
        return Result.ok(useCase.customsQuery(codes));
    }
}
