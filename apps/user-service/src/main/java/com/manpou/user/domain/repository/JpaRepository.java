package com.manpou.user.domain.repository;

import com.manpou.user.domain.model.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 通用仓库接口。
 * 所有业务仓库应继承此接口。
 * JPA 实现见 infrastructure/persistence/JpaGenericRepository。
 *
 * @param <E>  实体类型，必须继承 BaseEntity
 * @param <ID> 主键类型
 */
@NoRepositoryBean
public interface JpaRepository<E extends BaseEntity, ID> extends
        Repository<E, ID>,
        JpaSpecificationExecutor<E> {

    // ===== 基础 CRUD（逻辑删除过滤已封装） =====

    /**
     * 根据 ID 查询，自动过滤已删除记录。
     */
    Optional<E> findByIdAndIsDeletedFalse(ID id);

    /**
     * 查询所有未删除记录。
     */
    List<E> findAllByIsDeletedFalse();

    /**
     * 分页查询未删除记录。
     * 注意：排序字段使用 Java 属性名（如 createTime），JPA 自动映射到列名。
     */
    Page<E> findAllByIsDeletedFalse(Pageable pageable);

    /**
     * 保存实体（新建或更新）。
     */
    <S extends E> S save(S entity);
}
