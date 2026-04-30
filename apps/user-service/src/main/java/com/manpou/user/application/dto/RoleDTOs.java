package com.manpou.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 角色新增命令 */
@Data
public class RoleCreateCmd {
    @NotBlank(message = "roleCode is required")
    @Size(max = 32)
    private String roleCode;

    @NotBlank(message = "roleNameCn is required")
    private String roleNameCn;

    private String roleNameJp;
    private String roleType = "BUSINESS";
    private String description;
    private List<Long> permissionIds;
}

/** 角色更新命令 */
@Data
public class RoleUpdateCmd {
    private String roleNameCn;
    private String roleNameJp;
    private String description;
}

/** 权限分配命令 */
@Data
public class RolePermissionsCmd {
    private List<Long> permissionIds;
}

/** 权限 VO */
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

/** 角色 VO */
@Data
public class RoleVO {
    private Long id;
    private String roleCode;
    private String roleNameCn;
    private String roleNameJp;
    private String roleType;
    private String description;
    private Integer isEditable;
    private Integer status;
    private Integer userCount;
    private LocalDateTime createTime;
    private List<PermissionVO> permissions;
}

/** 角色列表 VO（不含权限列表） */
@Data
public class RoleSimpleVO {
    private Long id;
    private String roleCode;
    private String roleNameCn;
    private String roleNameJp;
    private String roleType;
    private Integer isEditable;
    private Integer status;
    private Integer userCount;
    private LocalDateTime createTime;
}

/** 权限树 VO */
@Data
public class PermissionTreeVO {
    private String module;
    private String moduleNameCn;
    private String moduleNameJp;
    private List<PermissionVO> permissions;
}
