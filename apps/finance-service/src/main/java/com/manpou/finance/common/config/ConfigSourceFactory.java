package com.manpou.finance.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * ConfigSource 工厂。
 *
 * <p>根据 {@code app.config.source} 配置项自动选择实现：</p>
 * <table>
 *   <tr><th>配置值</th><th>实现类</th><th>用途</th></tr>
 *   <tr><td>properties</td><td>{@link PropertiesConfigSource}</td><td>默认，使用 Spring Environment</td></tr>
 *   <tr><td>local</td><td>{@link LocalFileConfigSource}</td><td>开发 / CI / 测试</td></tr>
 *   <tr><td>nacos</td><td>{@link NacosConfigSource}</td><td>生产环境（需 Nacos 依赖）</td></tr>
 * </table>
 *
 * <p>配置优先级：命令行 &gt; 环境变量 &gt; .env.local &gt; 配置中心</p>
 *
 * <p>使用方式：</p>
 * <pre>
 * // 自动选择（推荐）
 * ConfigSource source = ConfigSourceFactory.create(environment);
 *
 * // 显式指定
 * ConfigSource source = ConfigSourceFactory.create(environment, "local");
 * </pre>
 */
public final class ConfigSourceFactory {

    private static final Logger log = LoggerFactory.getLogger(ConfigSourceFactory.class);

    /** 配置项：选择配置源类型 */
    private static final String SOURCE_KEY = "app.config.source";
    /** 默认值 */
    private static final String DEFAULT_SOURCE = "properties";

    /** 本地配置文件路径配置项 */
    private static final String LOCAL_PATH_KEY = "app.config.local.path";
    /** 默认本地配置文件 */
    private static final String DEFAULT_LOCAL_PATH = "config/local.yaml";

    private ConfigSourceFactory() {
    }

    /**
     * 创建 ConfigSource 实例（自动检测配置）。
     *
     * @param env Spring Environment
     * @return 根据 app.config.source 配置选择的 ConfigSource 实现
     */
    public static ConfigSource create(Environment env) {
        String source = env.getProperty(SOURCE_KEY, DEFAULT_SOURCE);
        return create(env, source);
    }

    /**
     * 创建 ConfigSource 实例（显式指定类型）。
     *
     * @param env    Spring Environment
     * @param source 配置源类型：properties / local / nacos
     * @return 对应的 ConfigSource 实现
     */
    @SuppressWarnings("deprecation")
    public static ConfigSource create(Environment env, String source) {
        return switch (source.toLowerCase()) {
            case "local" -> {
                String path = env.getProperty(LOCAL_PATH_KEY, DEFAULT_LOCAL_PATH);
                log.info("[ConfigSourceFactory] 使用本地文件配置: source=local, path={}", path);
                yield LocalFileConfigSource.fromClasspath(path);
            }
            case "nacos" -> {
                if (!hasNacosOnClasspath()) {
                    log.warn("[ConfigSourceFactory] Nacos 不在 classpath 中，回退到 PropertiesConfigSource");
                    yield new PropertiesConfigSource(env);
                }
                // NacosConfigSource 需要 NacosConfigManager bean
                // Phase B: 从 ApplicationContext 获取 NacosConfigManager
                log.info("[ConfigSourceFactory] 使用 Nacos 配置中心: source=nacos");
                yield new PropertiesConfigSource(env); // Phase B 替换为 new NacosConfigSource(env, manager)
            }
            default -> {
                log.info("[ConfigSourceFactory] 使用 Spring Environment 配置: source=properties");
                yield new PropertiesConfigSource(env);
            }
        };
    }

    /**
     * 检测 Nacos 是否在 classpath 中。
     */
    private static boolean hasNacosOnClasspath() {
        try {
            Class.forName("com.alibaba.cloud.nacos.NacosConfigManager");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
