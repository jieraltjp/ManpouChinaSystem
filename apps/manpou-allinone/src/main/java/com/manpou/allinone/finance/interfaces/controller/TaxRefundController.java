package com.manpou.allinone.finance.interfaces.controller;

import com.manpou.allinone.finance.application.dto.*;
import com.manpou.allinone.finance.application.usecase.TaxRefundUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-refunds")
@RequiredArgsConstructor
public class TaxRefundController {

    private final TaxRefundUseCase taxRefundUseCase;

    @GetMapping
    public ResponseEntity<Page<TaxRefundPageQuery>> list(TaxRefundQuery query) {
        return ResponseEntity.ok(taxRefundUseCase.pageQuery(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxRefundPageQuery> get(@PathVariable Long id) {
        return ResponseEntity.ok(taxRefundUseCase.getById(id));
    }

    @PostMapping
    public ResponseEntity<Long> create(@Valid @RequestBody TaxRefundCreateCmd cmd) {
        return ResponseEntity.ok(taxRefundUseCase.create(cmd));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> complete(@PathVariable Long id, @Valid @RequestBody TaxRefundCompleteCmd cmd) {
        taxRefundUseCase.complete(id, cmd);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/no-refund")
    public ResponseEntity<Void> markNoRefund(@PathVariable Long id) {
        taxRefundUseCase.markNoRefund(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taxRefundUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
