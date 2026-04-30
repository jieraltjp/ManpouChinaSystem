package com.manpou.allinone.logistics.domain.model;

/**
 * 货柜类型枚举。
 */
public enum ContainerType {
    GP20("20GP", "20尺普柜", "33.2"),
    GP40("40GP", "40尺普柜", "67.7"),
    HC40("40HC", "40尺高柜", "76.4"),
    HC45("45HC", "45尺高柜", "86.0");

    private final String code;
    private final String label;
    private final String maxVolumeCbm;

    ContainerType(String code, String label, String maxVolumeCbm) {
        this.code = code;
        this.label = label;
        this.maxVolumeCbm = maxVolumeCbm;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getMaxVolumeCbm() { return maxVolumeCbm; }
}
