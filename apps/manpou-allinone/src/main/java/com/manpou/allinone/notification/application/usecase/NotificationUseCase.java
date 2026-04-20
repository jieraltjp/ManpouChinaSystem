package com.manpou.allinone.notification.application.usecase;

import com.manpou.allinone.notification.application.dto.NotificationCreateCmd;
import com.manpou.allinone.notification.application.dto.NotificationPageQuery;
import com.manpou.allinone.notification.application.dto.NotificationQuery;
import com.manpou.allinone.notification.application.dto.NotificationUpdateCmd;
import com.manpou.allinone.notification.application.assembler.NotificationAssembler;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.common.filter.TraceFilter;
import com.manpou.allinone.notification.domain.model.NotificationExample;
import com.manpou.allinone.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 示例用例服务。
 * 负责编排业务操作，不含领域逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationUseCase {

    private final NotificationRepository exampleRepository;
    private final NotificationAssembler exampleAssembler;

    /**
     * 分页查询。
     */
    @Transactional(readOnly = true)
    public Page<NotificationPageQuery> pageQuery(NotificationQuery query) {
        PageRequest pageRequest = PageRequest.of(
                (query.getPage() - 1),
                Math.min(query.getPageSize(), 100), // 上限 100
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<NotificationExample> page = exampleRepository.findAllByIsDeletedFalse(pageRequest);
        return page.map(exampleAssembler::toDto);
    }

    /**
     * 根据 ID 查询。
     */
    @Transactional(readOnly = true)
    public NotificationPageQuery getById(Long id) {
        NotificationExample entity = exampleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("NotificationExample", id));
        return exampleAssembler.toDto(entity);
    }

    /**
     * 创建。
     */
    @Transactional
    public Long create(NotificationCreateCmd cmd) {
        NotificationExample entity = exampleAssembler.toEntity(cmd);
        NotificationExample saved = exampleRepository.save(entity);
        log.info("[NotificationExample] created, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), saved.getId());
        return saved.getId();
    }

    /**
     * 更新。
     */
    @Transactional
    public void update(Long id, NotificationUpdateCmd cmd) {
        NotificationExample entity = exampleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("NotificationExample", id));
        exampleAssembler.copyToEntity(cmd, entity);
        exampleRepository.save(entity);
        log.info("[NotificationExample] updated, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }

    /**
     * 逻辑删除。
     */
    @Transactional
    public void delete(Long id) {
        NotificationExample entity = exampleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("NotificationExample", id));
        entity.markDeleted();
        exampleRepository.save(entity);
        log.info("[NotificationExample] deleted, traceId={}, id={}", MDC.get(TraceFilter.TRACE_ID_KEY), id);
    }
}
