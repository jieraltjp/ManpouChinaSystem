package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Data
public class ContainerUpdateCmd {

    @Length(max = 32)
    private String containerNo;

    private ContainerStatus status;

    private LocalDate loadDate;

    private LocalDate departureDate;

    private LocalDate arrivalDate;

    private Long shipId;

    @Size(max = 32)
    private String timeSlot;

    @Size(max = 128)
    private String arrivalLocation;

    @Size(max = 512)
    private String remarks;

    @Size(max = 16)
    private String cabinetNo;

    @Size(max = 16)
    private String period;

    @Size(max = 32)
    private String legacyStatus;

    private Boolean showFlag;
}
