package com.manpou.allinone.factory.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryStatsDTO {

    private long total;
    private long active;
    private long potential;
    private long suspended;
    private long eliminated;
}
