package com.manpou.customs.common.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Nacos 配置中心的 ConfigSource 实现。
 *
 * <p>集成 Spring Cloud Alibaba Nacos Config，委托 {@link NacosConfigManager} 读取和监听配置变更。</p>
 *
 * <p>功能：</p>
 * <ul>
 *   <li>从 Nacos Server 读取配置（dataId = {spring.application.name}）</li>
 *   <li>支持配置变更监听（watch()），变更时回调 {@link ConfigListener}</li>
 *   <li>回退到 Spring Environment（当 Nacos 不可用时）</li>
 * </ul>
 *
 * <p>配置项（application.yml）：</p>
 * <pre>
 * app:
 *   config:
 *     source: nacos
 *     nacos:
 *       data-id: ${spring.application.name}   # 默认使用应用名
 *       group: ${SERVICE_GROUP:DEFAULT_GROUP}
 * </pre>
 *
 * <p>INTJ 设计原则：</p>
 * <ul>
 *   <li>无 Nacos 依赖时编译失败——此实现仅在 {@code com.alibaba.cloud:nacos-config} 存在时使用</li>
 *   <li>watch() 使用 Nacos 原生 Listener API，非轮询</li>
 *   <li>本地缓存：避免每次 get() 都访问 Nacos Server</li>
 * </ul>
 *
 * <p>TODO（Phase B）：</p>
 * <ul>
 *   <li>接入 NacosConfigManager#getConfigService()</li>
 *   <li>实现 getConfigAndSignListener() 监听变更</li>
 *   <li>单元测试（mock Nacos Server）</li>
 * </ul>
 */
public class NacosConfigSource implements ConfigSource {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigSource.class);

    private final Environment env;
    private final NacosConfigManager nacosManager;

    private final ConcurrentHashMap<String, String> localCache = new ConcurrentHashMap<>();

    public NacosConfigSource(Environment env, NacosConfigManager nacosManager) {
        this.env = env;
        this.nacosManager = nacosManager;
    }

    @Override
    public Optional<String> get(String key) {
        // 优先读本地缓存
        String cached = localCache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        // 回退到 Spring Environment（含 Nacos 注入的配置）
        return Optional.ofNullable(env.getProperty(key));
    }

    @Override
    public String get(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    @Override
    public void watch(String key, ConfigListener listener) {
        String dataId = env.getProperty("app.config.nacos.data-id",
                env.getProperty("spring.application.name", "application"));
        String group = env.getProperty("app.config.nacos.group", "DEFAULT_GROUP");

        try {
            nacosManager.getConfigService().addListener(
                    dataId, group,
                    new Listener() {
                        @Override
                        public Executor getExecutor() {
                            return null;
                        }

                        @Override
                        public void receiveConfigInfo(String configInfo) {
                            // 解析 configInfo（JSON/YAML）并匹配 key
                            // TODO: 实现配置解析和 key 匹配
                            log.info("[NacosConfigSource] 收到配置变更: dataId={}, group={}", dataId, group);
                            listener.onChange(key, extractValue(configInfo, key));
                        }
                    }
            );
            log.info("[NacosConfigSource] 已注册监听: key={}, dataId={}, group={}", key, dataId, group);
        } catch (Exception e) {
            log.warn("[NacosConfigSource] 注册 Nacos 监听失败: key={}", key, e);
        }
    }

    @Override
    public String name() {
        return "nacos";
    }

    /**
     * 从配置内容（JSON/YAML）中提取指定 key 的值。
     * TODO: 替换为 SnakeYAML 或 Jackson 解析
     */
    private String extractValue(String content, String key) {
        // 临时实现：简单 key=value 行解析
        if (content == null) return null;
        for (String line : content.split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0 && line.substring(0, eq).trim().equals(key)) {
                return line.substring(eq + 1).trim();
            }
        }
        return null;
    }
}
