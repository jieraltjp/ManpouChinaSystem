package com.manpou.allinone.domain.repository;

import com.manpou.allinone.domain.model.SigningKey;
import com.manpou.allinone.domain.model.SigningKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SigningKeyRepository extends JpaRepository<SigningKey, Long> {

    Optional<SigningKey> findByStatus(SigningKeyStatus status);

    Optional<SigningKey> findByKid(String kid);

    @Query("SELECT s FROM SigningKey s ORDER BY s.createTime DESC")
    List<SigningKey> findAllOrderByCreateTimeDesc();
}
