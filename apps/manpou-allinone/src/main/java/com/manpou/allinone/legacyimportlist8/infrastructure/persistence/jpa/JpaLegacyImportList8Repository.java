package com.manpou.allinone.legacyimportlist8.infrastructure.persistence.jpa;

import com.manpou.allinone.legacyimportlist8.domain.model.LegacyImportList8;
import com.manpou.allinone.legacyimportlist8.domain.repository.LegacyImportList8Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaLegacyImportList8Repository
        extends LegacyImportList8Repository, JpaRepository<LegacyImportList8, Integer>, JpaSpecificationExecutor<LegacyImportList8> {

    @Override
    Page<LegacyImportList8> findAll(Pageable pageable);

    @Override
    Page<LegacyImportList8> findByCodeContaining(String code, Pageable pageable);

    @Override
    Page<LegacyImportList8> findByLocationContaining(String location, Pageable pageable);

    @Override
    Page<LegacyImportList8> findBySoukoContaining(String souko, Pageable pageable);

    @Override
    Page<LegacyImportList8> findByDestinationContaining(String destination, Pageable pageable);

    @Override
    Optional<LegacyImportList8> findById(Integer id);

    @Override
    List<LegacyImportList8> findAll();

    @Override
    @Query(value = "SELECT * FROM legacy_import_list8 WHERE TRIM(UPPER(code)) = TRIM(UPPER(:code)) ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<LegacyImportList8> findByCodeTrimUpper(String code);

    @Modifying
    @Query("DELETE FROM LegacyImportList8 e WHERE e.id = :id")
    void deleteByIdCustom(@Param("id") Integer id);
}
