package com.manpou.warehouse.common.config;

import org.springframework.core.env.Environment;

import java.util.Optional;

/**
 * Spring Environment 适配的 ConfigSource 实现。
 *
 * <p>委托 Spring 的 {@link Environment} 读取配置，继承其完整优先级链：</p>
 * <ol>
 *   <li>命令行参数（-D）</li>
 *   <li>环境变量（OS ENV）</li>
 *   <li>本地覆盖（.env.local / config.yaml）</li>
 *   <li>配置中心（Nacos / Consul）</li>
 *   <li>application.yml 默认值</li>
 * </ol>
 *
 * <p>此实现是 Phase A 的核心：所有其他 ConfigSource 实现最终都应委托给此实现，
 * 或者通过 {@link ConfigSourceFactory} 选择性地替代。</p>
 *
 * <p>watch() 不支持（返回空实现），因为 Properties 本身不支持热监听。</p>
 */
public class PropertiesConfigSource implements ConfigSource {

    private final Environment env;

    public PropertiesConfigSource(Environment env) {
        this.env = env;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(env.getProperty(key));
    }

    @Override
    public String get(String key, String defaultValue) {
        return env.getProperty(key, defaultValue);
    }

    @Override
    public String name() {
        return "properties";
    }
}
