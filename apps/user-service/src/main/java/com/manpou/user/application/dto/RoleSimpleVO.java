package com.manpou.user.application.dto;

import lombok.Data;

import java.time.LocalDateTime;

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
