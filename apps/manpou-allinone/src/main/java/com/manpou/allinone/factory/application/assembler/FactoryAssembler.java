package com.manpou.allinone.factory.application.assembler;

import com.manpou.allinone.factory.application.dto.FactoryCreateCmd;
import com.manpou.allinone.factory.application.dto.FactoryPageQuery;
import com.manpou.allinone.factory.application.dto.FactoryUpdateCmd;
import com.manpou.allinone.factory.domain.model.CooperationStatus;
import com.manpou.allinone.factory.domain.model.Factory;
import com.manpou.allinone.factory.domain.model.FactoryCategory;
import com.manpou.allinone.factory.domain.model.PaymentTerms;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FactoryAssembler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1000);

    public String generateFactoryCode() {
        String date = LocalDate.now().format(DATE_FMT);
        return String.format("F-%s-%03d", date, SEQ.incrementAndGet() % 1000);
    }

    public FactoryPageQuery toDto(Factory entity) {
        FactoryPageQuery dto = FactoryPageQuery.builder()
                .id(entity.getId())
                .factoryCode(entity.getFactoryCode())
                .factoryName(entity.getFactoryName())
                .category(entity.getCategory())
                .province(entity.getProvince())
                .city(entity.getCity())
                .county(entity.getCounty())
                .roughLocation(entity.getRoughLocation())
                .longitude(entity.getLongitude())
                .latitude(entity.getLatitude())
                .contactName(entity.getContactName())
                .contactPhone(entity.getContactPhone())
                .contactWechat(entity.getContactWechat())
                .contactQq(entity.getContactQq())
                .cooperationStatus(entity.getCooperationStatus())
                .needsQc(entity.getNeedsQc())
                .paymentTerms(entity.getPaymentTerms())
                .notes(entity.getNotes())
                .createBy(entity.getCreateBy())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
        return dto;
    }

    public Factory toEntity(FactoryCreateCmd cmd) {
        Factory entity = new Factory();
        entity.setFactoryCode(generateFactoryCode());
        copyToEntity(cmd, entity);
        return entity;
    }

    public void copyToEntity(FactoryCreateCmd cmd, Factory entity) {
        if (cmd.getFactoryName() != null) entity.setFactoryName(cmd.getFactoryName());
        if (cmd.getCategory() != null) entity.setCategory(cmd.getCategory());
        else entity.setCategory(FactoryCategory.OTHER);
        if (cmd.getProvince() != null) entity.setProvince(cmd.getProvince());
        if (cmd.getCity() != null) entity.setCity(cmd.getCity());
        if (cmd.getCounty() != null) entity.setCounty(cmd.getCounty());
        if (cmd.getRoughLocation() != null) entity.setRoughLocation(cmd.getRoughLocation());
        entity.setLongitude(cmd.getLongitude());
        entity.setLatitude(cmd.getLatitude());
        if (cmd.getContactName() != null) entity.setContactName(cmd.getContactName());
        if (cmd.getContactPhone() != null) entity.setContactPhone(cmd.getContactPhone());
        if (cmd.getContactWechat() != null) entity.setContactWechat(cmd.getContactWechat());
        if (cmd.getContactQq() != null) entity.setContactQq(cmd.getContactQq());
        if (cmd.getCooperationStatus() != null) entity.setCooperationStatus(cmd.getCooperationStatus());
        else entity.setCooperationStatus(CooperationStatus.POTENTIAL);
        if (cmd.getNeedsQc() != null) entity.setNeedsQc(cmd.getNeedsQc());
        else entity.setNeedsQc(true);
        if (cmd.getPaymentTerms() != null) entity.setPaymentTerms(cmd.getPaymentTerms());
        else entity.setPaymentTerms(PaymentTerms.NET_30);
        if (cmd.getNotes() != null) entity.setNotes(cmd.getNotes());
    }

    public void copyToEntity(FactoryUpdateCmd cmd, Factory entity) {
        if (cmd.getFactoryName() != null) entity.setFactoryName(cmd.getFactoryName());
        if (cmd.getCategory() != null) entity.setCategory(cmd.getCategory());
        if (cmd.getProvince() != null) entity.setProvince(cmd.getProvince());
        if (cmd.getCity() != null) entity.setCity(cmd.getCity());
        if (cmd.getCounty() != null) entity.setCounty(cmd.getCounty());
        if (cmd.getRoughLocation() != null) entity.setRoughLocation(cmd.getRoughLocation());
        entity.setLongitude(cmd.getLongitude());
        entity.setLatitude(cmd.getLatitude());
        if (cmd.getContactName() != null) entity.setContactName(cmd.getContactName());
        if (cmd.getContactPhone() != null) entity.setContactPhone(cmd.getContactPhone());
        if (cmd.getContactWechat() != null) entity.setContactWechat(cmd.getContactWechat());
        if (cmd.getContactQq() != null) entity.setContactQq(cmd.getContactQq());
        if (cmd.getCooperationStatus() != null) entity.setCooperationStatus(cmd.getCooperationStatus());
        if (cmd.getNeedsQc() != null) entity.setNeedsQc(cmd.getNeedsQc());
        if (cmd.getPaymentTerms() != null) entity.setPaymentTerms(cmd.getPaymentTerms());
        if (cmd.getNotes() != null) entity.setNotes(cmd.getNotes());
    }
}
