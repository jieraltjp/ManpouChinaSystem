package com.manpou.product.application.dto;

import com.manpou.product.domain.model.ExampleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新示例命令对象。
 * 所有字段可选，不传的字段不更新。
 */
@Data
public class ExampleUpdateCmd {

    @NotBlank(message = "名称不能为空")
    @Size(max = 128, message = "名称最多 128 字符")
    private String name;

    private ExampleStatus status;
}
