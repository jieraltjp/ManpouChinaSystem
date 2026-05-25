package com.manpou.allinone.legacyimportlist8.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "legacy_import_list8")
@Access(AccessType.FIELD)
@Getter
@Setter
public class LegacyImportList8 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "code", columnDefinition = "text")
    private String code;

    @Column(name = "manager", columnDefinition = "text")
    private String manager;

    @Column(name = "destination", columnDefinition = "text")
    private String destination;

    @Column(name = "tax", columnDefinition = "text")
    private String tax;

    @Column(name = "material", columnDefinition = "text")
    private String material;

    @Column(name = "kensa")
    private String kensa;

    @Column(name = "num")
    private Integer num;

    @Column(name = "pieces")
    private Integer pieces;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "weight2")
    private Double weight2;

    @Column(name = "length")
    private Double length;

    @Column(name = "location", columnDefinition = "text")
    private String location;

    @Column(name = "date1")
    private LocalDate date1;

    @Column(name = "status", columnDefinition = "text")
    private String status;

    @Column(name = "other", columnDefinition = "text")
    private String other;

    @Column(name = "unit_ch")
    private Double unitCh;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "souko", columnDefinition = "text")
    private String souko;

    @Column(name = "factory_addr")
    private String factoryAddr;

    @Column(name = "updatetime")
    private LocalDateTime updatetime;

    @Column(name = "updateuser", columnDefinition = "text")
    private String updateuser;

    @Column(name = "showFlag")
    private Integer showFlag;

    @Column(name = "rireki", columnDefinition = "longtext")
    private String rireki;
}
