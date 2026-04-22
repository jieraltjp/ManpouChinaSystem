package com.manpou.allinone.factory.application.dto;

import com.manpou.allinone.factory.domain.model.CooperationStatus;
import lombok.Data;

@Data
public class FactoryQuery {

    private Integer page = 0;
    private Integer pageSize = 20;
    private String factoryName;
    private CooperationStatus cooperationStatus;
}
