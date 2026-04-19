package com.manpou.customs.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建示例命令对象。
 * 对应 HTTP POST 请求体。
 */
@Data
public class ExampleCreateCmd {

    @NotBlank(message = "名称不能为空")
    @Size(max = 128, message = "名称最多 128 字符")
    private String name;
}
