package com.manpou.allinone.dispatch.application.assembler;

import com.manpou.allinone.dispatch.application.dto.DispatchCreateCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchUpdateCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchVO;
import com.manpou.allinone.dispatch.domain.model.Dispatch;
import org.springframework.stereotype.Component;

@Component
public class DispatchAssembler {

    public DispatchVO toVo(Dispatch entity) {
        return DispatchVO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .manager(entity.getManager())
                .destination(entity.getDestination())
                .tax(entity.getTax())
                .material(entity.getMaterial())
                .kensa(entity.getKensa())
                .quantity(entity.getQuantity())
                .pieces(entity.getPieces())
                .weight(entity.getWeight())
                .weight2(entity.getWeight2())
                .length(entity.getLength())
                .location(entity.getLocation())
                .dispatchDate(entity.getDispatchDate())
                .status(entity.getStatus())
                .other(entity.getOther())
                .unitPrice(entity.getUnitPrice())
                .rate(entity.getRate())
                .warehouse(entity.getWarehouse())
                .factoryAddr(entity.getFactoryAddr())
                .showFlag(entity.getShowFlag())
                .rireki(entity.getRireki())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public Dispatch toEntity(DispatchCreateCmd cmd) {
        Dispatch entity = new Dispatch();
        entity.setCode(cmd.getCode());
        entity.setManager(cmd.getManager());
        entity.setDestination(cmd.getDestination());
        entity.setTax(cmd.getTax() != null ? cmd.getTax() : "");
        entity.setMaterial(cmd.getMaterial() != null ? cmd.getMaterial() : "");
        entity.setKensa(cmd.getKensa());
        entity.setQuantity(cmd.getQuantity());
        entity.setPieces(cmd.getPieces());
        entity.setWeight(cmd.getWeight());
        entity.setWeight2(cmd.getWeight2() != null ? cmd.getWeight2() : 0.0);
        entity.setLength(cmd.getLength() != null ? cmd.getLength() : 0.0);
        entity.setLocation(cmd.getLocation() != null ? cmd.getLocation() : "");
        entity.setDispatchDate(cmd.getDispatchDate());
        entity.setStatus(cmd.getStatus() != null ? cmd.getStatus() : "");
        entity.setOther(cmd.getOther() != null ? cmd.getOther() : "");
        entity.setUnitPrice(cmd.getUnitPrice() != null ? cmd.getUnitPrice() : 0.0);
        entity.setRate(cmd.getRate() != null ? cmd.getRate() : 0.0);
        entity.setWarehouse(cmd.getWarehouse() != null ? cmd.getWarehouse() : "");
        entity.setFactoryAddr(cmd.getFactoryAddr());
        entity.setShowFlag(cmd.getShowFlag() != null ? cmd.getShowFlag() : 0);
        entity.setRireki(cmd.getRireki());
        return entity;
    }

    public void copyUpdate(DispatchUpdateCmd cmd, Dispatch entity) {
        if (cmd.getCode() != null) entity.setCode(cmd.getCode());
        if (cmd.getManager() != null) entity.setManager(cmd.getManager());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getTax() != null) entity.setTax(cmd.getTax());
        if (cmd.getMaterial() != null) entity.setMaterial(cmd.getMaterial());
        if (cmd.getKensa() != null) entity.setKensa(cmd.getKensa());
        if (cmd.getQuantity() != null) entity.setQuantity(cmd.getQuantity());
        if (cmd.getPieces() != null) entity.setPieces(cmd.getPieces());
        if (cmd.getWeight() != null) entity.setWeight(cmd.getWeight());
        if (cmd.getWeight2() != null) entity.setWeight2(cmd.getWeight2());
        if (cmd.getLength() != null) entity.setLength(cmd.getLength());
        if (cmd.getLocation() != null) entity.setLocation(cmd.getLocation());
        if (cmd.getDispatchDate() != null) entity.setDispatchDate(cmd.getDispatchDate());
        if (cmd.getStatus() != null) entity.setStatus(cmd.getStatus());
        if (cmd.getOther() != null) entity.setOther(cmd.getOther());
        if (cmd.getUnitPrice() != null) entity.setUnitPrice(cmd.getUnitPrice());
        if (cmd.getRate() != null) entity.setRate(cmd.getRate());
        if (cmd.getWarehouse() != null) entity.setWarehouse(cmd.getWarehouse());
        if (cmd.getFactoryAddr() != null) entity.setFactoryAddr(cmd.getFactoryAddr());
        if (cmd.getShowFlag() != null) entity.setShowFlag(cmd.getShowFlag());
        if (cmd.getRireki() != null) entity.setRireki(cmd.getRireki());
    }
}