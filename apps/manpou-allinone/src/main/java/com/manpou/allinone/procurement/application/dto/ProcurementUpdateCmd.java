package com.manpou.allinone.procurement.application.dto;

import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新发注单命令对象。
 * 所有字段可选，不传的字段不更新。
 * TODO Phase A: 替换为真实 ShippingOrder 字段。
 */
@Data
public class ProcurementUpdateCmd {

    @Size(max = 128, message = "名称最多 128 字符")
    private String name;

    private ShipmentStatus status;
}
