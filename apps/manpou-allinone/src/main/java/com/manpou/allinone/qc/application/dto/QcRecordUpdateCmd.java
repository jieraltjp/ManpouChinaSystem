package com.manpou.allinone.qc.application.dto;

import com.manpou.allinone.qc.domain.model.QcResult;
import com.manpou.allinone.qc.domain.model.QcStatus;
import com.manpou.allinone.qc.domain.model.QcType;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class QcRecordUpdateCmd {

    @Length(max = 128)
    private String sellerName;

    private Long qcUserId;

    private QcType qcType;

    private LocalDate qcDate;

    private QcResult result;

    private QcStatus status;

    private Integer inspectionCount;

    private Integer passedCount;

    private Integer boxCount;

    private BigDecimal boxLengthCm;

    private BigDecimal boxWidthCm;

    private BigDecimal boxHeightCm;

    private BigDecimal netWeightPerUnit;

    private BigDecimal grossWeight;

    private BigDecimal taxInclusivePrice;

    @Length(max = 64)
    private String material;

    private Boolean taxRefund;

    @Length(max = 512)
    private String qcStandard;

    @Length(max = 512)
    private String remarks;

    private String images;
}
