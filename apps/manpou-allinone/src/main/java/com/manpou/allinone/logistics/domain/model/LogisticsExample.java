package com.manpou.allinone.logistics.domain.model;

import com.manpou.allinone.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * 示例实体。
 * 领域层核心，禁外部直接操作。
 */
@Entity
@Table(name = "example", indexes = {
        @Index(name = "idx_create_time", columnList = "create_time"),
        @Index(name = "idx_update_time", columnList = "update_time")
})
@Access(AccessType.FIELD)
@Getter
public class LogisticsExample extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private LogisticsStatus status = LogisticsStatus.PLANNED;

    // ===== 领域方法（禁止外部直接修改字段） =====

    /** 更新名称 */
    public void rename(String newName) {
        this.name = newName;
    }

    /** 更新状态 */
    public void updateStatus(LogisticsStatus newStatus) {
        this.status = newStatus;
    }
}
