package com.manpou.user.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * 角色实体。
 */
@Entity
@Table(name = "role")
@Getter
@Setter
public class Role extends BaseEntity {

    @Column(name = "role_code", unique = true, nullable = false)
    private String roleCode;

    @Column(name = "role_name_cn", nullable = false)
    private String roleNameCn;

    @Column(name = "role_name_jp", nullable = false)
    private String roleNameJp;

    @Column(name = "role_type")
    private String roleType; // SYSTEM/BUSINESS

    @Column(name = "description")
    private String description;

    /** 0=系统内置不可编辑 */
    @Column(name = "is_editable", columnDefinition = "TINYINT")
    private Integer isEditable = 1;

    @Column(name = "status", columnDefinition = "TINYINT")
    private Integer status = 1;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
