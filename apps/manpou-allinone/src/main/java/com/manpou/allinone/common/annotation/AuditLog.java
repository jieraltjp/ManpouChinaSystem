package com.manpou.allinone.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解。
 * 标注在 Controller 方法上，自动记录操作日志到 user-service。
 *
 * <p>仅记录成功操作（方法正常返回）。业务异常同样记录。
 *
 * <p>SpEL 表达式支持：
 * <ul>
 *   <li>resourceId: 从方法参数提取，如 "#cmd.id"</li>
 *   <li>resourceCode: 如 "#cmd.code"</li>
 * </ul>
 *
 * <p>敏感字段（password/token/secret/credential/key）自动脱敏。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 业务模块，如 "procurement"、"factory"、"user" */
    String module();

    /** 操作动作，如 "CREATE"、"UPDATE"、"DELETE" */
    String action();

    /** 资源类型，如 "demand"、"procurement"、"factory" */
    String resourceType() default "";

    /** 资源 ID（SpEL 表达式），如 "#cmd.id" */
    String resourceId() default "";

    /** 资源编码（SpEL 表达式），如 "#cmd.code" */
    String resourceCode() default "";
}
