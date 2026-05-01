package com.manpou.user.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateCmd {
    private String nameCn;
    private String nameJp;
    private String phone;
    private String avatarUrl;
    private Long companyId;
    private Long departmentId;
    private List<Long> positionIds;
    private String language;
    private String timezone;
}
