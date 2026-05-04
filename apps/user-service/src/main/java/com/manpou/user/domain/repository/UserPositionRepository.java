package com.manpou.user.domain.repository;

import com.manpou.user.domain.model.UserPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户职务关联仓库（操作 join 表 user_position）。
 */
@Repository
public interface UserPositionRepository extends JpaRepository<UserPosition, Long> {

    @Query(value = """
        SELECT up.user_id, up.position_id, p.position_name_cn, p.position_name_jp
        FROM user_position up
        INNER JOIN position p ON up.position_id = p.id
        WHERE up.user_id IN (:userIds) AND p.is_deleted = 0
        """, nativeQuery = true)
    List<Object[]> findPositionsByUserIds(@Param("userIds") List<Long> userIds);
}