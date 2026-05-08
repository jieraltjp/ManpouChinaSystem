package com.manpou.allinone.qc.interfaces.controller;

import com.manpou.allinone.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import com.manpou.allinone.qc.application.dto.QcRecordCreateCmd;
import com.manpou.allinone.qc.application.dto.QcRecordPageQuery;
import com.manpou.allinone.qc.application.dto.QcRecordQuery;
import com.manpou.allinone.qc.application.dto.QcRecordUpdateCmd;
import com.manpou.allinone.qc.application.usecase.QcRecordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 验货记录 REST 接口。
 */
@RestController
@RequestMapping("/api/v1/qc-records")
@RequiredArgsConstructor
public class QcRecordController {

    private final QcRecordUseCase qcRecordUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('qc:read')")
    public Result<Page<QcRecordPageQuery>> list(QcRecordQuery query) {
        return Result.ok(qcRecordUseCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('qc:read')")
    public Result<QcRecordPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(qcRecordUseCase.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('qc:create')")
    public Result<Long> create(@Valid @RequestBody QcRecordCreateCmd cmd) {
        return Result.ok(qcRecordUseCase.create(cmd));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('qc:update')")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody QcRecordUpdateCmd cmd) {
        qcRecordUseCase.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('qc:delete')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        qcRecordUseCase.delete(id);
        return Result.ok();
    }
}
