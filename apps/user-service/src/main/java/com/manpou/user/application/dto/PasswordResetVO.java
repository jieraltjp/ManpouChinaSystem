package com.manpou.user.application.dto;

import lombok.Data;

@Data
public class PasswordResetVO {
    private String username;
    private String newPassword;

    public PasswordResetVO(String username, String newPassword) {
        this.username = username;
        this.newPassword = newPassword;
    }
}
