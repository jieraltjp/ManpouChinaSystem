package com.manpou.allinone.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 所有业务实体的基类。
 * 包含审计字段和逻辑删除标记。
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
@Getter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time", nullable = false, updatable = false)
    private LocalDateTime updateTime;

    @CreatedBy
    @Column(name = "create_by", nullable = false, updatable = false, length = 64)
    private String createBy = "SYSTEM";

    @LastModifiedBy
    @Column(name = "update_by", nullable = false, length = 64)
    private String updateBy = "SYSTEM";

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /** 逻辑删除（不物理删除数据）。 */
    public void markDeleted() {
        this.isDeleted = true;
    }

    /** 判断是否已删除。 */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.isDeleted);
    }
}
