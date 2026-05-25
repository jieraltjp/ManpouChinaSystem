package com.manpou.allinone.legacyimportlist8.application.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class LegacyImportList8Query {

    private String code;
    private String location;
    private String souko;
    private String destination;
    private String status;
    private Integer showFlag;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo;

    private Integer page = 0;
    private Integer pageSize = 20;
}
