package com.manpou.allinone.qc.application.dto;

import com.manpou.allinone.qc.domain.model.QcResult;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class QcRecordQuery {

    private Integer page = 0;
    private Integer pageSize = 20;

    private String qcCode;
    private String productCode;
    private QcResult result;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate qcDateFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate qcDateTo;

    private Long procurementId;
    private Long shipmentBatchId;          // 按出货批次筛选（V43新增）
}
