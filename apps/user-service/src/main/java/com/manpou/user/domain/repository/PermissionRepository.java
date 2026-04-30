package com.manpou.user.domain.repository;

import com.manpou.user.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 权限仓库。
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Query(value = """
        SELECT p.* FROM permission p
        INNER JOIN role_permission rp ON p.id = rp.permission_id
        INNER JOIN user_role ur ON rp.role_id = ur.role_id
        WHERE ur.user_id = :userId AND p.is_deleted = 0
        """, nativeQuery = true)
    List<Permission> findPermissionsByUserId(@Param("userId") Long userId);

    List<Permission> findByModule(String module);
}
