package com.manpou.allinone.finance.application.assembler;

import com.manpou.allinone.finance.application.dto.FinanceCreateCmd;
import com.manpou.allinone.finance.application.dto.FinancePageQuery;
import com.manpou.allinone.finance.application.dto.FinanceUpdateCmd;
import com.manpou.allinone.finance.domain.model.FinanceExample;
import org.springframework.stereotype.Component;

/**
 * DTO ↔ Entity 转换器。
 */
@Component
public class FinanceAssembler {

    public FinancePageQuery toDto(FinanceExample entity) {
        return FinancePageQuery.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    public FinanceExample toEntity(FinanceCreateCmd cmd) {
        FinanceExample entity = new FinanceExample();
        entity.rename(cmd.getName());
        return entity;
    }

    public void copyToEntity(FinanceUpdateCmd cmd, FinanceExample entity) {
        if (cmd.getName() != null) entity.rename(cmd.getName());
        if (cmd.getStatus() != null) entity.updateStatus(cmd.getStatus());
    }
}
