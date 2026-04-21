package com.manpou.allinone.qc.interfaces.controller;

import com.manpou.common.result.Result;
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
    public Result<Page<QcRecordPageQuery>> list(QcRecordQuery query) {
        return Result.ok(qcRecordUseCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    public Result<QcRecordPageQuery> get(@PathVariable("id") Long id) {
        return Result.ok(qcRecordUseCase.getById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody QcRecordCreateCmd cmd) {
        return Result.ok(qcRecordUseCase.create(cmd));
    }

    @PatchMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody QcRecordUpdateCmd cmd) {
        qcRecordUseCase.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        qcRecordUseCase.delete(id);
        return Result.ok();
    }
}
