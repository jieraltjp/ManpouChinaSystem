package com.manpou.gateway.security;

import java.util.List;

/**
 * JWT Claims 值对象（网关视角，只读）。
 *
 * 由网关从 Token 中提取，下游微服务通过 X-User-Id 等请求头接收。
 * 网关不将完整 Claims 传递下游，只传递必要的安全上下文。
 */
public record JwtClaims(
    String userId,
    String username,
    List<String> roles,
    List<String> permissions,
    String tenantId
) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAuthority(String authority) {
        return permissions != null && permissions.contains(authority);
    }
}
