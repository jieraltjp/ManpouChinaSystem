package com.manpou.user.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserPageVO {
    private List<UserVO> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
}
