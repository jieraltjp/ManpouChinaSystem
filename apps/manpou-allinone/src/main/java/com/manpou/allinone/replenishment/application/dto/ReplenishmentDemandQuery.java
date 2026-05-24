package com.manpou.allinone.replenishment.application.dto;

import com.manpou.allinone.replenishment.domain.model.DemandType;
import lombok.Data;

@Data
public class ReplenishmentDemandQuery {

    private Integer page = 0;
    private Integer pageSize = 20;
    private DemandType demandType;
    private String keyword;
    /** 关联发注单过滤：true=仅已关联，false=仅未关联，null=全部 */
    private Boolean linked;
}
