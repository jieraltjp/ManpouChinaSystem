package com.manpou.user.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class PermissionTreeVO {
    private String module;
    private String moduleNameCn;
    private String moduleNameJp;
    private List<PermissionVO> permissions;
}
