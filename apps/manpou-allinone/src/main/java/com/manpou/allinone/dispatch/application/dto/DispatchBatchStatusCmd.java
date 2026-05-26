package com.manpou.allinone.dispatch.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class DispatchBatchStatusCmd {
    private List<Long> ids;
    private String status;
}
