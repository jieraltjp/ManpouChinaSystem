package com.manpou.allinone.logistics.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Data
public class ContainerCreateCmd {

    @NotBlank(message = "货柜号不能为空")
    @Length(max = 32)
    private String containerNo;

    private LocalDate loadDate;

    private LocalDate departureDate;

    private LocalDate arrivalDate;

    private Long shipId;

    @Length(max = 32)
    private String timeSlot;

    @Length(max = 128)
    private String arrivalLocation;

    @Length(max = 512)
    private String remarks;

    @Length(max = 16)
    private String cabinetNo;

    @Length(max = 16)
    private String period;

    @Length(max = 32)
    private String legacyStatus;

    private Boolean showFlag;
}
