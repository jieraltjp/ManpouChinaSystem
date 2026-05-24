package com.manpou.allinone.legacyprocurement.infrastructure.persistence.jpa;

import com.manpou.allinone.legacyprocurement.domain.model.LegacyProcurement;
import com.manpou.allinone.legacyprocurement.domain.repository.LegacyProcurementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaLegacyProcurementRepository
        extends LegacyProcurementRepository, org.springframework.data.jpa.repository.JpaRepository<LegacyProcurement, Integer> {

    Page<LegacyProcurement> findByCodeContaining(String code, Pageable pageable);

    Page<LegacyProcurement> findByCodeContainingAndDeletedFalse(String code, Pageable pageable);

    Page<LegacyProcurement> findByOrderGroupContaining(String orderGroup, Pageable pageable);

    Page<LegacyProcurement> findByOrderGroupContainingAndDeletedFalse(String orderGroup, Pageable pageable);

    Page<LegacyProcurement> findByItemNameContaining(String itemName, Pageable pageable);

    Page<LegacyProcurement> findByItemNameContainingAndDeletedFalse(String itemName, Pageable pageable);

    Page<LegacyProcurement> findByUpdaterContaining(String updater, Pageable pageable);

    Page<LegacyProcurement> findByUpdaterContainingAndDeletedFalse(String updater, Pageable pageable);

    long countByContainerIsNotNull();

    long countByImgIsNotNullAndImgNot(String img);

    long countByContainerIsNotNullAndDeletedFalse();

    long countByImgIsNotNullAndImgNotAndDeletedFalse(String img);

    @Override
    default Page<LegacyProcurement> findAllExcludeDeleted(Pageable pageable) {
        return findByDeletedFalse(pageable);
    }

    @Override
    default Page<LegacyProcurement> findByCodeContainingExcludeDeleted(String code, Pageable pageable) {
        return findByCodeContainingAndDeletedFalse(code, pageable);
    }

    @Override
    default Page<LegacyProcurement> findByOrderGroupContainingExcludeDeleted(String orderGroup, Pageable pageable) {
        return findByOrderGroupContainingAndDeletedFalse(orderGroup, pageable);
    }

    @Override
    default Page<LegacyProcurement> findByItemNameContainingExcludeDeleted(String itemName, Pageable pageable) {
        return findByItemNameContainingAndDeletedFalse(itemName, pageable);
    }

    @Override
    default Page<LegacyProcurement> findByUpdaterContainingExcludeDeleted(String updater, Pageable pageable) {
        return findByUpdaterContainingAndDeletedFalse(updater, pageable);
    }

    @Override
    default Optional<LegacyProcurement> findByIdExcludeDeleted(Integer id) {
        return findByLegacyIdAndDeletedFalse(id);
    }

    Page<LegacyProcurement> findByDeletedFalse(Pageable pageable);

    Optional<LegacyProcurement> findByLegacyIdAndDeletedFalse(Integer id);

    @Modifying
    @Query("UPDATE LegacyProcurement lp SET lp.deleted = true WHERE lp.legacyId = :id")
    void softDelete(@Param("id") Integer id);
}