package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import com.manpou.allinone.logistics.domain.model.ContainerType;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Data
public class ContainerUpdateCmd {

    @Length(max = 32)
    private String containerNo;

    private ContainerType containerType;

    private ContainerStatus status;

    private LocalDate loadDate;

    private LocalDate departureDate;

    private LocalDate arrivalDate;

    // ===== v2.0 扩展字段（SPEC-B12）=====
    private Long shipId;                      // 关联船只（null=解除关联）

    @Size(max = 32)
    private String timeSlot;                  // 时间段

    @Size(max = 128)
    private String arrivalLocation;          // 到岗地点

    @Size(max = 512)
    private String remarks;                  // 备注
}
