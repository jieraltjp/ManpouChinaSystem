package com.manpou.allinone.finance.interfaces.controller;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.finance.application.dto.*;
import com.manpou.allinone.finance.application.usecase.TaxRefundUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-refunds")
@RequiredArgsConstructor
public class TaxRefundController {

    private final TaxRefundUseCase taxRefundUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('tax_refund:read')")
    public ResponseEntity<Page<TaxRefundPageQuery>> list(TaxRefundQuery query) {
        return ResponseEntity.ok(taxRefundUseCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('tax_refund:read')")
    public ResponseEntity<TaxRefundPageQuery> get(@PathVariable Long id) {
        return ResponseEntity.ok(taxRefundUseCase.getById(id));
    }

    @PostMapping
      @AuditLog(module = "tax_refund", action = "CREATE", resourceType = "tax_refund", resourceId = "#_return")
    @PreAuthorize("hasAuthority('tax_refund:create')")
    public ResponseEntity<Long> create(@Valid @RequestBody TaxRefundCreateCmd cmd) {
        return ResponseEntity.ok(taxRefundUseCase.create(cmd));
    }

    @PatchMapping("/{id}/complete")
    @AuditLog(module = "tax_refund", action = "UPDATE", resourceType = "tax_refund", resourceId = "#id")
    @PreAuthorize("hasAuthority('tax_refund:update')")
    public ResponseEntity<Void> complete(@PathVariable Long id, @Valid @RequestBody TaxRefundCompleteCmd cmd) {
        taxRefundUseCase.complete(id, cmd);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/no-refund")
    @AuditLog(module = "tax_refund", action = "UPDATE", resourceType = "tax_refund", resourceId = "#id")
    @PreAuthorize("hasAuthority('tax_refund:update')")
    public ResponseEntity<Void> markNoRefund(@PathVariable Long id) {
        taxRefundUseCase.markNoRefund(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "tax_refund", action = "DELETE", resourceType = "tax_refund", resourceId = "#id")
    @PreAuthorize("hasAuthority('tax_refund:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taxRefundUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
