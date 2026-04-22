package com.manpou.allinone.product.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主货号自动补全响应 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterCodeSuggestVO {
    /** 主货号 */
    private String masterCode;
    /** 中文名称（用于显示） */
    private String nameZh;
    /** 该主货号下子货号数量 */
    private Integer colorCount;
}
