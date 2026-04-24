package com.manpou.allinone.replenishment.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转采购请求 DTO（v2.0.0）。
 * POST /api/v1/demands/{id}/convert
 */
@Data
public class ConvertDemandCmd {

    @NotNull(message = "工厂ID不能为空")
    private Long factoryId;
}
