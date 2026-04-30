package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import lombok.Data;

@Data
public class ContainerQuery {

    private Integer page = 0;
    private Integer pageSize = 20;

    private ContainerStatus status;
    private String containerNo;
    private Long poolId;
}
