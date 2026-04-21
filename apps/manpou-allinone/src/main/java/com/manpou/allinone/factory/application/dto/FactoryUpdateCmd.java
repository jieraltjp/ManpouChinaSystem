package com.manpou.allinone.factory.application.dto;

import com.manpou.allinone.factory.domain.model.FactoryStatus;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class FactoryUpdateCmd {

    @Length(max = 128)
    private String factoryName;

    @Length(max = 128)
    private String location;

    @Length(max = 128)
    private String roughLocation;

    @Length(max = 64)
    private String contactName;

    @Length(max = 32)
    private String contactPhone;

    private FactoryStatus status;
}
