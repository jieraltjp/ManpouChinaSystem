package com.manpou.allinone.procurement.domain.repository;

import com.manpou.allinone.procurement.domain.model.ShipmentBatch;
import com.manpou.allinone.procurement.domain.model.ShipmentBatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 出货批次仓储接口（领域层契约）。
 */
public interface ShipmentBatchRepository {

    Optional<ShipmentBatch> findById(Long id);

    Optional<ShipmentBatch> findByIdAndDeletedIsFalse(Long id);

    /** 按采购单查询所有批次。 */
    List<ShipmentBatch> findByProcurementIdAndDeletedIsFalse(Long procurementId);

    /** 按采购单分页查询。 */
    Page<ShipmentBatch> findByProcurementIdAndDeletedIsFalse(Long procurementId, Pageable pageable);

    /** 按采购单统计批次数量。 */
    long countByProcurementIdAndDeletedIsFalse(Long procurementId);

    /** 按状态查询。 */
    Page<ShipmentBatch> findByStatusAndDeletedIsFalse(ShipmentBatchStatus status, Pageable pageable);

    /** 查询所有批次。 */
    Page<ShipmentBatch> findAllByDeletedIsFalse(Pageable pageable);

    ShipmentBatch save(ShipmentBatch entity);

    void deleteById(Long id);

    void flush();

    /** 按采购单 ID 列表查询批次。 */
    List<ShipmentBatch> findByProcurementIdInAndDeletedIsFalse(List<Long> procurementIds);
}
