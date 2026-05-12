package com.manpou.allinone.logistics.application.dto.ship;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShipUpdateCmd {

    @NotBlank(message = "船名不能为空")
    @Size(max = 64, message = "船名最大64字符")
    private String shipName;

    @Size(max = 64, message = "船公司最大64字符")
    private String carrier;

    @Size(max = 64, message = "出发港最大64字符")
    private String departurePort;

    @Size(max = 64, message = "目的港最大64字符")
    private String arrivalPort;
}
