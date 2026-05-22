package com.manpou.allinone.factory.application.dto;

import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.FactoryCategory;
import com.manpou.allinone.factory.domain.model.PaymentTerms;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FactoryUpdateCmd {

    @Size(max = 128)
    private String factoryName;

    private FactoryCategory category;

    @Size(max = 64)
    private String province;

    @Size(max = 64)
    private String city;

    @Size(max = 64)
    private String county;

    @Size(max = 500)
    private String roughLocation;

    private BigDecimal longitude;
    private BigDecimal latitude;

    @Size(max = 64)
    private String contactName;

    @Size(max = 32)
    private String contactPhone;

    @Size(max = 64)
    private String contactWechat;

    @Size(max = 32)
    private String contactQq;

    private CooperationStatus cooperationStatus;
    private Boolean needsQc;                       // 是否需要验货（SPEC-B13）

    private PaymentTerms paymentTerms;

    @Size(max = 500)
    private String notes;
}
