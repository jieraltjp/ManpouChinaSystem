package com.manpou.allinone.logistics.infrastructure.persistence.jpa;

import com.manpou.allinone.logistics.domain.model.Ship;
import com.manpou.allinone.logistics.domain.repository.ShipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA 持久化适配器（基础设施层）。
 */
@Repository
public interface ShipJpaRepository extends ShipRepository, JpaRepository<Ship, Long> {

    Optional<Ship> findByShipNumberAndDeletedIsFalse(String shipNumber);

    Page<Ship> findByArrivalPortContainingAndDeletedIsFalse(String arrivalPort, Pageable pageable);

    Page<Ship> findByShipNameContainingAndDeletedIsFalse(String shipName, Pageable pageable);

    Page<Ship> findByShipNumberContainingAndDeletedIsFalse(String shipNumber, Pageable pageable);

    long countByDeletedIsFalse();
}
