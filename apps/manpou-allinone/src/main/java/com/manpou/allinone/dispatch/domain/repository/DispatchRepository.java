package com.manpou.allinone.dispatch.domain.repository;

import com.manpou.allinone.dispatch.domain.model.Dispatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DispatchRepository {

    Optional<Dispatch> findById(Long id);

    Optional<Dispatch> findByIdAndDeletedIsFalse(Long id);

    Page<Dispatch> findAllByDeletedIsFalse(Pageable pageable);

    Page<Dispatch> findByCodeContainingAndDeletedIsFalse(String code, Pageable pageable);

    Page<Dispatch> findByDestinationContainingAndDeletedIsFalse(String destination, Pageable pageable);

    Page<Dispatch> findByManagerContainingAndDeletedIsFalse(String manager, Pageable pageable);

    Dispatch save(Dispatch entity);

    void deleteById(Long id);
}