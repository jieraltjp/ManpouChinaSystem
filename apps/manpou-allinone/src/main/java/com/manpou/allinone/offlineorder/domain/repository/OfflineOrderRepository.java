package com.manpou.allinone.offlineorder.domain.repository;

import com.manpou.allinone.offlineorder.domain.model.OfflineOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OfflineOrderRepository {

    Optional<OfflineOrder> findById(Long id);

    OfflineOrder save(OfflineOrder entity);

    void deleteById(Long id);

    Page<OfflineOrder> findAll(Pageable pageable);

    Page<OfflineOrder> findByCodeContaining(String code, Pageable pageable);

    Page<OfflineOrder> findByItemNameContaining(String itemName, Pageable pageable);

    Page<OfflineOrder> findByFactoryContaining(String factory, Pageable pageable);

    Page<OfflineOrder> findByArrival(String arrival, Pageable pageable);
}