package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ContainerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Data
public class ContainerCreateCmd {

    @NotBlank(message = "货柜号不能为空")
    @Length(max = 32)
    private String containerNo;

    @NotNull(message = "货柜类型不能为空")
    private ContainerType containerType;

    private Long poolId;

    private LocalDate loadDate;

    private LocalDate departureDate;

    private LocalDate arrivalDate;
}
