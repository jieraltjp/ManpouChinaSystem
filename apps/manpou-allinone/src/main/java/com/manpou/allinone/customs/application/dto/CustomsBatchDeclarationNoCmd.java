package com.manpou.allinone.customs.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量修改报关申报号命令（v2.0）。
 */
@Data
public class CustomsBatchDeclarationNoCmd {

    @NotEmpty(message = "至少选择一条记录")
    private List<Long> ids;

    @Size(max = 64, message = "报关申报号最多 64 字符")
    private String customsDeclarationNo;
}
