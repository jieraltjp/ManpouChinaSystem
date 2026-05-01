package com.manpou.allinone.common.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "cos")
@Getter
@Setter
public class CosConfig {

    private boolean enabled = false;
    private String secretId = "";
    private String secretKey = "";
    private String bucket = "manpou-1324246219";
    private String region = "ap-tokyo";
    private String domain = "https://manpou-1324246219.cos.ap-tokyo.myqcloud.com";
    private String prefix = "qc-images/";
    private long maxFileSize = 5 * 1024 * 1024; // 5MB

    public String getBucketHost() {
        return bucket + ".cos." + region + ".myqcloud.com";
    }

    /**
     * 生成带日期路径前缀的 COS 对象 key。
     * 例如：qc-images/2026/05/01/uuid-filename.jpg
     */
    public String generateKey(String originalFilename) {
        java.time.LocalDate now = java.time.LocalDate.now();
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        String ext = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            ext = originalFilename.substring(dotIdx);
        }
        return String.format("%s%d/%02d/%02d/%s%s",
                prefix, now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                uuid, ext);
    }
}
