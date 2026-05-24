package com.manpou.allinone.offlineorder.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 线下订单实体，映射遗留表 list6。
 */
@Entity
@Table(name = "list6")
@Access(AccessType.FIELD)
@Getter
@Setter
public class OfflineOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "showFlag")
    private Integer showFlag;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "sub_code", length = 20)
    private String subCode;

    @Column(name = "houkoku", length = 50)
    private String houkoku;

    @Column(name = "info_file", length = 10)
    private String infoFile;

    @Column(name = "item_name", length = 50)
    private String itemName;

    @Column(name = "volume_count")
    private Integer volumeCount;

    @Column(name = "order_count")
    private Integer orderCount;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "arrival", length = 10)
    private String arrival;

    @Column(name = "unit_ch")
    private Double unitCh;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "souko", length = 10)
    private String souko;

    @Column(name = "factory", length = 50)
    private String factory;

    @Column(name = "contactor", length = 20)
    private String contactor;

    @Column(name = "contactor_tel", length = 30)
    private String contactorTel;

    @Column(name = "principal", length = 30)
    private String principal;

    @Column(name = "memo", columnDefinition = "text")
    private String memo;

    @Column(name = "link", columnDefinition = "text")
    private String link;

    @Column(name = "updater", length = 20)
    private String updater;

    @Column(name = "updatetime")
    private LocalDateTime updatetime;

    @Column(name = "inventory_note", precision = 10, scale = 0)
    private BigDecimal inventoryNote;

    @Column(name = "rireki", columnDefinition = "longtext")
    private String rireki;
}