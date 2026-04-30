package com.manpou.allinone.customs.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量创建日本清关记录命令。
 * 逻辑：按货柜号筛选已放行（CLEARED）的国内报关记录，勾选后批量创建日本清关。
 * 字段（containerNo/domesticCustomsId/procurementId/productCode/subProductCode）由 UseCase 层从 DomesticCustomsRecord 自动填充。
 */
@Data
public class JapanCustomsBatchCreateCmd {

    /** 货柜号（必填，用于筛选国内报关） */
    @NotBlank(message = "货柜号不能为空")
    private String containerNo;

    /**
     * 要创建清关的国内报关 ID 列表（必填，至少选一条）。
     * UseCase 层从 DomesticCustomsRecord 查询填充字段。
     */
    @NotEmpty(message = "请至少选择一条国内报关记录")
    private List<@NotNull Long> domesticCustomsIds;
}
