package com.manpou.allinone.legacyprocurement.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegacyProcurementStatsDTO {

    private long total;
    private long withContainer;
    private long withImg;
}