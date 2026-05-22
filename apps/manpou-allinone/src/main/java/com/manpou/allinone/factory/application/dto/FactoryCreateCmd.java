package com.manpou.allinone.factory.application.dto;

import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.FactoryCategory;
import com.manpou.allinone.factory.domain.model.PaymentTerms;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FactoryCreateCmd {

    @NotBlank(message = "工厂名称不能为空")
    @Size(max = 128)
    private String factoryName;     // 工厂名称

    private FactoryCategory category;  // 分类

    // 地理信息
    @Size(max = 64)
    private String province;        // 省

    @Size(max = 64)
    private String city;            // 市

    @Size(max = 64)
    private String county;          // 县/区

    @Size(max = 500)
    private String roughLocation;  // 详细地址（粗略）

    private BigDecimal longitude;   // 经度
    private BigDecimal latitude;    // 纬度

    // 联系方式
    @Size(max = 64)
    private String contactName;    // 联系人姓名

    @Size(max = 32)
    private String contactPhone;   // 手机号

    @Size(max = 64)
    private String contactWechat;  // 微信号

    @Size(max = 32)
    private String contactQq;      // QQ号

    // 合作信息
    private CooperationStatus cooperationStatus;  // 合作状态
    private Boolean needsQc;                       // 是否需要验货（SPEC-B13）

    private PaymentTerms paymentTerms;             // 账期

    @Size(max = 500)
    private String notes;          // 备注
}
