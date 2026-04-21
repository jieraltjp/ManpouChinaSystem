package com.manpou.allinone.factory.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class FactoryCreateCmd {

    @NotBlank(message = "工厂名称不能为空")
    @Length(max = 128)
    private String factoryName;     // 工厂名称

    @Length(max = 128)
    private String location;        // 工厂位置（省/市）

    @Length(max = 128)
    private String roughLocation;  // 粗略位置

    @Length(max = 64)
    private String contactName;    // 联系人名称

    @Length(max = 32)
    private String contactPhone;   // 联系人电话
}
