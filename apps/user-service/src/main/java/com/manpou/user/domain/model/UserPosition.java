package com.manpou.user.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户职务关联实体（join 表）。
 */
@Entity
@Table(name = "user_position")
@Getter
@Setter
public class UserPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "position_id", nullable = false)
    private Long positionId;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
}