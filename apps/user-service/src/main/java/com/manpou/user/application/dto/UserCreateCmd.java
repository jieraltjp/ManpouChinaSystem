package com.manpou.user.application.dto;

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
    @Size(min = 6, max = 100, message = "password must be 6-100 characters")
    private String password;

    private String nameCn;

    private String nameJp;

    private String email;

    private String phone;
    private String avatarUrl;
    private Long companyId;
    private Long departmentId;
    private List<Long> positionIds;
    private List<Long> roleIds;
    private String language = "zh";
    private String timezone = "CST";
}
