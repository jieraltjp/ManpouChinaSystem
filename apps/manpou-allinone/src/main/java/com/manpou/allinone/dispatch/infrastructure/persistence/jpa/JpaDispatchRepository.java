package com.manpou.allinone.dispatch.infrastructure.persistence.jpa;

import com.manpou.allinone.dispatch.domain.model.Dispatch;
import com.manpou.allinone.dispatch.domain.repository.DispatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaDispatchRepository extends DispatchRepository, JpaRepository<Dispatch, Long> {

    Optional<Dispatch> findByIdAndDeletedIsFalse(Long id);

    Page<Dispatch> findAllByDeletedIsFalse(Pageable pageable);

    Page<Dispatch> findByCodeContainingAndDeletedIsFalse(String code, Pageable pageable);

    Page<Dispatch> findByDestinationContainingAndDeletedIsFalse(String destination, Pageable pageable);

    Page<Dispatch> findByManagerContainingAndDeletedIsFalse(String manager, Pageable pageable);

    long countByDeletedIsFalse();
}