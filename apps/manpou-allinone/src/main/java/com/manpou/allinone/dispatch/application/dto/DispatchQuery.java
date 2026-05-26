package com.manpou.allinone.dispatch.application.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DispatchQuery {

    private Integer page = 0;
    private Integer pageSize = 20;

    /** 货号/商品名模糊搜索 */
    private String keyword;
    /** 目的地/负责人模糊搜索（OR 关系） */
    private String destManager;
    private String showFlag;
    private String status;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}