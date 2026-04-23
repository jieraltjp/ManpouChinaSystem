package com.manpou.allinone.customs.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 日本清关完成事件。
 * 触发：JapanCustomsRecord.status = CLEARED
 * 副作用：自动创建 SalesRecord（步骤8）
 */
@Getter
public class JapanCustomsClearedEvent extends ApplicationEvent {

    private final Long japanCustomsId;
    private final Long procurementId;

    public JapanCustomsClearedEvent(Object source, Long japanCustomsId, Long procurementId) {
        super(source);
        this.japanCustomsId = japanCustomsId;
        this.procurementId = procurementId;
    }
}
