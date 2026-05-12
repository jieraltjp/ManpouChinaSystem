package com.manpou.allinone.logistics.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssignShipCmd {

    @NotNull(message = "船只ID不能为空")
    private Long shipId;

    private LocalDate loadDate;   // 装柜日期（可选）
}
