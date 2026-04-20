package com.manpou.allinone;

import com.alibaba.cloud.nacos.NacosConfigAutoConfiguration;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * All-in-One 启动入口。
 * 合并 product, warehouse, customs, logistics, finance, notification 六个领域。
 */
@SpringBootApplication(
    exclude = {
        NacosConfigAutoConfiguration.class,
        NacosDiscoveryAutoConfiguration.class,
    },
    scanBasePackages = "com.manpou.allinone"
)
public class ManpouAllInOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManpouAllInOneApplication.class, args);
    }
}
