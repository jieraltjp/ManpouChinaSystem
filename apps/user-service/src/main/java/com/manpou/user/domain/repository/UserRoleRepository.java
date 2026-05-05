package com.manpou.user.domain.repository;

import com.manpou.user.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关联仓库（操作 join 表 user_role）。
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * 查询用户拥有的角色 ID 列表。
     */
    @Query(value = "SELECT ur.role_id FROM user_role ur WHERE ur.user_id = :userId",
           nativeQuery = true)
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 批量查询多个用户的角色 ID 列表（避免 N+1）。
     * 返回 [userId, roleId] 元组列表。
     */
    @Query(value = "SELECT ur.user_id, ur.role_id FROM user_role ur WHERE ur.user_id IN :userIds",
           nativeQuery = true)
    List<Object[]> findRoleIdsByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 查询拥有指定角色的用户 ID 列表。
     */
    @Query(value = "SELECT ur.user_id FROM user_role ur WHERE ur.role_id = :roleId",
           nativeQuery = true)
    List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除用户所有角色关联。
     */
    @Modifying
    @Query(value = "DELETE FROM user_role WHERE user_id = :userId",
           nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 插入用户角色关联。
     */
    @Modifying
    @Query(value = "INSERT INTO user_role (user_id, role_id, create_time) VALUES (:userId, :roleId, NOW(3))",
           nativeQuery = true)
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 批量查询各角色的成员数量。
     * 返回 [roleId, count] 元组列表。
     */
    @Query(value = "SELECT ur.role_id, COUNT(ur.user_id) FROM user_role ur GROUP BY ur.role_id",
           nativeQuery = true)
    List<Object[]> countUsersByRoleIds();
}