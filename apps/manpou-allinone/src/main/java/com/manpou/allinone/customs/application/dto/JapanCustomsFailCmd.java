package com.manpou.allinone.customs.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JapanCustomsFailCmd {

    @NotBlank
    private String reason;
}
