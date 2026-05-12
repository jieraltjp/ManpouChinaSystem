package com.manpou.allinone.logistics.application.dto.ship;

import lombok.Data;

@Data
public class ShipQuery {

    private Integer page = 0;
    private Integer pageSize = 20;

    private String shipName;
    private String shipNumber;
    private String arrivalPort;
}
