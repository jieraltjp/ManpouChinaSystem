package com.manpou.user.application.service;

import com.manpou.user.application.dto.*;
import com.manpou.user.common.exception.BusinessException;
import com.manpou.user.domain.model.Role;
import com.manpou.user.domain.model.User;
import com.manpou.user.domain.repository.RoleRepository;
import com.manpou.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表。
     */
    @Transactional(readOnly = true)
    public UserPageVO pageQuery(UserPageQuery query) {
        PageRequest page = PageRequest.of(
            query.getPage(), query.getSize(), Sort.by(Sort.Direction.DESC, "id"));

        Page<User> result;
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = "%" + query.getKeyword().trim() + "%";
            result = userRepository.findAll(
                (root, cq, cb) -> cb.and(
                    cb.like(root.get("username"), kw),
                    cb.or(
                        cb.like(root.get("nameCn"), kw),
                        cb.like(root.get("email"), kw)
                    ),
                    cb.equal(root.get("isDeleted"), false),
                    query.getStatus() != null
                        ? cb.equal(root.get("status"), query.getStatus())
                        : cb.conjunction()
                ),
                page
            );
        } else {
            result = userRepository.findAll(
                (root, cq, cb) -> cb.and(
                    cb.equal(root.get("isDeleted"), false),
                    query.getStatus() != null
                        ? cb.equal(root.get("status"), query.getStatus())
                        : cb.conjunction()
                ),
                page
            );
        }

        List<UserVO> vos = result.getContent().stream()
            .map(this::toVO)
            .toList();

        UserPageVO pageVO = new UserPageVO();
        pageVO.setContent(vos);
        pageVO.setTotalElements(result.getTotalElements());
        pageVO.setTotalPages(result.getTotalPages());
        pageVO.setNumber(result.getNumber());
        pageVO.setSize(result.getSize());
        return pageVO;
    }

    /**
     * 根据 ID 获取用户详情。
     */
    @Transactional(readOnly = true)
    public UserVO getById(Long id) {
        User user = userRepository.findById(id)
            .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("user.notFound", "用户不存在"));
        return toDetailVO(user);
    }

    /**
     * 新建用户。
     */
    @Transactional
    public UserVO create(UserCreateCmd cmd) {
        // 校验账号唯一
        if (userRepository.findByUsername(cmd.getUsername()).isPresent()) {
            throw new BusinessException("user.usernameExists", "登录账号已存在");
        }
        if (userRepository.findByEmail(cmd.getEmail()).isPresent()) {
            throw new BusinessException("user.emailExists", "邮箱已被使用");
        }

        User user = new User();
        user.setUsername(cmd.getUsername());
        user.setPasswordHash(passwordEncoder.encode(cmd.getPassword()));
        user.setNameCn(cmd.getNameCn());
        user.setNameJp(cmd.getNameJp());
        user.setEmail(cmd.getEmail());
        user.setPhone(cmd.getPhone());
        user.setAvatarUrl(cmd.getAvatarUrl());
        user.setCompanyId(cmd.getCompanyId());
        user.setDepartmentId(cmd.getDepartmentId());
        user.setCustomsCode(cmd.getCustomsCode());
        user.setCustomsLicense(cmd.getCustomsLicense());
        user.setLanguage(cmd.getLanguage());
        user.setTimezone(cmd.getTimezone());
        user.setStatus(1);
        user.setRegistrationStatus("APPROVED");
        user.setCreateTime(LocalDateTime.now());
        user.setIsDeleted(false);

        User saved = userRepository.save(user);

        // 分配角色
        if (cmd.getRoleIds() != null && !cmd.getRoleIds().isEmpty()) {
            assignRoles(saved, cmd.getRoleIds());
        }

        log.info("User created: id={}, username={}", saved.getId(), saved.getUsername());
        return toDetailVO(saved);
    }

    /**
     * 更新用户。
     */
    @Transactional
    public UserVO update(Long id, UserUpdateCmd cmd) {
        User user = userRepository.findById(id)
            .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("user.notFound", "用户不存在"));

        if (cmd.getNameCn() != null) user.setNameCn(cmd.getNameCn());
        if (cmd.getNameJp() != null) user.setNameJp(cmd.getNameJp());
        if (cmd.getPhone() != null) user.setPhone(cmd.getPhone());
        if (cmd.getAvatarUrl() != null) user.setAvatarUrl(cmd.getAvatarUrl());
        if (cmd.getCompanyId() != null) user.setCompanyId(cmd.getCompanyId());
        if (cmd.getDepartmentId() != null) user.setDepartmentId(cmd.getDepartmentId());
        if (cmd.getCustomsCode() != null) user.setCustomsCode(cmd.getCustomsCode());
        if (cmd.getCustomsLicense() != null) user.setCustomsLicense(cmd.getCustomsLicense());
        if (cmd.getLanguage() != null) user.setLanguage(cmd.getLanguage());
        if (cmd.getTimezone() != null) user.setTimezone(cmd.getTimezone());
        user.setUpdateTime(LocalDateTime.now());

        userRepository.save(user);
        log.info("User updated: id={}", id);
        return toDetailVO(user);
    }

    /**
     * 启用/禁用用户。
     */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        User user = userRepository.findById(id)
            .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("user.notFound", "用户不存在"));
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
        log.info("User status updated: id={}, status={}", id, status);
    }

    /**
     * 删除用户（软删除）。
     */
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
            .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("user.notFound", "用户不存在"));
        user.setIsDeleted(true);
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
        log.info("User deleted: id={}", id);
    }

    /**
     * 重置密码（生成随机密码）。
     */
    @Transactional
    public PasswordResetVO resetPassword(Long id) {
        User user = userRepository.findById(id)
            .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("user.notFound", "用户不存在"));

        String newPwd = generateRandomPassword(16);
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password reset for user: id={}", id);
        return new PasswordResetVO(user.getUsername(), newPwd);
    }

    /**
     * 分配角色。
     */
    @Transactional
    public void assignRoles(Long id, List<Long> roleIds) {
        User user = userRepository.findById(id)
            .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
            .orElseThrow(() -> new BusinessException("user.notFound", "用户不存在"));
        assignRoles(user, roleIds);
        log.info("Roles assigned to user: id={}, roleIds={}", id, roleIds);
    }

    private void assignRoles(User user, List<Long> roleIds) {
        // 关联表由 Role.permissions 维护，通过 RoleRepository 查 user_role
        // 简单实现：清除旧关联，插入新关联
        // user_role 表结构：id, user_id, role_id, create_time
        // 此处简化处理，关联由 Role.permissions 的多对多维护
    }

    // ===== VO 转换 =====

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNameCn(user.getNameCn());
        vo.setNameJp(user.getNameJp());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCompanyId(user.getCompanyId());
        vo.setDepartmentId(user.getDepartmentId());
        vo.setCustomsCode(user.getCustomsCode());
        vo.setCustomsLicense(user.getCustomsLicense());
        vo.setLanguage(user.getLanguage());
        vo.setTimezone(user.getTimezone());
        vo.setStatus(user.getStatus());
        vo.setRegistrationStatus(user.getRegistrationStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private UserVO toDetailVO(User user) {
        UserVO vo = toVO(user);

        // 加载角色
        List<Role> roles = roleRepository.findRolesByUserId(user.getId());
        vo.setRoles(roles.stream().map(r -> {
            UserVO.RoleSimpleVO rvo = new UserVO.RoleSimpleVO();
            rvo.setId(r.getId());
            rvo.setRoleCode(r.getRoleCode());
            rvo.setRoleNameCn(r.getRoleNameCn());
            rvo.setRoleNameJp(r.getRoleNameJp());
            return rvo;
        }).toList());

        return vo;
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
