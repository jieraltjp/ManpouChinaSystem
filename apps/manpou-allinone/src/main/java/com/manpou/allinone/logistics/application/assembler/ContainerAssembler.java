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
                .status(entity.getStatus())
                .loadDate(entity.getLoadDate())
                .departureDate(entity.getDepartureDate())
                .arrivalDate(entity.getArrivalDate())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .shipId(entity.getShipId())
                .shipName(null) // requires JOIN to ship table
                .shipNumber(null)
                .timeSlot(entity.getTimeSlot())
                .arrivalLocation(entity.getArrivalLocation())
                .remarks(entity.getRemarks())
                .cabinetNo(entity.getCabinetNo())
                .period(entity.getPeriod())
                .legacyStatus(entity.getLegacyStatus())
                .showFlag(entity.getShowFlag())
                .legacyId(entity.getLegacyId())
                .legacyUpdater(entity.getLegacyUpdater())
                .legacyUpdatetime(entity.getLegacyUpdatetime())
                .legacyShipName(entity.getShipName())
                .build();
    }

    public Container toEntity(ContainerCreateCmd cmd) {
        Container entity = new Container();
        entity.setContainerNo(cmd.getContainerNo());
        entity.setLoadDate(cmd.getLoadDate());
        entity.setDepartureDate(cmd.getDepartureDate());
        entity.setArrivalDate(cmd.getArrivalDate());
        if (cmd.getShipId() != null) entity.setShipId(cmd.getShipId());
        if (cmd.getTimeSlot() != null) entity.setTimeSlot(cmd.getTimeSlot());
        if (cmd.getArrivalLocation() != null) entity.setArrivalLocation(cmd.getArrivalLocation());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getCabinetNo() != null) entity.setCabinetNo(cmd.getCabinetNo());
        if (cmd.getPeriod() != null) entity.setPeriod(cmd.getPeriod());
        if (cmd.getLegacyStatus() != null) entity.setLegacyStatus(cmd.getLegacyStatus());
        if (cmd.getShowFlag() != null) entity.setShowFlag(cmd.getShowFlag());
        return entity;
    }

    public ContainerPageQuery toDtoFromArray(Object[] row) {
        Container c = (Container) row[0];
        return ContainerPageQuery.builder()
                .id(c.getId())
                .containerNo(c.getContainerNo())
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
                .cabinetNo(c.getCabinetNo())
                .period(c.getPeriod())
                .legacyStatus(c.getLegacyStatus())
                .showFlag(c.getShowFlag())
                .legacyId(c.getLegacyId())
                .legacyUpdater(c.getLegacyUpdater())
                .legacyUpdatetime(c.getLegacyUpdatetime())
                .legacyShipName(c.getShipName())
                .build();
    }

    public void copyUpdate(ContainerUpdateCmd cmd, Container entity) {
        if (cmd.getContainerNo() != null) entity.setContainerNo(cmd.getContainerNo());
        if (cmd.getStatus() != null) entity.advanceStatus(cmd.getStatus());
        if (cmd.getLoadDate() != null) entity.setLoadDate(cmd.getLoadDate());
        if (cmd.getDepartureDate() != null) entity.setDepartureDate(cmd.getDepartureDate());
        if (cmd.getArrivalDate() != null) entity.setArrivalDate(cmd.getArrivalDate());
        if (cmd.getShipId() != null) entity.setShipId(cmd.getShipId());
        if (cmd.getTimeSlot() != null) entity.setTimeSlot(cmd.getTimeSlot());
        if (cmd.getArrivalLocation() != null) entity.setArrivalLocation(cmd.getArrivalLocation());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());
        if (cmd.getCabinetNo() != null) entity.setCabinetNo(cmd.getCabinetNo());
        if (cmd.getPeriod() != null) entity.setPeriod(cmd.getPeriod());
        if (cmd.getLegacyStatus() != null) entity.setLegacyStatus(cmd.getLegacyStatus());
        if (cmd.getShowFlag() != null) entity.setShowFlag(cmd.getShowFlag());
    }
}
