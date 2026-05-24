package com.manpou.allinone.offlineorder.application.assembler;

import com.manpou.allinone.offlineorder.application.dto.OfflineOrderCreateCmd;
import com.manpou.allinone.offlineorder.application.dto.OfflineOrderPageVO;
import com.manpou.allinone.offlineorder.application.dto.OfflineOrderUpdateCmd;
import com.manpou.allinone.offlineorder.domain.model.OfflineOrder;
import org.springframework.stereotype.Component;

@Component
public class OfflineOrderAssembler {

    public OfflineOrderPageVO toDto(OfflineOrder entity) {
        return OfflineOrderPageVO.builder()
                .id(entity.getId())
                .showFlag(entity.getShowFlag())
                .code(entity.getCode())
                .subCode(entity.getSubCode())
                .houkoku(entity.getHoukoku())
                .infoFile(entity.getInfoFile())
                .itemName(entity.getItemName())
                .volumeCount(entity.getVolumeCount())
                .orderCount(entity.getOrderCount())
                .expectedDate(entity.getExpectedDate())
                .orderDate(entity.getOrderDate())
                .arrival(entity.getArrival())
                .unitCh(entity.getUnitCh())
                .rate(entity.getRate())
                .souko(entity.getSouko())
                .factory(entity.getFactory())
                .contactor(entity.getContactor())
                .contactorTel(entity.getContactorTel())
                .principal(entity.getPrincipal())
                .memo(entity.getMemo())
                .link(entity.getLink())
                .updater(entity.getUpdater())
                .updatetime(entity.getUpdatetime())
                .inventoryNote(entity.getInventoryNote())
                .rireki(entity.getRireki())
                .build();
    }

    public OfflineOrder toEntity(OfflineOrderCreateCmd cmd) {
        OfflineOrder entity = new OfflineOrder();
        copyToEntity(cmd, entity);
        return entity;
    }

    public void copyToEntity(OfflineOrderCreateCmd cmd, OfflineOrder entity) {
        entity.setShowFlag(cmd.getShowFlag() != null ? cmd.getShowFlag() : 1);
        entity.setCode(cmd.getCode());
        entity.setSubCode(cmd.getSubCode());
        entity.setHoukoku(cmd.getHoukoku());
        entity.setInfoFile(cmd.getInfoFile());
        entity.setItemName(cmd.getItemName());
        entity.setVolumeCount(cmd.getVolumeCount());
        entity.setOrderCount(cmd.getOrderCount());
        entity.setExpectedDate(cmd.getExpectedDate());
        entity.setOrderDate(cmd.getOrderDate());
        entity.setArrival(cmd.getArrival());
        entity.setUnitCh(cmd.getUnitCh());
        entity.setRate(cmd.getRate());
        entity.setSouko(cmd.getSouko());
        entity.setFactory(cmd.getFactory());
        entity.setContactor(cmd.getContactor());
        entity.setContactorTel(cmd.getContactorTel());
        entity.setPrincipal(cmd.getPrincipal());
        entity.setMemo(cmd.getMemo());
        entity.setLink(cmd.getLink());
        entity.setInventoryNote(cmd.getInventoryNote());
        entity.setRireki(cmd.getRireki());
    }

    public void copyToEntity(OfflineOrderUpdateCmd cmd, OfflineOrder entity) {
        if (cmd.getShowFlag() != null) entity.setShowFlag(cmd.getShowFlag());
        if (cmd.getCode() != null) entity.setCode(cmd.getCode());
        if (cmd.getSubCode() != null) entity.setSubCode(cmd.getSubCode());
        if (cmd.getHoukoku() != null) entity.setHoukoku(cmd.getHoukoku());
        if (cmd.getInfoFile() != null) entity.setInfoFile(cmd.getInfoFile());
        if (cmd.getItemName() != null) entity.setItemName(cmd.getItemName());
        if (cmd.getVolumeCount() != null) entity.setVolumeCount(cmd.getVolumeCount());
        if (cmd.getOrderCount() != null) entity.setOrderCount(cmd.getOrderCount());
        if (cmd.getExpectedDate() != null) entity.setExpectedDate(cmd.getExpectedDate());
        if (cmd.getOrderDate() != null) entity.setOrderDate(cmd.getOrderDate());
        if (cmd.getArrival() != null) entity.setArrival(cmd.getArrival());
        if (cmd.getUnitCh() != null) entity.setUnitCh(cmd.getUnitCh());
        if (cmd.getRate() != null) entity.setRate(cmd.getRate());
        if (cmd.getSouko() != null) entity.setSouko(cmd.getSouko());
        if (cmd.getFactory() != null) entity.setFactory(cmd.getFactory());
        if (cmd.getContactor() != null) entity.setContactor(cmd.getContactor());
        if (cmd.getContactorTel() != null) entity.setContactorTel(cmd.getContactorTel());
        if (cmd.getPrincipal() != null) entity.setPrincipal(cmd.getPrincipal());
        if (cmd.getMemo() != null) entity.setMemo(cmd.getMemo());
        if (cmd.getLink() != null) entity.setLink(cmd.getLink());
        if (cmd.getInventoryNote() != null) entity.setInventoryNote(cmd.getInventoryNote());
        if (cmd.getRireki() != null) entity.setRireki(cmd.getRireki());
    }
}