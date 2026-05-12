package com.manpou.allinone.logistics.application.assembler;

import com.manpou.allinone.logistics.application.dto.ship.ShipCreateCmd;
import com.manpou.allinone.logistics.application.dto.ship.ShipUpdateCmd;
import com.manpou.allinone.logistics.application.dto.ship.ShipVO;
import com.manpou.allinone.logistics.domain.model.Ship;
import org.springframework.stereotype.Component;

@Component
public class ShipAssembler {

    public ShipVO toVo(Ship entity, Long containerCount) {
        return ShipVO.builder()
                .id(entity.getId())
                .shipName(entity.getShipName())
                .shipNumber(entity.getShipNumber())
                .carrier(entity.getCarrier())
                .departurePort(entity.getDeparturePort())
                .arrivalPort(entity.getArrivalPort())
                .containerCount(containerCount)
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public Ship toEntity(ShipCreateCmd cmd) {
        Ship entity = new Ship();
        entity.setShipName(cmd.getShipName());
        entity.setShipNumber(cmd.getShipNumber());
        entity.setCarrier(cmd.getCarrier());
        entity.setDeparturePort(cmd.getDeparturePort());
        entity.setArrivalPort(cmd.getArrivalPort());
        return entity;
    }

    public void copyUpdate(ShipUpdateCmd cmd, Ship entity) {
        if (cmd.getShipName() != null) entity.setShipName(cmd.getShipName());
        if (cmd.getCarrier() != null) entity.setCarrier(cmd.getCarrier());
        if (cmd.getDeparturePort() != null) entity.setDeparturePort(cmd.getDeparturePort());
        if (cmd.getArrivalPort() != null) entity.setArrivalPort(cmd.getArrivalPort());
    }
}
