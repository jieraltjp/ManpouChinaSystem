package com.manpou.notification.domain.repository;

import com.manpou.notification.domain.model.SigningKey;
import com.manpou.notification.domain.model.SigningKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 签名密钥仓库接口。
 */
@Repository
public interface SigningKeyRepository extends JpaRepository<SigningKey, Long> {

    /**
     * 查询当前活跃密钥（用于签发新 Token）。
     */
    Optional<SigningKey> findByStatus(SigningKeyStatus status);

    /**
     * 根据 kid 查询密钥（用于验签）。
     */
    Optional<SigningKey> findByKid(String kid);

    /**
     * 查询所有密钥（按创建时间倒序）。
     */
    List<SigningKey> findAllByOrderByCreateTimeDesc();
}

