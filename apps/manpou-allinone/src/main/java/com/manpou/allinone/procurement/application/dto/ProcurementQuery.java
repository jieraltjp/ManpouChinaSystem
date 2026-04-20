package com.manpou.allinone.procurement.application.dto;

import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import lombok.Data;

/**
 * 发注单分页查询请求参数。
 * TODO Phase A: 替换为真实 ShippingOrder 筛选字段。
 */
@Data
public class ProcurementQuery {

    /** 当前页（从 1 开始） */
    private Integer page = 1;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** 状态筛选 */
    private ShipmentStatus status;

    /** 商品代码筛选 */
    private String productCode;

    /** 客户公司筛选 */
    private String customerCompany;
}
