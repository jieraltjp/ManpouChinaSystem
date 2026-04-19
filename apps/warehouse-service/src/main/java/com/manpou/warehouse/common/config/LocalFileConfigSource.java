package com.manpou.warehouse.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 本地文件配置的 ConfigSource 实现（用于开发 / CI / 测试）。
 *
 * <p>读取本地 YAML / Properties 文件，监听文件变更并回调监听器。</p>
 *
 * <p>典型用途：</p>
 * <ul>
 *   <li>本地开发时无需启动 Nacos</li>
 *   <li>CI 单元测试环境隔离</li>
 *   <li>多云部署时作为配置降级fallback</li>
 * </ul>
 *
 * <p>配置示例（application.yml）：</p>
 * <pre>
 * app:
 *   config:
 *     source: local
 *     local:
 *       path: classpath:config/local.yaml
 * </pre>
 *
 * <p>INTJ 设计原则：</p>
 * <ul>
 *   <li>watch() 使用文件轮询（5 秒间隔），避免引入 Nacos 依赖</li>
 *   <li>变更检测基于文件内容 MD5，避免误报</li>
 *   <li>启动时一次性加载，变更时全量刷新</li>
 * </ul>
 */
public class LocalFileConfigSource implements ConfigSource {

    private static final Logger log = LoggerFactory.getLogger(LocalFileConfigSource.class);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(5);

    private final Path configPath;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile byte[] lastContentHash = new byte[0];

    private final Map<String, ConfigListener> listeners = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "config-file-watcher")
    );

    public LocalFileConfigSource(String configPath) {
        this.configPath = Path.of(configPath);
        load();
        startWatcher();
    }

    /** 从 classpath 资源加载（推荐构造方式）。 */
    public static LocalFileConfigSource fromClasspath(String resourcePath) {
        try {
            var url = LocalFileConfigSource.class.getClassLoader().getResource(resourcePath);
            if (url == null) {
                log.warn("[LocalFileConfigSource] 资源未找到: {}, 使用空配置", resourcePath);
                return new LocalFileConfigSource(_nullDevice());
            }
            return new LocalFileConfigSource(Path.of(url.toURI()).toString());
        } catch (Exception e) {
            log.warn("[LocalFileConfigSource] 加载 classpath 资源失败: {}, 使用空配置", resourcePath, e);
            return new LocalFileConfigSource(_nullDevice());
        }
    }

    /** 跨平台空设备路径（Unix=/dev/null，Windows=NUL）。 */
    private static String _nullDevice() {
        return System.getProperty("os.name", "").startsWith("Windows") ? "NUL" : "/dev/null";
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public String get(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    @Override
    public void watch(String key, ConfigListener listener) {
        listeners.put(key, listener);
    }

    @Override
    public String name() {
        return "local-file";
    }

    /** 一次性加载文件内容到缓存。 */
    private void load() {
        if (!Files.exists(configPath)) {
            log.info("[LocalFileConfigSource] 配置文件不存在: {}, 将使用空配置", configPath);
            return;
        }
        try {
            String content = Files.readString(configPath);
            lastContentHash = md5(content);
            Map<String, String> loaded = parseContent(content);
            cache.clear();
            cache.putAll(loaded);
            log.info("[LocalFileConfigSource] 加载配置文件: {}, 共 {} 项", configPath, cache.size());
        } catch (IOException e) {
            log.warn("[LocalFileConfigSource] 读取配置文件失败: {}", configPath, e);
        }
    }

    /** 启动文件轮询监听器。 */
    private void startWatcher() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!Files.exists(configPath)) {
                    return;
                }
                String content = Files.readString(configPath);
                byte[] hash = md5(content);
                if (!java.util.Arrays.equals(hash, lastContentHash)) {
                    lastContentHash = hash;
                    Map<String, String> oldCache = Map.copyOf(cache);
                    load();
                    notifyListeners(oldCache);
                }
            } catch (IOException e) {
                log.debug("[LocalFileConfigSource] 文件轮询读取失败: {}", configPath);
            }
        }, POLL_INTERVAL.toSeconds(), POLL_INTERVAL.toSeconds(), TimeUnit.SECONDS);
    }

    private void notifyListeners(Map<String, String> oldCache) {
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            String key = entry.getKey();
            String newValue = entry.getValue();
            String oldValue = oldCache.get(key);
            if (!java.util.Objects.equals(oldValue, newValue)) {
                ConfigListener listener = listeners.get(key);
                if (listener != null) {
                    log.info("[LocalFileConfigSource] 配置变更: {} = {}", key, newValue);
                    listener.onChange(key, newValue);
                }
            }
        }
    }

    private static Map<String, String> parseContent(String content) {
        // 简单 key=value 解析（支持 # 注释和空行）
        // 生产环境建议替换为 SnakeYAML 解析
        Map<String, String> result = new ConcurrentHashMap<>();
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq > 0) {
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                result.put(k, v);
            }
        }
        return result;
    }

    private static byte[] md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            return md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new byte[0];
        }
    }

    /** 关闭时调用，释放线程池。 */
    public void shutdown() {
        scheduler.shutdown();
    }
}
