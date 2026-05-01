package com.manpou.user.application.dto;

import lombok.Data;

@Data
public class UserPageQuery {
    private String keyword;       // 姓名/账号/邮箱
    private Long companyId;
    private Long departmentId;
    private Long roleId;
    private Integer status;       // 1=正常 0=禁用
    private Integer page = 0;
    private Integer size = 20;
}
