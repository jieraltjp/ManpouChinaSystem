package com.manpou.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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
