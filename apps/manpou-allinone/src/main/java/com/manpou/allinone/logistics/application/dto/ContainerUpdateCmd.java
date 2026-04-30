package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import com.manpou.allinone.logistics.domain.model.ContainerType;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Data
public class ContainerUpdateCmd {

    @Length(max = 32)
    private String containerNo;

    private ContainerType containerType;

    private ContainerStatus status;

    private LocalDate loadDate;

    private LocalDate departureDate;

    private LocalDate arrivalDate;
}
