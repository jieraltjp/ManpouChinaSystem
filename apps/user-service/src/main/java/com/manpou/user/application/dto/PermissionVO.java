package com.manpou.user.application.dto;

import lombok.Data;

@Data
public class PermissionVO {
    private Long id;
    private String permissionCode;
    private String permissionNameCn;
    private String permissionNameJp;
    private String module;
    private String action;
    private String description;
    private Integer sortOrder;
    private Boolean checked; // 前端使用

    public PermissionVO() {}

    public PermissionVO(Long id, String permissionCode, String permissionNameCn,
                       String permissionNameJp, String module, String action,
                       String description, Integer sortOrder) {
        this.id = id;
        this.permissionCode = permissionCode;
        this.permissionNameCn = permissionNameCn;
        this.permissionNameJp = permissionNameJp;
        this.module = module;
        this.action = action;
        this.description = description;
        this.sortOrder = sortOrder;
    }
}
