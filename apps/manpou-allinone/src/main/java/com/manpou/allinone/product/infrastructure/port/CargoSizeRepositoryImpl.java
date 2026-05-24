package com.manpou.allinone.product.infrastructure.port;

import com.manpou.allinone.product.domain.model.CargoSize;
import com.manpou.allinone.product.domain.model.CargoSizeStatus;
import com.manpou.allinone.product.domain.repository.CargoSizeRepository;
import com.manpou.allinone.product.infrastructure.persistence.jpa.CargoSizeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CargoSizeRepositoryImpl implements CargoSizeRepository {

    private final CargoSizeJpaRepository jpaRepository;

    @Override
    public CargoSize save(CargoSize cargoSize) {
        return jpaRepository.save(cargoSize);
    }

    @Override
    public Optional<CargoSize> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<CargoSize> findByCode(String code) {
        return jpaRepository.findByCode(code);
    }

    @Override
    public Page<CargoSize> findByStatus(CargoSizeStatus status, Pageable pageable) {
        return jpaRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<CargoSize> findByStatusAndCodeContainingIgnoreCase(
            CargoSizeStatus status, String keyword, Pageable pageable) {
        return jpaRepository.findByStatusAndCodeContainingIgnoreCase(status, keyword, pageable);
    }

    @Override
    public Page<CargoSize> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public Page<CargoSize> findByCodeContainingIgnoreCase(String keyword, Pageable pageable) {
        return jpaRepository.findByCodeContainingIgnoreCase(keyword, pageable);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }
}
