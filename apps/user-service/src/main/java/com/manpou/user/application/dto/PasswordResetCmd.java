package com.manpou.user.application.dto;

import lombok.Data;

@Data
public class PasswordResetCmd {
    private String newPassword;
}
