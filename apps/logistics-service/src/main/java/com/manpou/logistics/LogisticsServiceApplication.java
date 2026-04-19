package com.manpou.logistics;

import com.alibaba.cloud.nacos.NacosConfigAutoConfiguration;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务启动入口。
 */
@SpringBootApplication(
    exclude = {
        NacosConfigAutoConfiguration.class,
        NacosDiscoveryAutoConfiguration.class,
    }
)
public class LogisticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsServiceApplication.class, args);
    }
}
