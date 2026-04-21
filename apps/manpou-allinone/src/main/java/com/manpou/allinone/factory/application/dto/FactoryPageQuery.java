package com.manpou.allinone.factory.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manpou.allinone.factory.domain.model.FactoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FactoryPageQuery {

    private Long id;
    private String factoryCode;
    private String factoryName;
    private String location;
    private String roughLocation;
    private String contactName;
    private String contactPhone;
    private FactoryStatus status;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
