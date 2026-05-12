package com.manpou.allinone.logistics.domain.repository;

import com.manpou.allinone.logistics.domain.model.Ship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 船只仓库接口（领域层契约，不感知 JPA）。
 */
public interface ShipRepository {

    Optional<Ship> findById(Long id);

    Optional<Ship> findByIdAndDeletedIsFalse(Long id);

    Optional<Ship> findByShipNumberAndDeletedIsFalse(String shipNumber);

    Page<Ship> findAllByDeletedIsFalse(Pageable pageable);

    Page<Ship> findByArrivalPortContainingAndDeletedIsFalse(String arrivalPort, Pageable pageable);

    Page<Ship> findByShipNameContainingAndDeletedIsFalse(String shipName, Pageable pageable);

    Page<Ship> findByShipNumberContainingAndDeletedIsFalse(String shipNumber, Pageable pageable);

    Ship save(Ship entity);

    void deleteById(Long id);
}
