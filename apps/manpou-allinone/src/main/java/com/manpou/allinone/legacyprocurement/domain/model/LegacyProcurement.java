package com.manpou.allinone.legacyprocurement.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "legacy_import_list1")
@Access(AccessType.FIELD)
@Getter
@Setter
public class LegacyProcurement {

    @Id
    @Column(name = "ID")
    private Integer legacyId;

    @Column(name = "lockuser-id")
    private Integer lockuserId;

    @Column(name = "lockuser", columnDefinition = "text")
    private String lockuser;

    @Column(name = "locktime")
    private LocalDateTime locktime;

    @Column(name = "updater-id")
    private Integer updaterId;

    @Column(name = "updater", columnDefinition = "text")
    private String updater;

    @Column(name = "updatetime")
    private LocalDateTime updatetime;

    @Column(name = "code", columnDefinition = "text")
    private String code;

    @Column(name = "sub-code", columnDefinition = "text")
    private String subCode;

    @Column(name = "img", columnDefinition = "text")
    private String img;

    @Column(name = "item-name", columnDefinition = "text")
    private String itemName;

    @Column(name = "order-group", columnDefinition = "text")
    private String orderGroup;

    @Column(name = "order-count")
    private Integer orderCount;

    @Column(name = "inspect-count")
    private Integer inspectCount;

    @Column(name = "yoyaku-hasoubi")
    private LocalDate yoyakuHasoubi;

    @Column(name = "arrival-depo", columnDefinition = "text")
    private String arrivalDepo;

    @Column(name = "departure")
    private LocalDate departure;

    @Column(name = "arrival")
    private LocalDate arrival;

    @Column(name = "arrival-jikan")
    private Integer arrivalJikan;

    @Column(name = "arrival-flag")
    private Integer arrivalFlag;

    @Column(name = "unit-ch")
    private Double unitCh;

    @Column(name = "total-ch")
    private Double totalCh;

    @Column(name = "unit-jp")
    private Double unitJp;

    @Column(name = "total-jp")
    private Integer totalJp;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "fba-stock")
    private Integer fbaStock;

    @Column(name = "houkoku", length = 50)
    private String houkoku;

    @Column(name = "kaitsuke", precision = 10, scale = 2)
    private BigDecimal kaitsuke;

    @Column(name = "hyoten", precision = 5, scale = 4)
    private BigDecimal hyoten;

    @Column(name = "kanpu", length = 10)
    private String kanpu;

    @Column(name = "ne-stock", columnDefinition = "text")
    private String neStock;

    @Column(name = "container", columnDefinition = "text")
    private String container;

    @Column(name = "box-num", columnDefinition = "text")
    private String boxNum;

    @Column(name = "box-count")
    private Integer boxCount;

    @Column(name = "kg")
    private Double kg;

    @Column(name = "one-m3")
    private Double oneM3;

    @Column(name = "all-m3")
    private Double allM3;

    @Column(name = "material", columnDefinition = "text")
    private String material;

    @Column(name = "material-ch", columnDefinition = "text")
    private String materialCh;

    @Column(name = "height")
    private Double height;

    @Column(name = "width")
    private Double width;

    @Column(name = "depth")
    private Double depth;

    @Column(name = "info-file1", columnDefinition = "text")
    private String infoFile1;

    @Column(name = "info-file2", columnDefinition = "text")
    private String infoFile2;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "receive", columnDefinition = "text")
    private String receive;

    @Column(name = "is_deleted", columnDefinition = "TINYINT(1)")
    private Boolean deleted = false;

    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.deleted);
    }

    public void markDeleted() {
        this.deleted = true;
    }
}