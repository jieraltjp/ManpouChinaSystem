package com.manpou.allinone.product.domain.repository;

import com.manpou.allinone.product.domain.model.CargoSize;
import com.manpou.allinone.product.domain.model.CargoSizeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CargoSizeRepository {

    CargoSize save(CargoSize cargoSize);

    Optional<CargoSize> findById(Long id);

    Optional<CargoSize> findByCode(String code);

    Page<CargoSize> findByStatus(CargoSizeStatus status, Pageable pageable);

    Page<CargoSize> findByStatusAndCodeContainingIgnoreCase(
            CargoSizeStatus status, String keyword, Pageable pageable);

    Page<CargoSize> findAll(Pageable pageable);

    Page<CargoSize> findByCodeContainingIgnoreCase(String keyword, Pageable pageable);

    boolean existsByCode(String code);
}
