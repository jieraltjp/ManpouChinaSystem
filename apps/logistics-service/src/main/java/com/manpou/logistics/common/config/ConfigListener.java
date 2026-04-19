package com.manpou.logistics.common.config;

/**
 * 配置变更监听器。
 * 当 {@link ConfigSource} 检测到配置项变更时回调此接口。
 *
 * <p>用途：刷新缓存、重新加载策略、触发热更新等。</p>
 */
@FunctionalInterface
public interface ConfigListener {

    /**
     * 配置项变更通知。
     *
     * @param key   变更的配置键
     * @param value 变更后的新值（可能为 null 表示删除）
     */
    void onChange(String key, String value);
}
