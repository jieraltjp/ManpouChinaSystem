package com.manpou.finance.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * ConfigSource Spring Boot 自动配置。
 *
 * <p>当项目中存在 {@code com.alibaba.cloud}（Nacos 依赖）时，
 * 此配置类自动注册 {@link ConfigSource} Bean。</p>
 *
 * <p>注入方式（推荐）：</p>
 * <pre>
 * {@code @Autowired ConfigSource configSource;}
 *
 * // 或在构造器中
 * {@code public MyService(ConfigSource configSource) { ... }}
 * </pre>
 *
 * <p>application.yml 配置：</p>
 * <pre>
 * app:
 *   config:
 *     source: properties   # properties(默认) / local / nacos
 *     local:
 *       path: config/local.yaml
 *     nacos:
 *       data-id: ${spring.application.name}
 *       group: DEFAULT_GROUP
 * </pre>
 *
 * <p>INTJ 设计原则：</p>
 * <ul>
 *   <li>@ConditionalOnMissingBean：用户可覆盖默认实现</li>
 *   <li>注入点可选：可通过构造器注入或 @Autowired，无需强制继承基类</li>
 *   <li>职责单一：此配置仅注册 ConfigSource Bean，不包含其他逻辑</li>
 * </ul>
 */
@Configuration
public class ConfigSourceAutoConfiguration {

    /**
     * 注册 ConfigSource Bean。
     *
     * <p>优先级：用户自定义 Bean &gt; 此处自动配置。
     * 如需覆盖，定义自己的 {@code @Bean ConfigSource} 即可。</p>
     */
    @Bean
    @ConditionalOnMissingBean(ConfigSource.class)
    public ConfigSource configSource(Environment env) {
        return ConfigSourceFactory.create(env);
    }
}
