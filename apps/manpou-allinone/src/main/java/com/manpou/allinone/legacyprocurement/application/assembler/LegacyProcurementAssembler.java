package com.manpou.allinone.legacyprocurement.application.assembler;

import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementCreateCmd;
import com.manpou.allinone.legacyprocurement.application.dto.LegacyProcurementPageVO;
import com.manpou.allinone.legacyprocurement.domain.model.LegacyProcurement;
import org.springframework.stereotype.Component;

@Component
public class LegacyProcurementAssembler {

    public LegacyProcurementPageVO toDto(LegacyProcurement entity) {
        return LegacyProcurementPageVO.builder()
                .legacyId(entity.getLegacyId())
                .code(entity.getCode())
                .subCode(entity.getSubCode())
                .img(entity.getImg())
                .itemName(entity.getItemName())
                .orderGroup(entity.getOrderGroup())
                .orderCount(entity.getOrderCount())
                .inspectCount(entity.getInspectCount())
                .yoyakuHasoubi(entity.getYoyakuHasoubi())
                .arrivalDepo(entity.getArrivalDepo())
                .departure(entity.getDeparture())
                .arrival(entity.getArrival())
                .arrivalJikan(entity.getArrivalJikan())
                .arrivalFlag(entity.getArrivalFlag())
                .unitCh(entity.getUnitCh())
                .totalCh(entity.getTotalCh())
                .unitJp(entity.getUnitJp())
                .totalJp(entity.getTotalJp())
                .rate(entity.getRate())
                .fbaStock(entity.getFbaStock())
                .houkoku(entity.getHoukoku())
                .kaitsuke(entity.getKaitsuke())
                .hyoten(entity.getHyoten())
                .kanpu(entity.getKanpu())
                .neStock(entity.getNeStock())
                .container(entity.getContainer())
                .boxNum(entity.getBoxNum())
                .boxCount(entity.getBoxCount())
                .kg(entity.getKg())
                .oneM3(entity.getOneM3())
                .allM3(entity.getAllM3())
                .material(entity.getMaterial())
                .materialCh(entity.getMaterialCh())
                .height(entity.getHeight())
                .width(entity.getWidth())
                .depth(entity.getDepth())
                .infoFile1(entity.getInfoFile1())
                .infoFile2(entity.getInfoFile2())
                .note(entity.getNote())
                .receive(entity.getReceive())
                .updater(entity.getUpdater())
                .updatetime(entity.getUpdatetime())
                .build();
    }

    public LegacyProcurement toEntity(LegacyProcurementCreateCmd cmd) {
        LegacyProcurement entity = new LegacyProcurement();
        entity.setCode(cmd.getCode());
        entity.setSubCode(cmd.getSubCode());
        entity.setItemName(cmd.getItemName());
        entity.setOrderGroup(cmd.getOrderGroup());
        entity.setOrderCount(cmd.getOrderCount());
        entity.setInspectCount(cmd.getInspectCount());
        entity.setYoyakuHasoubi(cmd.getYoyakuHasoubi());
        entity.setArrivalDepo(cmd.getArrivalDepo());
        entity.setDeparture(cmd.getDeparture());
        entity.setArrival(cmd.getArrival());
        entity.setArrivalJikan(cmd.getArrivalJikan());
        entity.setUnitCh(cmd.getUnitCh());
        entity.setTotalCh(cmd.getTotalCh());
        entity.setUnitJp(cmd.getUnitJp());
        entity.setTotalJp(cmd.getTotalJp());
        entity.setRate(cmd.getRate());
        entity.setFbaStock(cmd.getFbaStock());
        entity.setHoukoku(cmd.getHoukoku());
        entity.setKaitsuke(cmd.getKaitsuke());
        entity.setHyoten(cmd.getHyoten());
        entity.setKanpu(cmd.getKanpu());
        entity.setNeStock(cmd.getNeStock());
        entity.setContainer(cmd.getContainer());
        entity.setBoxNum(cmd.getBoxNum());
        entity.setBoxCount(cmd.getBoxCount());
        entity.setKg(cmd.getKg());
        entity.setOneM3(cmd.getOneM3());
        entity.setAllM3(cmd.getAllM3());
        entity.setMaterial(cmd.getMaterial());
        entity.setMaterialCh(cmd.getMaterialCh());
        entity.setHeight(cmd.getHeight());
        entity.setWidth(cmd.getWidth());
        entity.setDepth(cmd.getDepth());
        entity.setInfoFile1(cmd.getInfoFile1());
        entity.setInfoFile2(cmd.getInfoFile2());
        entity.setNote(cmd.getNote());
        entity.setReceive(cmd.getReceive());
        return entity;
    }
}