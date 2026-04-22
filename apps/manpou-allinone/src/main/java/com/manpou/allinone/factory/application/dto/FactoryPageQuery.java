package com.manpou.allinone.factory.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.FactoryCategory;
import com.manpou.allinone.factory.domain.model.PaymentTerms;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FactoryPageQuery {

    private Long id;
    private String factoryCode;
    private String factoryName;
    private FactoryCategory category;

    // 地理信息
    private String province;
    private String city;
    private String county;
    private String roughLocation;
    private BigDecimal longitude;
    private BigDecimal latitude;

    // 联系方式
    private String contactName;
    private String contactPhone;
    private String contactWechat;
    private String contactQq;

    // 合作信息
    private CooperationStatus cooperationStatus;
    private PaymentTerms paymentTerms;

    // 审计字段
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 备注
    private String notes;
}
