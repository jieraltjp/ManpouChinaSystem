package com.manpou.customs.infrastructure.security;

import com.manpou.customs.common.context.UserContext;

/**
 * JWT 上下文 Holder。
 * 持有当前请求的用户上下文（ThreadLocal）。
 *
 * 返回类型为 {@link UserContext}（common 包，跨层共享），
 * 各层均通过此接口访问用户上下文，禁止直接引用 Infrastructure 内部类型。
 */
public class JwtContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    public static void set(UserContext userContext) {
        CONTEXT.set(userContext);
    }

    /**
     * 返回 {@link UserContext}（common 包），而非 Infrastructure 内部类型。
     */
    public static UserContext get() {
        return CONTEXT.get();
    }

    public static String getUserId() {
        UserContext ctx = get();
        return ctx != null ? ctx.getUserId() : null;
    }

    public static String getTenantId() {
        UserContext ctx = get();
        return ctx != null ? ctx.getTenantId() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
