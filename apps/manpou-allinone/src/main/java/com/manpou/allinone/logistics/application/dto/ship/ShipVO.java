package com.manpou.allinone.logistics.application.dto.ship;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShipVO {

    private Long id;
    private String shipName;
    private String shipNumber;
    private String carrier;
    private String departurePort;
    private String arrivalPort;
    private Long containerCount;   // 关联货柜数
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
