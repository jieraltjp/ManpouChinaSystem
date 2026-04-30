package com.manpou.user.domain.repository;

import com.manpou.user.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色仓库。
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleCode(String roleCode);

    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE r.id = :userId")
    List<Role> findByUserId(@Param("userId") Long userId);

    @Query(value = """
        SELECT r.* FROM role r
        INNER JOIN user_role ur ON r.id = ur.role_id
        WHERE ur.user_id = :userId AND r.is_deleted = 0
        """, nativeQuery = true)
    List<Role> findRolesByUserId(@Param("userId") Long userId);
}
