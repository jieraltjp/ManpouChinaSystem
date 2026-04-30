package com.manpou.user.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 权限实体。
 */
@Entity
@Table(name = "permission")
@Getter
@Setter
public class Permission extends BaseEntity {

    @Column(name = "permission_code", unique = true, nullable = false)
    private String permissionCode;

    @Column(name = "permission_name_cn", nullable = false)
    private String permissionNameCn;

    @Column(name = "permission_name_jp", nullable = false)
    private String permissionNameJp;

    @Column(name = "module", nullable = false)
    private String module;

    @Column(name = "action_", nullable = false)
    private String action; // action_ 映射数据库 action 列

    @Column(name = "description")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "status", columnDefinition = "TINYINT")
    private Integer status = 1;
}
