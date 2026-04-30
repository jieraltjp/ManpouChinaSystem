package com.manpou.allinone.logistics.application.assembler;

import com.manpou.allinone.logistics.application.dto.ContainerCreateCmd;
import com.manpou.allinone.logistics.application.dto.ContainerPageQuery;
import com.manpou.allinone.logistics.application.dto.ContainerUpdateCmd;
import com.manpou.allinone.logistics.domain.model.Container;
import org.springframework.stereotype.Component;

@Component
public class ContainerAssembler {

    public ContainerPageQuery toDto(Container entity) {
        return ContainerPageQuery.builder()
                .id(entity.getId())
                .containerNo(entity.getContainerNo())
                .containerType(entity.getContainerType())
                .totalCbm(entity.getTotalCbm())
                .totalWeightKg(entity.getTotalWeightKg())
                .planCount(entity.getPlanCount())
                .poolId(entity.getPoolId())
                .status(entity.getStatus())
                .loadDate(entity.getLoadDate())
                .departureDate(entity.getDepartureDate())
                .arrivalDate(entity.getArrivalDate())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public Container toEntity(ContainerCreateCmd cmd) {
        Container entity = new Container();
        entity.setContainerNo(cmd.getContainerNo());
        if (cmd.getContainerType() != null) {
            entity.setContainerType(cmd.getContainerType());
        }
        if (cmd.getPoolId() != null) entity.setPoolId(cmd.getPoolId());
        return entity;
    }

    public void copyUpdate(ContainerUpdateCmd cmd, Container entity) {
        if (cmd.getContainerNo() != null) entity.setContainerNo(cmd.getContainerNo());
        if (cmd.getContainerType() != null) entity.setContainerType(cmd.getContainerType());
        if (cmd.getStatus() != null) entity.advanceStatus(cmd.getStatus());
        if (cmd.getLoadDate() != null) entity.setLoadDate(cmd.getLoadDate());
        if (cmd.getDepartureDate() != null) entity.setDepartureDate(cmd.getDepartureDate());
        if (cmd.getArrivalDate() != null) entity.setArrivalDate(cmd.getArrivalDate());
    }
}
