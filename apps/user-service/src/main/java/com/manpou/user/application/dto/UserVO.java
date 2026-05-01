package com.manpou.user.application.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String userCode;
    private String username;
    private String nameCn;
    private String nameJp;
    private String email;
    private String phone;
    private String avatarUrl;
    private Long companyId;
    private String companyName;
    private Long departmentId;
    private String departmentName;
    private List<PositionVO> positions;
    private List<RoleSimpleVO> roles;
    private String customsCode;
    private String customsLicense;
    private String language;
    private String timezone;
    private Integer status;
    private String registrationStatus;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createTime;

    @Data
    public static class PositionVO {
        private Long id;
        private String nameCn;
        private String nameJp;
    }

    @Data
    public static class RoleSimpleVO {
        private Long id;
        private String roleCode;
        private String roleNameCn;
        private String roleNameJp;
    }
}
