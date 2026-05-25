package com.manpou.allinone.legacyimportlist8.application.assembler;

import com.manpou.allinone.legacyimportlist8.application.dto.LegacyImportList8UpdateCmd;
import com.manpou.allinone.legacyimportlist8.application.dto.LegacyImportList8VO;
import com.manpou.allinone.legacyimportlist8.domain.model.LegacyImportList8;
import org.springframework.stereotype.Component;

@Component
public class LegacyImportList8Assembler {

    public LegacyImportList8VO toDto(LegacyImportList8 entity) {
        return LegacyImportList8VO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .manager(entity.getManager())
                .destination(entity.getDestination())
                .tax(entity.getTax())
                .material(entity.getMaterial())
                .kensa(entity.getKensa())
                .num(entity.getNum())
                .pieces(entity.getPieces())
                .weight(entity.getWeight())
                .weight2(entity.getWeight2())
                .length(entity.getLength())
                .location(entity.getLocation())
                .date1(entity.getDate1())
                .status(entity.getStatus())
                .other(entity.getOther())
                .unitCh(entity.getUnitCh())
                .rate(entity.getRate())
                .souko(entity.getSouko())
                .factoryAddr(entity.getFactoryAddr())
                .updatetime(entity.getUpdatetime())
                .updateuser(entity.getUpdateuser())
                .showFlag(entity.getShowFlag())
                .rireki(entity.getRireki())
                .build();
    }

    public void applyUpdate(LegacyImportList8 entity, LegacyImportList8UpdateCmd cmd) {
        if (cmd.getCode() != null) entity.setCode(cmd.getCode());
        if (cmd.getManager() != null) entity.setManager(cmd.getManager());
        if (cmd.getDestination() != null) entity.setDestination(cmd.getDestination());
        if (cmd.getTax() != null) entity.setTax(cmd.getTax());
        if (cmd.getMaterial() != null) entity.setMaterial(cmd.getMaterial());
        if (cmd.getKensa() != null) entity.setKensa(cmd.getKensa());
        if (cmd.getNum() != null) entity.setNum(cmd.getNum());
        if (cmd.getPieces() != null) entity.setPieces(cmd.getPieces());
        if (cmd.getWeight() != null) entity.setWeight(cmd.getWeight());
        if (cmd.getWeight2() != null) entity.setWeight2(cmd.getWeight2());
        if (cmd.getLength() != null) entity.setLength(cmd.getLength());
        if (cmd.getLocation() != null) entity.setLocation(cmd.getLocation());
        if (cmd.getDate1() != null) entity.setDate1(cmd.getDate1());
        if (cmd.getStatus() != null) entity.setStatus(cmd.getStatus());
        if (cmd.getOther() != null) entity.setOther(cmd.getOther());
        if (cmd.getUnitCh() != null) entity.setUnitCh(cmd.getUnitCh());
        if (cmd.getRate() != null) entity.setRate(cmd.getRate());
        if (cmd.getSouko() != null) entity.setSouko(cmd.getSouko());
        if (cmd.getFactoryAddr() != null) entity.setFactoryAddr(cmd.getFactoryAddr());
        if (cmd.getShowFlag() != null) entity.setShowFlag(cmd.getShowFlag());
        if (cmd.getRireki() != null) entity.setRireki(cmd.getRireki());
    }
}
