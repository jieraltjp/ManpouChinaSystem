package com.manpou.user.application.dto;

import lombok.Data;

/**
 * 用户简洁视图（仅含显示名信息，用于前端下拉/映射）。
 */
@Data
public class UserSimpleVO {
    private String username;
    private String nameCn;
    private String nameJp;
}
