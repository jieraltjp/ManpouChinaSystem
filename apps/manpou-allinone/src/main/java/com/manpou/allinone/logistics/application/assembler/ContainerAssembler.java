package com.manpou.allinone.logistics.application.assembler;

import com.manpou.allinone.logistics.application.dto.ContainerCreateCmd;
import com.manpou.allinone.logistics.application.dto.ContainerPageQuery;
import com.manpou.allinone.logistics.application.dto.ContainerUpdateCmd;
import com.manpou.allinone.logistics.domain.model.Container;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ContainerAssembler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000);

    public String generateContainerNo() {
        String date = LocalDateTime.now().format(DATE_FMT);
        return String.format("C-%s-%04d", date, SEQ.incrementAndGet() % 10000);
    }

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
                // v2.0 扩展字段
                .shipId(entity.getShipId())
                .timeSlot(entity.getTimeSlot())
                .arrivalLocation(entity.getArrivalLocation())
                .remarks(entity.getRemarks())
                .build();
    }

    public Container toEntity(ContainerCreateCmd cmd) {
        Container entity = new Container();
        entity.setContainerNo(cmd.getContainerNo());
        if (cmd.getContainerType() != null) {
            entity.setContainerType(cmd.getContainerType());
        }
        if (cmd.getPoolId() != null) entity.setPoolId(cmd.getPoolId());
        // v2.0 扩展字段
        if (cmd.getTimeSlot() != null) entity.setTimeSlot(cmd.getTimeSlot());
        if (cmd.getArrivalLocation() != null) entity.setArrivalLocation(cmd.getArrivalLocation());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        return entity;
    }

    /**
     * 从 JOIN 查询 Object[] 转换为 DTO（含 shipName / shipNumber）。
     * array[0]=Container, array[1]=shipName(String), array[2]=shipNumber(String)
     */
    public ContainerPageQuery toDtoFromArray(Object[] row) {
        Container c = (Container) row[0];
        return ContainerPageQuery.builder()
                .id(c.getId())
                .containerNo(c.getContainerNo())
                .containerType(c.getContainerType())
                .totalCbm(c.getTotalCbm())
                .totalWeightKg(c.getTotalWeightKg())
                .planCount(c.getPlanCount())
                .poolId(c.getPoolId())
                .status(c.getStatus())
                .loadDate(c.getLoadDate())
                .departureDate(c.getDepartureDate())
                .arrivalDate(c.getArrivalDate())
                .createTime(c.getCreateTime())
                .updateTime(c.getUpdateTime())
                .shipId(c.getShipId())
                .shipName((String) row[1])
                .shipNumber((String) row[2])
                .timeSlot(c.getTimeSlot())
                .arrivalLocation(c.getArrivalLocation())
                .remarks(c.getRemarks())
                .build();
    }

    public void copyUpdate(ContainerUpdateCmd cmd, Container entity) {
        if (cmd.getContainerNo() != null) entity.setContainerNo(cmd.getContainerNo());
        if (cmd.getContainerType() != null) entity.setContainerType(cmd.getContainerType());
        if (cmd.getStatus() != null) entity.advanceStatus(cmd.getStatus());
        if (cmd.getLoadDate() != null) entity.setLoadDate(cmd.getLoadDate());
        if (cmd.getDepartureDate() != null) entity.setDepartureDate(cmd.getDepartureDate());
        if (cmd.getArrivalDate() != null) entity.setArrivalDate(cmd.getArrivalDate());
        // v2.0 扩展字段
        if (cmd.getShipId() != null) entity.setShipId(cmd.getShipId());
        if (cmd.getTimeSlot() != null) entity.setTimeSlot(cmd.getTimeSlot());
        if (cmd.getArrivalLocation() != null) entity.setArrivalLocation(cmd.getArrivalLocation());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
    }
}
