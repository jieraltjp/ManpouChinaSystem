package com.manpou.allinone.product.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 子货号候选项响应 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubCodeSuggestVO {
    /** 子货号/色号（如 re / wh / bk） */
    private String subCode;
    /** 颜色名称（如 红色 / 白色 / 黑色） */
    private String colorName;
}
