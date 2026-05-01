package com.manpou.user.application.dto;

import lombok.Data;

@Data
public class RoleUpdateCmd {
    private String roleNameCn;
    private String roleNameJp;
    private String description;
}
