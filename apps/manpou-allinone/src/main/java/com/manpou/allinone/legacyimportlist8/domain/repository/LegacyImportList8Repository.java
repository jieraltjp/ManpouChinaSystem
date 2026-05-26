package com.manpou.allinone.legacyimportlist8.domain.repository;

import com.manpou.allinone.legacyimportlist8.domain.model.LegacyImportList8;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface LegacyImportList8Repository {

    Page<LegacyImportList8> findAll(Pageable pageable);

    Page<LegacyImportList8> findAll(Specification<LegacyImportList8> spec, Pageable pageable);

    Page<LegacyImportList8> findByCodeContaining(String code, Pageable pageable);

    Page<LegacyImportList8> findByLocationContaining(String location, Pageable pageable);

    Page<LegacyImportList8> findBySoukoContaining(String souko, Pageable pageable);

    Page<LegacyImportList8> findByDestinationContaining(String destination, Pageable pageable);

    Optional<LegacyImportList8> findById(Integer id);

    LegacyImportList8 save(LegacyImportList8 entity);

    void deleteById(Integer id);

    long count();

    List<LegacyImportList8> findAll();

    /** 根据货号精准查询（TRIM+UPPER） */
    Optional<LegacyImportList8> findByCodeTrimUpper(String code);
}
