package com.manpou.allinone.sales.interfaces.controller;

import com.manpou.allinone.sales.application.dto.*;
import com.manpou.allinone.sales.application.usecase.SalesRecordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales-records")
@RequiredArgsConstructor
public class SalesRecordController {

    private final SalesRecordUseCase salesRecordUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('sales:read')")
    public ResponseEntity<Page<SalesRecordPageQuery>> list(SalesRecordQuery query) {
        return ResponseEntity.ok(salesRecordUseCase.pageQuery(query));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('sales:read')")
    public ResponseEntity<Page<SalesRecordPageQuery>> alerts(SalesRecordQuery query) {
        return ResponseEntity.ok(salesRecordUseCase.getAlerts(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:read')")
    public ResponseEntity<SalesRecordPageQuery> get(@PathVariable Long id) {
        return ResponseEntity.ok(salesRecordUseCase.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales:create')")
    public ResponseEntity<Long> create(@Valid @RequestBody SalesRecordCreateCmd cmd) {
        return ResponseEntity.ok(salesRecordUseCase.create(cmd));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:update')")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody SalesRecordUpdateCmd cmd) {
        salesRecordUseCase.update(id, cmd);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAuthority('sales:update')")
    public ResponseEntity<Void> updateStock(@PathVariable Long id, @Valid @RequestBody SalesRecordStockCmd cmd) {
        salesRecordUseCase.updateStock(id, cmd);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/discontinue")
    @PreAuthorize("hasAuthority('sales:update')")
    public ResponseEntity<Void> discontinue(@PathVariable Long id) {
        salesRecordUseCase.discontinue(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/relist")
    @PreAuthorize("hasAuthority('sales:update')")
    public ResponseEntity<Void> relist(@PathVariable Long id) {
        salesRecordUseCase.relist(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesRecordUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
