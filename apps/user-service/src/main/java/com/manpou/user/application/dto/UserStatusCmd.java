package com.manpou.user.application.dto;

import lombok.Data;

@Data
public class UserStatusCmd {
    private Integer status; // 1=正常 0=禁用
}
