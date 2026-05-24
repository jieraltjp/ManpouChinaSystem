package com.manpou.allinone.offlineorder.infrastructure.persistence.jpa;

import com.manpou.allinone.offlineorder.domain.model.OfflineOrder;
import com.manpou.allinone.offlineorder.domain.repository.OfflineOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaOfflineOrderRepository extends OfflineOrderRepository, JpaRepository<OfflineOrder, Long> {

    Page<OfflineOrder> findByCodeContaining(String code, Pageable pageable);

    Page<OfflineOrder> findByItemNameContaining(String itemName, Pageable pageable);

    Page<OfflineOrder> findByFactoryContaining(String factory, Pageable pageable);

    Page<OfflineOrder> findByArrival(String arrival, Pageable pageable);
}