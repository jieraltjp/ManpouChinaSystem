package com.manpou.allinone.qc.application.dto;

import com.manpou.allinone.qc.domain.model.QcResult;
import com.manpou.allinone.qc.domain.model.QcStatus;
import com.manpou.allinone.qc.domain.model.QcType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class QcRecordPageQuery {

    private Long id;
    private String qcCode;
    private Long procurementId;
    private String sellerName;
    private String productCode;
    private String subProductCode;
    private Long qcUserId;
    private QcType qcType;
    private LocalDate qcDate;
    private QcResult result;
    private QcStatus status;
    private Integer inspectionCount;
    private Integer passedCount;
    private Integer defectiveCount;
    private Integer boxCount;
    private BigDecimal boxLengthCm;
    private BigDecimal boxWidthCm;
    private BigDecimal boxHeightCm;
    private BigDecimal netWeightPerUnit;
    private BigDecimal grossWeight;
    private BigDecimal taxInclusivePrice;
    private String material;
    private Boolean taxRefund;
    private String qcStandard;
    private String remarks;
    private String images;
    private String destination;
    private Integer quantity;
    private LocalDate orderDate;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
