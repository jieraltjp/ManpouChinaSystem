package com.manpou.user.domain.repository;

import com.manpou.user.domain.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 操作日志仓库。
 *
 * <p>查询逻辑全在 Service 层用 JPA Specification 动态拼装，
 * 此处无需自定义方法。
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}
