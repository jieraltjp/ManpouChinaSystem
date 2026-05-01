package com.manpou.allinone.common.service;

import com.manpou.allinone.common.config.CosConfig;
import com.manpou.allinone.common.exception.BusinessException;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 腾讯云 COS 对象存储服务（使用官方 COS XML SDK）。
 *
 * SDK 自动处理签名，无需手写 HMAC-SHA1。
 * 参考：https://cloud.tencent.com/document/product/436/655
 */
@Slf4j
@Service
public class CosService {

    private final CosConfig cosConfig;
    private volatile COSClient cosClient;

    @Autowired
    public CosService(CosConfig cosConfig) {
        this.cosConfig = cosConfig;
    }

    private COSClient getClient() {
        if (cosClient != null) {
            return cosClient;
        }
        synchronized (this) {
            if (cosClient != null) {
                return cosClient;
            }
            if (!cosConfig.isEnabled()) {
                throw BusinessException.internal("COS 未启用，请检查 cos.enabled 配置");
            }
            String secretId = cosConfig.getSecretId();
            String secretKey = cosConfig.getSecretKey();
            if (secretId == null || secretId.isBlank()) {
                throw BusinessException.internal("COS secret-id 未配置");
            }
            if (secretKey == null || secretKey.isBlank()) {
                throw BusinessException.internal("COS secret-key 未配置");
            }

            COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
            ClientConfig clientConfig = new ClientConfig(new Region(cosConfig.getRegion()));
            cosClient = new COSClient(credentials, clientConfig);
            log.info("[COS] SDK client initialized, bucket={}, region={}", cosConfig.getBucket(), cosConfig.getRegion());
            return cosClient;
        }
    }

    /**
     * 上传文件到 COS，返回完整访问 URL。
     */
    public String upload(MultipartFile file) {
        String key = cosConfig.generateKey(file.getOriginalFilename());
        String bucket = cosConfig.getBucket();
        log.info("[COS] upload start, bucket={}, key={}, size={}", bucket, key, file.getSize());

        try (InputStream is = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            // inline：让浏览器直接显示（预览），不弹出下载框
            metadata.setContentDisposition("inline");
            PutObjectRequest putRequest = new PutObjectRequest(bucket, key, is, metadata);

            PutObjectResult putResult = getClient().putObject(putRequest);
            log.info("[COS] upload success, key={}, etag={}", key, putResult.getETag());

            String url = cosConfig.getDomain() + "/" + key + "?response-content-disposition=inline";
            log.info("[COS] public url={}", url);
            return url;
        } catch (IOException e) {
            throw BusinessException.internal("文件读取失败: " + e.getMessage());
        } catch (CosServiceException e) {
            log.error("[COS] upload failed, key={}, code={}, message={}", key, e.getErrorCode(), e.getErrorMessage());
            throw BusinessException.internal("COS 上传失败 [" + e.getErrorCode() + "]: " + e.getErrorMessage());
        } catch (CosClientException e) {
            log.error("[COS] upload failed (client), key={}, message={}", key, e.getMessage());
            throw BusinessException.internal("COS 上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除 COS 对象。
     */
    public void delete(String url) {
        String key = extractKey(url);
        if (key == null || key.isBlank()) {
            log.warn("[COS] Cannot extract key from url={}", url);
            return;
        }
        String bucket = cosConfig.getBucket();
        log.info("[COS] delete start, bucket={}, key={}", bucket, key);

        try {
            DeleteObjectRequest deleteRequest = new DeleteObjectRequest(bucket, key);
            getClient().deleteObject(deleteRequest);
            log.info("[COS] delete success, key={}", key);
        } catch (CosServiceException e) {
            // 404 也算成功（幂等）
            if ("NoSuchKey".equals(e.getErrorCode())) {
                log.info("[COS] delete skip (not found), key={}", key);
                return;
            }
            log.error("[COS] delete failed, key={}, code={}, message={}", key, e.getErrorCode(), e.getErrorMessage());
            throw BusinessException.internal("COS 删除失败 [" + e.getErrorCode() + "]: " + e.getErrorMessage());
        } catch (CosClientException e) {
            log.error("[COS] delete failed (client), key={}, message={}", key, e.getMessage());
            throw BusinessException.internal("COS 删除失败: " + e.getMessage());
        }
    }

    /**
     * 从完整 URL 中提取 COS key。
     */
    private String extractKey(String url) {
        if (url == null) return null;
        // 去掉查询参数（?response-content-disposition=inline）
        int q = url.indexOf('?');
        if (q >= 0) url = url.substring(0, q);
        String domain = cosConfig.getDomain();
        if (domain != null && !domain.isBlank() && url.startsWith(domain)) {
            return url.substring(domain.length() + 1);
        }
        if (url.startsWith("https://" + cosConfig.getBucketHost())) {
            return url.substring(("https://" + cosConfig.getBucketHost()).length() + 1);
        }
        return url;
    }
}
