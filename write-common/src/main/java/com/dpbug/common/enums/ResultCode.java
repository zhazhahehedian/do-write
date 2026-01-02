package com.dpbug.common.enums;

import lombok.Getter;

/**
 * 统一返回状态码枚举
 */
@Getter
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    FAILED(500, "操作失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 参数为空
     */
    PARAM_IS_NULL(400, "参数为空"),

    /**
     * 参数类型错误
     */
    PARAM_TYPE_ERROR(400, "参数类型错误"),

    /**
     * 参数缺失
     */
    PARAM_MISSING(400, "参数缺失"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 权限不足
     */
    FORBIDDEN(403, "权限不足"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(408, "请求超时"),

    /**
     * 请求冲突
     */
    CONFLICT(409, "请求冲突"),

    /**
     * 请求过于频繁
     */
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    /**
     * 系统内部错误
     */
    INTERNAL_SERVER_ERROR(500, "系统内部错误"),

    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    /**
     * 网关超时
     */
    GATEWAY_TIMEOUT(504, "网关超时"),

    /**
     * 业务异常
     */
    BUSINESS_ERROR(600, "业务处理异常"),

    /**
     * 数据不存在
     */
    DATA_NOT_EXIST(601, "数据不存在"),

    /**
     * 数据已存在
     */
    DATA_ALREADY_EXIST(602, "数据已存在"),

    /**
     * 数据库操作失败
     */
    DATABASE_ERROR(603, "数据库操作失败"),

    /**
     * AI服务调用失败
     */
    AI_SERVICE_ERROR(700, "AI服务调用失败"),

    /**
     * AI模型不可用
     */
    AI_MODEL_UNAVAILABLE(701, "AI模型暂时不可用"),

    /**
     * AI请求超时
     */
    AI_REQUEST_TIMEOUT(702, "AI请求超时"),

    /**
     * AI配额不足
     */
    AI_QUOTA_EXCEEDED(703, "AI服务配额不足");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 返回信息
     */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}