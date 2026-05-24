package com.manpou.allinone.product.infrastructure.persistence.jpa;

import com.manpou.allinone.product.domain.model.CargoSize;
import com.manpou.allinone.product.domain.model.CargoSizeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CargoSizeJpaRepository extends JpaRepository<CargoSize, Long>, JpaSpecificationExecutor<CargoSize> {

    Optional<CargoSize> findByCode(String code);

    boolean existsByCode(String code);

    Page<CargoSize> findByStatus(CargoSizeStatus status, Pageable pageable);

    Page<CargoSize> findByStatusAndCodeContainingIgnoreCase(
            CargoSizeStatus status, String keyword, Pageable pageable);

    Page<CargoSize> findByCodeContainingIgnoreCase(String keyword, Pageable pageable);
}
