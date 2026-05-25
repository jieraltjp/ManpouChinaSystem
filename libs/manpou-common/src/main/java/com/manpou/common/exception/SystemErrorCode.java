package com.manpou.common.exception;

/**
 * 系统级错误码常量（INTJ 审计 2026-04-22）。
 * <p>
 * 对应 CODGEM P1 错误码规范：
 * E1xxx = 参数错误，E4xxx = 资源错误，E6xxx = 系统错误。
 * <p>
 * 业务错误码格式：{DOMAIN}_{NUMBER}，如 AUTH_1001 / PROC_2001。
 *
 * @see GlobalExceptionHandler
 */
public final class SystemErrorCode {

    private SystemErrorCode() {}

    // ===================== 系统级错误码 =====================

    /** 参数类型不匹配 */
    public static final String PARAM_TYPE_MISMATCH = "E1001";

    /** API 端点不存在 */
    public static final String RESOURCE_NOT_FOUND = "E4001";

    /** HTTP 方法不支持 */
    public static final String METHOD_NOT_ALLOWED = "E4002";

    /** 内部系统错误 */
    public static final String INTERNAL_ERROR = "E6001";

    // ===================== 认证/授权错误码（AUTH_1xxx） =====================

    /** Token 已过期或无效 */
    public static final String AUTH_1001 = "AUTH_1001";

    /** 登录失败（用户名或密码错误） */
    public static final String AUTH_1002 = "AUTH_1002";

    /** 无权限访问该资源 */
    public static final String AUTH_1003 = "AUTH_1003";

    /** 账户已被禁用 */
    public static final String AUTH_1004 = "AUTH_1004";

    // ===================== 业务错误码（PROC_2xxx） =====================

    /** 发注单状态不允许此操作 */
    public static final String PROC_2001 = "PROC_2001";

    /** 发注单不存在 */
    public static final String PROC_2002 = "PROC_2002";

    /** 发注单编号已存在 */
    public static final String PROC_2003 = "PROC_2003";

    // ===================== 需求单错误码（DEMAND_21xx） =====================

    /** 需求单状态不允许此操作 */
    public static final String DEMAND_2101 = "DEMAND_2101";

    /** 需求单不存在 */
    public static final String DEMAND_2102 = "DEMAND_2102";

    // ===================== 验货记录错误码（QC_22xx） =====================

    /** 验货记录状态不允许此操作 */
    public static final String QC_2201 = "QC_2201";

    /** 验货记录不存在 */
    public static final String QC_2202 = "QC_2202";

    // ===================== 物流/货柜/船只错误码（SHIP_3xxx） =====================

    /** 货柜状态不允许此操作 */
    public static final String SHIP_3001 = "SHIP_3001";

    /** 货柜不存在 */
    public static final String SHIP_3002 = "SHIP_3002";

    /** 船只状态不允许此操作 */
    public static final String SHIP_3003 = "SHIP_3003";

    /** 船只不存在 */
    public static final String SHIP_3004 = "SHIP_3004";

    // ===================== 商品/产品错误码（PROD_4xxx） =====================

    /** 商品不存在 */
    public static final String PROD_4001 = "PROD_4001";

    /** 商品编号已存在 */
    public static final String PROD_4002 = "PROD_4002";

    // ===================== 工厂错误码（FACTORY_5xxx） =====================

    /** 工厂不存在 */
    public static final String FACTORY_5001 = "FACTORY_5001";

    // ===================== 报关/税务错误码（CUSTOMS_6xxx） =====================

    /** 报关记录不存在 */
    public static final String CUSTOMS_6001 = "CUSTOMS_6001";

    /** 报关状态不允许此操作 */
    public static final String CUSTOMS_6002 = "CUSTOMS_6002";

    // ===================== 财务/销售错误码（FINANCE_7xxx / SALES_8xxx） =====================

    /** 财务记录不存在 */
    public static final String FINANCE_7001 = "FINANCE_7001";

    // ===================== 系统级错误码（SYS_9xxx） =====================

    /** 数据库连接失败 */
    public static final String SYS_9001 = "SYS_9001";

    /** 外部服务调用失败 */
    public static final String SYS_9002 = "SYS_9002";

    /** 文件上传失败 */
    public static final String SYS_9003 = "SYS_9003";

    /** 文件下载失败 */
    public static final String SYS_9004 = "SYS_9004";

    /** 数据一致性校验失败 */
    public static final String SYS_9005 = "SYS_9005";
}
