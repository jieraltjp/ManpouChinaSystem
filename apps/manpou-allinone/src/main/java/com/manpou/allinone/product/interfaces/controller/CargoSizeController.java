package com.manpou.allinone.product.interfaces.controller;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.product.application.dto.CargoSizeCreateCmd;
import com.manpou.allinone.product.application.dto.CargoSizePromoteCmd;
import com.manpou.allinone.product.application.dto.CargoSizeUpdateCmd;
import com.manpou.allinone.product.application.dto.CargoSizeVO;
import com.manpou.allinone.product.application.usecase.CargoSizeUseCase;
import com.manpou.allinone.product.domain.model.CargoSizeStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cargo-sizes")
@RequiredArgsConstructor
public class CargoSizeController {

    private final CargoSizeUseCase cargoSizeUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('cargo_size:read')")
    public Result<Page<CargoSizeVO>> query(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CargoSizeStatus s = null;
        if (status != null && !status.isBlank()) {
            try {
                s = CargoSizeStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        Page<CargoSizeVO> result = cargoSizeUseCase.query(keyword, s, page, size);
        return Result.ok(result);
    }

    @PostMapping
    @AuditLog(module = "cargo_size", action = "CREATE", resourceType = "cargo_size", resourceId = "0")
    @PreAuthorize("hasAuthority('cargo_size:create')")
    public Result<CargoSizeVO> create(@Valid @RequestBody CargoSizeCreateCmd cmd) {
        return Result.ok(cargoSizeUseCase.create(cmd));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('cargo_size:read')")
    public Result<CargoSizeVO> getById(@PathVariable Long id) {
        return Result.ok(cargoSizeUseCase.getById(id));
    }

    @PostMapping("/{id}/promote")
    @AuditLog(module = "cargo_size", action = "UPDATE", resourceType = "cargo_size", resourceId = "#id")
    @PreAuthorize("hasAuthority('cargo_size:promote') or hasAuthority('product:create')")
    public Result<CargoSizeVO> promote(@PathVariable Long id,
                                       @Valid @RequestBody CargoSizePromoteCmd cmd) {
        return Result.ok(cargoSizeUseCase.promote(id, cmd));
    }

    @PostMapping("/{id}/discard")
    @AuditLog(module = "cargo_size", action = "UPDATE", resourceType = "cargo_size", resourceId = "#id")
    @PreAuthorize("hasAuthority('cargo_size:discard')")
    public Result<CargoSizeVO> discard(@PathVariable Long id) {
        return Result.ok(cargoSizeUseCase.discard(id));
    }

    @PutMapping("/{id}")
    @AuditLog(module = "cargo_size", action = "UPDATE", resourceType = "cargo_size", resourceId = "#id")
    @PreAuthorize("hasAuthority('cargo_size:update')")
    public Result<CargoSizeVO> update(@PathVariable Long id,
                                      @Valid @RequestBody CargoSizeUpdateCmd cmd) {
        return Result.ok(cargoSizeUseCase.update(id, cmd));
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "cargo_size", action = "DELETE", resourceType = "cargo_size", resourceId = "#id")
    @PreAuthorize("hasAuthority('cargo_size:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        cargoSizeUseCase.softDelete(id);
        return Result.ok();
    }
}
