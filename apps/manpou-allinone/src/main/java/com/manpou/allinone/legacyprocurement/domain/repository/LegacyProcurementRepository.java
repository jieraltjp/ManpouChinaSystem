package com.manpou.allinone.legacyprocurement.domain.repository;

import com.manpou.allinone.legacyprocurement.domain.model.LegacyProcurement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LegacyProcurementRepository {

    Page<LegacyProcurement> findAllExcludeDeleted(Pageable pageable);

    Page<LegacyProcurement> findByCodeContainingExcludeDeleted(String code, Pageable pageable);

    Page<LegacyProcurement> findByOrderGroupContainingExcludeDeleted(String orderGroup, Pageable pageable);

    Page<LegacyProcurement> findByItemNameContainingExcludeDeleted(String itemName, Pageable pageable);

    Page<LegacyProcurement> findByUpdaterContainingExcludeDeleted(String updater, Pageable pageable);

    LegacyProcurement save(LegacyProcurement entity);

    void softDelete(Integer id);

    long count();

    /** 按 ID 查找，排除软删除 */
    Optional<LegacyProcurement> findByIdExcludeDeleted(Integer id);
}