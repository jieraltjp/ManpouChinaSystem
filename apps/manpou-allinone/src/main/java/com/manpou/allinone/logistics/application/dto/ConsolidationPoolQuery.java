package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ConsolidationPoolStatus;
import lombok.Data;

@Data
public class ConsolidationPoolQuery {

    private Integer page = 0;
    private Integer pageSize = 20;

    private ConsolidationPoolStatus status;
    private String destinationPort;
}
