package com.manpou.allinone.dispatch.application.dto;

import lombok.Data;

@Data
public class DispatchQuery {

    private Integer page = 0;
    private Integer pageSize = 20;

    private String code;
    private String destination;
    private String manager;
    private String showFlag;
}