package com.manpou.user.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserRolesCmd {
    private List<Long> roleIds;
}
