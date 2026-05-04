package com.manpou.user.application.dto;

import lombok.Data;

/**
 * 角色局部更新（允许修改 isEditable）。
 */
@Data
public class RolePatchCmd {
    private Integer isEditable;
    private String description;
}