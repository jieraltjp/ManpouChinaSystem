package com.manpou.user.application.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
