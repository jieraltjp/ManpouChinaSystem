package com.manpou.allinone.replenishment.domain.repository;

import com.manpou.allinone.replenishment.domain.model.DemandProcurementMapping;
import com.manpou.allinone.replenishment.domain.model.MappingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 需求-采购分配映射仓储接口（领域层契约）。
 */
public interface DemandProcurementMappingRepository {

    Optional<DemandProcurementMapping> findById(Long id);

    Optional<DemandProcurementMapping> findByIdAndDeletedIsFalse(Long id);

    /** 按需求单查询所有映射。 */
    List<DemandProcurementMapping> findByDemandIdAndDeletedIsFalse(Long demandId);

    /** 按采购单查询所有映射。 */
    List<DemandProcurementMapping> findByProcurementIdAndDeletedIsFalse(Long procurementId);

    /** 按需求单分页查询。 */
    Page<DemandProcurementMapping> findByDemandIdAndDeletedIsFalse(Long demandId, Pageable pageable);

    /** 按状态查询。 */
    Page<DemandProcurementMapping> findByStatusAndDeletedIsFalse(MappingStatus status, Pageable pageable);

    /** 查询所有映射。 */
    Page<DemandProcurementMapping> findAllByDeletedIsFalse(Pageable pageable);

    DemandProcurementMapping save(DemandProcurementMapping entity);

    void deleteById(Long id);

    void flush();

    /** 判断指定映射是否已存在。 */
    boolean existsByDemandIdAndProcurementIdAndDeletedIsFalse(Long demandId, Long procurementId);
}
