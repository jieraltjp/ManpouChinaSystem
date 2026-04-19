package com.manpou.warehouse.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
public class Example extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private ExampleStatus status = ExampleStatus.ACTIVE;

    // ===== 领域方法（禁止外部直接修改字段） =====

    /** 更新名称 */
    public void rename(String newName) {
        this.name = newName;
    }

    /** 更新状态 */
    public void updateStatus(ExampleStatus newStatus) {
        this.status = newStatus;
    }
}
