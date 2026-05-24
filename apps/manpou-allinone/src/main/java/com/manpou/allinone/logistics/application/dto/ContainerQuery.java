package com.manpou.allinone.logistics.application.dto;

import com.manpou.allinone.logistics.domain.model.ContainerStatus;
import lombok.Data;

@Data
public class ContainerQuery {

    private Integer page = 0;
    private Integer pageSize = 20;

    private ContainerStatus status;
    private String containerNo;
    private Long shipId;

    // ===== list7 筛选字段（SPEC-B14）=====

    // ===== list7 筛选字段（SPEC-B14）=====
    private Boolean showFlag;            // 显示标志（默认仅返回 showFlag=true）
    private String legacyStatus;        // 原始出运状态（未出 / 出完 / 待定）
    private String cabinetNo;           // 箱号模糊搜索
}
