package com.manpou.user.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 用户新增命令 */
@Data
public class UserCreateCmd {
    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank(message = "nameCn is required")
    private String nameCn;

    private String nameJp;

    @NotBlank(message = "email is required")
    @Email
    private String email;

    private String phone;
    private String avatarUrl;
    private Long companyId;
    private Long departmentId;
    private List<Long> positionIds;
    private List<Long> roleIds;
    private String customsCode;
    private String customsLicense;
    private String language = "zh";
    private String timezone = "CST";
}

/** 用户更新命令 */
@Data
public class UserUpdateCmd {
    private String nameCn;
    private String nameJp;
    private String phone;
    private String avatarUrl;
    private Long companyId;
    private Long departmentId;
    private List<Long> positionIds;
    private String customsCode;
    private String customsLicense;
    private String language;
    private String timezone;
}

/** 用户状态更新 */
@Data
public class UserStatusCmd {
    private Integer status; // 1=正常 0=禁用
}

/** 密码重置 */
@Data
public class PasswordResetCmd {
    private String newPassword;
}

/** 角色分配 */
@Data
public class UserRolesCmd {
    private List<Long> roleIds;
}

/** 用户详情 VO */
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

/** 用户分页查询 */
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

/** 用户分页 VO */
@Data
public class UserPageVO {
    private List<UserVO> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
}

/** 密码重置响应 */
@Data
public class PasswordResetVO {
    private String username;
    private String newPassword;
    public PasswordResetVO(String username, String newPassword) {
        this.username = username;
        this.newPassword = newPassword;
    }
}
