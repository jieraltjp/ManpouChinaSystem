package com.manpou.allinone.order.domain.repository;

import com.manpou.allinone.order.domain.view.OrderChainView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 订单总览仓库（9步完整链路）。
 * 基于 MySQL VIEW v_order_chain_v2（与 SPEC-B00 业务流程对齐）。
 * Step1: Demand → Step2: Procurement → Step3: ShipmentBatch
 * Step4: QcRecord → Step5: LogisticsPlan → Step6: DomesticCustoms
 * Step7: JapanCustoms → Step8: TaxRefund → Step9: SalesRecord
 */
@Repository
public interface OrderChainRepository extends JpaRepository<OrderChainView, Long> {

    @Query(value = """
        SELECT * FROM v_order_chain_v2 v
        WHERE (:demandStatus IS NULL OR v.demand_status = :demandStatus)
          AND (:keyword IS NULL
               OR LOWER(v.demand_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.demand_sub_product_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.demand_destination) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
        countQuery = """
        SELECT COUNT(*) FROM v_order_chain_v2 v
        WHERE (:demandStatus IS NULL OR v.demand_status = :demandStatus)
          AND (:keyword IS NULL
               OR LOWER(v.demand_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.demand_sub_product_code) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.demand_destination) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
        nativeQuery = true)
    Page<OrderChainView> findChainList(
            @Param("demandStatus") String demandStatus,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(value = "SELECT * FROM v_order_chain_v2 WHERE demand_id = :demandId ORDER BY demand_id", nativeQuery = true)
    Optional<OrderChainView> findByDemandId(@Param("demandId") Long demandId);
}
