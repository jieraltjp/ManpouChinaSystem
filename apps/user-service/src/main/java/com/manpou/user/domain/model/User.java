package com.manpou.user.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户实体。
 */
@Entity
@Table(name = "user")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(name = "user_code", unique = true, nullable = false)
    private String userCode;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "name_cn")
    private String nameCn;

    @Column(name = "name_jp")
    private String nameJp;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "language")
    private String language = "zh";

    @Column(name = "timezone")
    private String timezone = "CST";

    /** 1=正常 0=禁用 */
    @Column(name = "status", columnDefinition = "TINYINT")
    private Integer status = 1;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "last_login_ip")
    private String lastLoginIp;

    /** APPROVED/PENDING/REJECTED */
    @Column(name = "registration_status")
    private String registrationStatus = "APPROVED";

    @Column(name = "reject_reason")
    private String rejectReason;

    /** 是否可登录：status=1 且 registration_status=APPROVED */
    public boolean canLogin() {
        return status != null && status == 1 && "APPROVED".equals(registrationStatus);
    }
}
