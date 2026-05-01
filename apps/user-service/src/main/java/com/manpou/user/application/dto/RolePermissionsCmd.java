package com.manpou.user.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class RolePermissionsCmd {
    private List<Long> permissionIds;
}
