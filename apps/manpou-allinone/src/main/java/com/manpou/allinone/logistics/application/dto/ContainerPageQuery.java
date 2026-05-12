package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import com.manpou.allinone.logistics.domain.model.ContainerType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ContainerPageQuery {

    private Long id;
    private String containerNo;
    private ContainerType containerType;
    private BigDecimal totalCbm;
    private BigDecimal totalWeightKg;
    private Integer planCount;
    private Long poolId;
    private ContainerStatus status;
    private LocalDate loadDate;
    private LocalDate departureDate;
    private LocalDate arrivalDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ===== v2.0 扩展字段（SPEC-B12）=====
    private Long shipId;                   // 关联船只ID
    private String shipName;                // 船名（JOIN ship）
    private String shipNumber;             // 船号（JOIN ship）
    private String timeSlot;              // 时间段
    private String arrivalLocation;        // 到岗地点
    private String remarks;               // 备注
}
