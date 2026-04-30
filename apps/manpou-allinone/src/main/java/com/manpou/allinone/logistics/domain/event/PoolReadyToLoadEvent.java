package com.manpou.allinone.logistics.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 拼柜池达到阈值，触发装柜事件（v1.5.0，SPEC-B00 Issue #8）。
 */
@Getter
public class PoolReadyToLoadEvent extends ApplicationEvent {

    private final Long poolId;
    private final String poolCode;
    private final String destinationPort;

    public PoolReadyToLoadEvent(Object source, Long poolId, String poolCode, String destinationPort) {
        super(source);
        this.poolId = poolId;
        this.poolCode = poolCode;
        this.destinationPort = destinationPort;
    }
}
