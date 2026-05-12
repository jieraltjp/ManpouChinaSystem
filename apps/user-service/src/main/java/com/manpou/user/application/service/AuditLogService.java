package com.manpou.user.application.service;

import com.manpou.user.application.dto.AuditLogPageQuery;
import com.manpou.user.application.dto.AuditLogPageVO;
import com.manpou.user.application.dto.AuditLogVO;
import com.manpou.user.common.exception.BusinessException;
import com.manpou.user.domain.model.AuditLog;
import com.manpou.user.domain.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志服务。
 *
 * <p>负责 audit_log 表的写入和查询。
 * 写入分两种路径：
 * <ul>
 *   <li>同步写入（save）：AuthController 认证日志，同 JVM 调用</li>
 *   <li>异步写入（saveAsync）：allinone 业务日志，HTTP POST 调用</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 同步保存（AuthController 认证日志用）。
     */
    @Transactional
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getCreateTime() == null) {
            auditLog.setCreateTime(LocalDateTime.now());
        }
        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("[AuditLog] saved id={}, module={}, action={}", saved.getId(), saved.getModule(), saved.getAction());
        return saved;
    }

    /**
     * 异步保存（allinone 业务日志用，失败吞异常）。
     * REQUIRES_NEW 确保独立事务，避免 UnexpectedRollbackException。
     */
    @Async
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void saveAsync(AuditLog auditLog) {
        try {
            if (auditLog.getCreateTime() == null) {
                auditLog.setCreateTime(LocalDateTime.now());
            }
            AuditLog saved = auditLogRepository.save(auditLog);
            log.info("[AuditLog] async saved: id={}, username={}, module={}, action={}",
                saved.getId(), saved.getUsername(), saved.getModule(), saved.getAction());
        } catch (Exception ex) {
            log.warn("[AuditLog] async save failed: username={}, module={}, action={}, error={}",
                auditLog.getUsername(), auditLog.getModule(), auditLog.getAction(), ex.getMessage());
        }
    }

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public AuditLogPageVO pageQuery(AuditLogPageQuery query) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        PageRequest pageRequest = PageRequest.of(
            query.getPage() != null ? query.getPage() : 0,
            query.getSize() != null ? query.getSize() : 20,
            sort
        );

        Page<AuditLog> page = auditLogRepository.findAll(buildSpec(query), pageRequest);

        AuditLogPageVO result = new AuditLogPageVO();
        result.setContent(page.getContent().stream().map(this::toVO).toList());
        result.setTotalElements(page.getTotalElements());
        result.setTotalPages(page.getTotalPages());
        result.setNumber(page.getNumber());
        result.setSize(page.getSize());
        return result;
    }

    /**
     * 按 ID 查询详情。
     */
    @Transactional(readOnly = true)
    public AuditLogVO getById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
            .orElseThrow(() -> new BusinessException("auditLog.notFound", "操作日志不存在: " + id));
        return toVO(auditLog);
    }

    // ===== 内部方法 =====

    private Specification<AuditLog> buildSpec(AuditLogPageQuery query) {
        return (root, cq, cb) -> {
            var predicates = new java.util.ArrayList<Predicate>();

            if (query.getUserId() != null && !query.getUserId().isBlank()) {
                predicates.add(cb.equal(root.get("userId"), query.getUserId()));
            }
            if (query.getModule() != null && !query.getModule().isBlank()) {
                predicates.add(cb.equal(root.get("module"), query.getModule()));
            }
            if (query.getAction() != null && !query.getAction().isBlank()) {
                predicates.add(cb.equal(root.get("action"), query.getAction()));
            }
            if (query.getResourceType() != null && !query.getResourceType().isBlank()) {
                predicates.add(cb.equal(root.get("resourceType"), query.getResourceType()));
            }
            if (query.getResourceId() != null && !query.getResourceId().isBlank()) {
                predicates.add(cb.equal(root.get("resourceId"), query.getResourceId()));
            }
            if (query.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), query.getStartTime()));
            }
            if (query.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), query.getEndTime()));
            }

            cq.orderBy(cb.desc(root.get("createTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AuditLogVO toVO(AuditLog entity) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(entity.getId());
        vo.setTraceId(entity.getTraceId());
        vo.setUserId(entity.getUserId());
        vo.setUsername(entity.getUsername());
        vo.setOperatorName(entity.getOperatorName());
        vo.setCompanyId(entity.getCompanyId());
        vo.setDepartmentId(entity.getDepartmentId());
        vo.setModule(entity.getModule());
        vo.setAction(entity.getAction());
        vo.setHttpMethod(entity.getHttpMethod());
        vo.setHttpUrl(entity.getHttpUrl());
        vo.setResourceType(entity.getResourceType());
        vo.setResourceId(entity.getResourceId());
        vo.setResourceCode(entity.getResourceCode());
        vo.setDetail(entity.getDetail());
        vo.setIpAddress(entity.getIpAddress());
        vo.setUserAgent(entity.getUserAgent());
        vo.setRequestId(entity.getRequestId());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
