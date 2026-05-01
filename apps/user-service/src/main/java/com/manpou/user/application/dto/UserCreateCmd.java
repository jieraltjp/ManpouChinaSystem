package com.manpou.user.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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
