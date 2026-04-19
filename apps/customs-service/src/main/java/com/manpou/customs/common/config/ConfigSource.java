package com.manpou.customs.common.config;

import java.util.Optional;

/**
 * 配置读取统一接口。
 *
 * <p>配置优先级（由高到低）：</p>
 * <ol>
 *   <li>命令行参数（-Dkey=value）</li>
 *   <li>环境变量（OS ENV）</li>
 *   <li>本地覆盖文件（.env.local）</li>
 *   <li>配置中心（Nacos / Consul / etcd）</li>
 *   <li>应用默认值（@Value default）</li>
 * </ol>
 *
 * <p>实现可替换：Nacos / Consul / etcd / Apollo / 本地文件。</p>
 *
 * <p>INTJ 设计原则：</p>
 * <ul>
 *   <li>get() 返回 {@code Optional<String>}，强制调用方处理不存在的情况</li>
 *   <li>无默认值参数的方法要求调用方显式决策（无隐式降级）</li>
 *   <li>watch() 是可选操作，不支持 watch 的实现应为空实现</li>
 * </ul>
 */
public interface ConfigSource {

    /**
     * 获取配置值。
     *
     * @param key 配置键
     * @return 存在则返回值，否则返回空 Optional
     */
    Optional<String> get(String key);

    /**
     * 获取配置值（带默认值）。
     *
     * @param key          配置键
     * @param defaultValue 当 key 不存在时返回此值
     * @return 配置值或默认值
     */
    String get(String key, String defaultValue);

    /**
     * 监听配置变更（可选操作）。
     *
     * <p>实现注意事项：</p>
     * <ul>
     *   <li>Nacos 实现：使用 Nacos Listener API</li>
     *   <li>本地文件实现：使用文件系统轮询（如 5 秒间隔）</li>
     *   <li>Properties 实现：{@code default void} 空实现</li>
     * </ul>
     *
     * @param key      配置键
     * @param listener 变更回调
     */
    default void watch(String key, ConfigListener listener) {
        // 空实现：子类可选择性覆盖
    }

    /**
     * 配置源名称（用于日志和调试）。
     *
     * @return 实现名称，如 "nacos"、"local"、"properties"
     */
    String name();
}
