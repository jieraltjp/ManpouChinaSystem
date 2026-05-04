package com.manpou.user.application.service;

import com.manpou.user.application.dto.*;
import com.manpou.user.common.exception.BusinessException;
import com.manpou.user.domain.model.Permission;
import com.manpou.user.domain.model.Role;
import com.manpou.user.domain.repository.PermissionRepository;
import com.manpou.user.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色管理服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * 角色列表（不含权限）。
     */
    @Transactional(readOnly = true)
    public List<RoleSimpleVO> listAll() {
        return roleRepository.findAll().stream()
            .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
            .map(this::toSimpleVO)
            .toList();
    }

    /**
     * 角色详情（含权限列表）。
     */
    @Transactional(readOnly = true)
    public RoleVO getById(Long id) {
        Role role = roleRepository.findById(id)
            .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("role.notFound", "角色不存在"));
        return toVO(role);
    }

    /**
     * 新建角色。
     */
    @Transactional
    public RoleVO create(RoleCreateCmd cmd) {
        if (roleRepository.findByRoleCode(cmd.getRoleCode()).isPresent()) {
            throw new BusinessException("role.codeExists", "角色编码已存在");
        }

        Role role = new Role();
        role.setRoleCode(cmd.getRoleCode());
        role.setRoleNameCn(cmd.getRoleNameCn());
        role.setRoleNameJp(cmd.getRoleNameJp());
        role.setRoleType(cmd.getRoleType());
        role.setDescription(cmd.getDescription());
        role.setIsEditable(1);
        role.setStatus(1);
        role.setIsDeleted(false);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());

        if (cmd.getPermissionIds() != null && !cmd.getPermissionIds().isEmpty()) {
            Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(cmd.getPermissionIds()));
            role.setPermissions(perms);
        }

        Role saved = roleRepository.save(role);
        log.info("Role created: id={}, code={}", saved.getId(), saved.getRoleCode());
        return toVO(saved);
    }

    /**
     * 更新角色（系统内置不可编辑编码）。
     */
    @Transactional
    public RoleVO update(Long id, RoleUpdateCmd cmd) {
        Role role = roleRepository.findById(id)
            .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("role.notFound", "角色不存在"));

        if (role.getIsEditable() != null && role.getIsEditable() == 0) {
            throw new BusinessException("role.notEditable", "系统内置角色不可编辑");
        }

        if (cmd.getRoleNameCn() != null) role.setRoleNameCn(cmd.getRoleNameCn());
        if (cmd.getRoleNameJp() != null) role.setRoleNameJp(cmd.getRoleNameJp());
        if (cmd.getDescription() != null) role.setDescription(cmd.getDescription());
        role.setUpdateTime(LocalDateTime.now());

        Role saved = roleRepository.save(role);
        log.info("Role updated: id={}", id);
        return toVO(saved);
    }

    /**
     * 删除角色（系统内置不可删除）。
     */
    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
            .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("role.notFound", "角色不存在"));

        if (role.getIsEditable() != null && role.getIsEditable() == 0) {
            throw new BusinessException("role.notDeletable", "系统内置角色不可删除");
        }

        role.setIsDeleted(true);
        role.setUpdateTime(LocalDateTime.now());
        roleRepository.save(role);
        log.info("Role deleted: id={}", id);
    }

    /**
     * 分配权限。
     */
    @Transactional
    public RoleVO assignPermissions(Long id, RolePermissionsCmd cmd) {
        Role role = roleRepository.findById(id)
            .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("role.notFound", "角色不存在"));

        if (role.getIsEditable() != null && role.getIsEditable() == 0) {
            throw new BusinessException("role.notEditable", "系统内置角色不可修改权限");
        }

        Set<Permission> perms = cmd.getPermissionIds() == null || cmd.getPermissionIds().isEmpty()
            ? new HashSet<>()
            : new HashSet<>(permissionRepository.findAllById(cmd.getPermissionIds()));
        role.setPermissions(perms);
        role.setUpdateTime(LocalDateTime.now());

        Role saved = roleRepository.save(role);
        log.info("Permissions assigned to role: id={}, count={}", id, perms.size());
        return toVO(saved);
    }

    /**
     * 局部更新角色（允许修改 isEditable / description）。
     * 仅管理员通过 Flyway 迁移或调试时使用。
     */
    @Transactional
    public RoleVO patch(Long id, RolePatchCmd cmd) {
        Role role = roleRepository.findById(id)
            .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("role.notFound", "角色不存在"));

        if (cmd.getIsEditable() != null) role.setIsEditable(cmd.getIsEditable());
        if (cmd.getDescription() != null) role.setDescription(cmd.getDescription());
        role.setUpdateTime(LocalDateTime.now());

        Role saved = roleRepository.save(role);
        log.info("Role patched: id={}, isEditable={}, description={}", id, cmd.getIsEditable(), cmd.getDescription());
        return toVO(saved);
    }

    // ===== 权限树 =====

    /**
     * 权限树（按模块分组）。
     */
    @Transactional(readOnly = true)
    public List<PermissionTreeVO> getPermissionTree() {
        List<Permission> all = permissionRepository.findAll().stream()
            .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
            .toList();

        // 按模块分组
        Map<String, List<PermissionVO>> byModule = all.stream()
            .map(this::toPermissionVO)
            .collect(Collectors.groupingBy(PermissionVO::getModule));

        return byModule.entrySet().stream()
            .map(e -> {
                PermissionTreeVO tree = new PermissionTreeVO();
                tree.setModule(e.getKey());
                tree.setModuleNameCn(moduleNameCn(e.getKey()));
                tree.setModuleNameJp(moduleNameJp(e.getKey()));
                tree.setPermissions(e.getValue());
                return tree;
            })
            .sorted(Comparator.comparing(PermissionTreeVO::getModule))
            .toList();
    }

    private static String moduleNameCn(String module) {
        return switch (module) {
            case "demand" -> "补货需求";
            case "procurement" -> "发注单";
            case "shipment" -> "出货管理";
            case "qc" -> "验货";
            case "logistics" -> "物流调配";
            case "consolidation" -> "集拼";
            case "container" -> "货柜";
            case "customs" -> "国内报关";
            case "japan_customs" -> "日本清关";
            case "tax_refund" -> "退税";
            case "sales" -> "销售";
            case "factory" -> "工厂";
            case "product" -> "商品";
            case "user" -> "用户管理";
            case "role" -> "角色管理";
            case "audit" -> "操作日志";
            case "auth" -> "认证";
            default -> module;
        };
    }

    private static String moduleNameJp(String module) {
        return switch (module) {
            case "demand" -> "補充需要";
            case "procurement" -> "発注書";
            case "shipment" -> "出荷管理";
            case "qc" -> "検品";
            case "logistics" -> "物流調整";
            case "consolidation" -> "合体";
            case "container" -> "コンテナ";
            case "customs" -> "国内通関";
            case "japan_customs" -> "日本通関";
            case "tax_refund" -> "退税";
            case "sales" -> "販売";
            case "factory" -> "工場";
            case "product" -> "商品";
            case "user" -> "ユーザー管理";
            case "role" -> "役割管理";
            case "audit" -> "操作ログ";
            case "auth" -> "認証";
            default -> module;
        };
    }

    // ===== VO 转换 =====

    private RoleVO toVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleNameCn(role.getRoleNameCn());
        vo.setRoleNameJp(role.getRoleNameJp());
        vo.setRoleType(role.getRoleType());
        vo.setDescription(role.getDescription());
        vo.setIsEditable(role.getIsEditable());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        vo.setPermissions(role.getPermissions().stream()
            .map(this::toPermissionVO)
            .toList());
        return vo;
    }

    private RoleSimpleVO toSimpleVO(Role role) {
        RoleSimpleVO vo = new RoleSimpleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleNameCn(role.getRoleNameCn());
        vo.setRoleNameJp(role.getRoleNameJp());
        vo.setRoleType(role.getRoleType());
        vo.setIsEditable(role.getIsEditable());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }

    private PermissionVO toPermissionVO(Permission p) {
        return new PermissionVO(
            p.getId(), p.getPermissionCode(),
            p.getPermissionNameCn(), p.getPermissionNameJp(),
            p.getModule(), p.getAction(),
            p.getDescription(), p.getSortOrder()
        );
    }
}
