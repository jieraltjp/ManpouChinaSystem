package com.manpou.allinone.procurement.application.dto;

import com.manpou.allinone.procurement.domain.model.ShipmentStatus;
import lombok.Data;

/**
 * 发注单分页查询请求参数。
 * 与 docs/business/API-发注管理.md §1.2 Query 参数对齐。
 */
@Data
public class ProcurementQuery {

    /** 当前页（从 0 开始，与前端 0-indexed 兼容） */
    private Integer page = 0;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** 状态筛选 */
    private ShipmentStatus status;

    /** 商品代码筛选 */
    private String productCode;

    /** 客户公司筛选 */
    private String customerCompany;
}
