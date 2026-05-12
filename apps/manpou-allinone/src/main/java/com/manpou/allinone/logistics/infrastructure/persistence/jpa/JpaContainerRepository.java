package com.manpou.allinone.logistics.infrastructure.persistence.jpa;

import com.manpou.allinone.logistics.domain.model.Container;
import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import com.manpou.allinone.logistics.domain.model.Ship;
import com.manpou.allinone.logistics.domain.repository.ContainerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 货柜 JPA 持久化适配器。
 */
@Repository
public interface JpaContainerRepository extends ContainerRepository, JpaRepository<Container, Long> {

    Optional<Container> findByIdAndDeletedIsFalse(Long id);

    Optional<Container> findByContainerNoAndDeletedIsFalse(String containerNo);

    Page<Container> findByStatusAndDeletedIsFalse(ContainerStatus status, Pageable pageable);

    Page<Container> findByPoolIdAndDeletedIsFalse(Long poolId, Pageable pageable);

    Page<Container> findByShipIdAndDeletedIsFalse(Long shipId, Pageable pageable);

    long countByShipIdAndDeletedIsFalse(Long shipId);

    /**
     * 分页查询，包含 ship 关联信息。
     * 返回 Object[]: {c, shipName, shipNumber}，由 Assembler 转换。
     */
    @Query("""
        SELECT c, COALESCE(s.shipName,''), COALESCE(s.shipNumber,'')
        FROM Container c LEFT JOIN Ship s ON c.shipId = s.id AND s.deleted = false
        WHERE c.deleted = false
        ORDER BY c.createTime DESC
        """)
    Page<Object[]> findAllWithShip(Pageable pageable);
}
