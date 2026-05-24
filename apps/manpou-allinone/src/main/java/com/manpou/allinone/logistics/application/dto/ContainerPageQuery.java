package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ContainerPageQuery {

    private Long id;
    private String containerNo;
    private ContainerStatus status;
    private LocalDate loadDate;
    private LocalDate departureDate;
    private LocalDate arrivalDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Long shipId;
    private String shipName;
    private String shipNumber;
    private String timeSlot;
    private String arrivalLocation;
    private String remarks;

    private String cabinetNo;
    private String period;
    private String legacyStatus;
    private Boolean showFlag;
    private Long legacyId;
    private String legacyUpdater;
    private LocalDateTime legacyUpdatetime;
    private String legacyShipName;
}
