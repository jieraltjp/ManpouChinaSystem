package com.manpou.allinone.product.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * item_size 旧系统数据只读实体，仅用于导入时读取数据源。
 */
@Entity
@Table(name = "item_size")
@Getter
@Setter
public class ItemSize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "height", precision = 6, scale = 2)
    private BigDecimal height;

    @Column(name = "width", precision = 6, scale = 2)
    private BigDecimal width;

    @Column(name = "depth", precision = 6, scale = 2)
    private BigDecimal depth;

    @Column(name = "size_total", precision = 8, scale = 2)
    private BigDecimal sizeTotal;

    @Column(name = "weight", precision = 6, scale = 2)
    private BigDecimal weight;

    @Column(name = "pack_qty")
    private Integer packQty;

    @Column(name = "pack_height", precision = 6, scale = 2)
    private BigDecimal packHeight;

    @Column(name = "pack_width", precision = 6, scale = 2)
    private BigDecimal packWidth;

    @Column(name = "pack_depth", precision = 6, scale = 2)
    private BigDecimal packDepth;

    @Column(name = "pack_size_total", precision = 8, scale = 2)
    private BigDecimal packSizeTotal;

    @Column(name = "pack_weight_total", precision = 8, scale = 2)
    private BigDecimal packWeightTotal;

    @Column(name = "input_user", length = 50)
    private String inputUser;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "showFlag")
    private Integer showFlag;

    @Column(name = "other", columnDefinition = "TEXT")
    private String other;

    @Column(name = "rireki", columnDefinition = "TEXT")
    private String rireki;
}