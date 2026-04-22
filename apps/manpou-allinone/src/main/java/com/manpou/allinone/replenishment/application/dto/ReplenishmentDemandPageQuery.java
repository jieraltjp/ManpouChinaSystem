package com.manpou.allinone.replenishment.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.replenishment.domain.model.DemandStatus;
import com.manpou.allinone.replenishment.domain.model.DemandType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReplenishmentDemandPageQuery {

    private Long id;
    private String demandCode;
    private DemandType demandType;
    private String productCode;
    /** 子货号数组（JSON 解析后）；单个时为单元素列表 */
    private List<String> subProductCodes;
    private Integer quantity;
    private String destination;
    private String japanLead;
    private DemandStatus status;
    private Long linkedProcurementId;
    private String remarks;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
