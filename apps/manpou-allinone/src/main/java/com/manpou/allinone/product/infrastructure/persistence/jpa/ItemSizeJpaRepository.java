package com.manpou.allinone.product.infrastructure.persistence.jpa;

import com.manpou.allinone.product.domain.model.ItemSize;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemSizeJpaRepository extends JpaRepository<ItemSize, Long> {
}