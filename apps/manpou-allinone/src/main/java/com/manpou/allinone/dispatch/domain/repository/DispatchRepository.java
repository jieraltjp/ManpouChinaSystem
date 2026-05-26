package com.manpou.allinone.dispatch.domain.repository;

import com.manpou.allinone.dispatch.domain.model.Dispatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface DispatchRepository {

    Optional<Dispatch> findById(Long id);

    Optional<Dispatch> findByIdAndDeletedIsFalse(Long id);

    Optional<Dispatch> findTopByCodeAndDeletedIsFalseOrderByIdDesc(String code);

    List<Dispatch> findAll(Specification<Dispatch> spec, Sort sort);

    Page<Dispatch> findAllByDeletedIsFalse(Pageable pageable);

    Page<Dispatch> findAll(Specification<Dispatch> spec, Pageable pageable);

    Page<Dispatch> findByCodeContainingAndDeletedIsFalse(String code, Pageable pageable);

    Page<Dispatch> findByDestinationContainingAndDeletedIsFalse(String destination, Pageable pageable);

    Page<Dispatch> findByManagerContainingAndDeletedIsFalse(String manager, Pageable pageable);

    Page<Dispatch> findByStatusAndDeletedIsFalse(String status, Pageable pageable);

    Page<Dispatch> findByStatusEmptyAndDeletedIsFalse(Pageable pageable);

    Dispatch save(Dispatch entity);

    void deleteById(Long id);
}