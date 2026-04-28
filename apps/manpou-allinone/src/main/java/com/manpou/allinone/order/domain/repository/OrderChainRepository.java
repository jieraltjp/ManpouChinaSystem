package com.manpou.allinone.order.domain.repository;

import com.manpou.allinone.order.domain.model.OrderChainView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 订单总览视图仓库（Phase1 步骤1~4）。
 * 映射 v_order_chain_v1 视图（只读）。
 */
@Repository
public interface OrderChainRepository extends JpaRepository<OrderChainView, Long> {

    /**
     * 分页查询订单链列表（支持状态筛选 + 关键词搜索）。
     */
    @Query("""
        SELECT v FROM OrderChainView v
        WHERE (:demandStatus IS NULL OR v.demandStatus = :demandStatus)
          AND (:keyword IS NULL
               OR LOWER(v.demandCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.demandSubProductCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.demandDestination) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY v.demandCreateTime DESC
        """)
    Page<OrderChainView> findChainList(
            @Param("demandStatus") String demandStatus,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 详情查询（按 demandId）。
     */
    Optional<OrderChainView> findByDemandId(Long demandId);
}
