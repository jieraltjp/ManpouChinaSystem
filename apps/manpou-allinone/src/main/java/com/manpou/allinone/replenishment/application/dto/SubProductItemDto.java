package com.manpou.allinone.replenishment.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

/**
 * 子货号明细 DTO（v1.6.0）。
 * 用于创建/编辑需求单表单。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubProductItemDto {

    @NotBlank(message = "子货号不能为空")
    @Length(max = 64)
    private String subCode;

    @NotNull(message = "数量不能为空")
    @Min(value = 0, message = "数量不能为负数")
    private Integer quantity;

    @Length(max = 128)
    private String destination;
}
