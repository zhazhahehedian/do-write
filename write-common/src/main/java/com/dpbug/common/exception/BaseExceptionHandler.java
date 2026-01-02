package com.dpbug.common.exception;

import com.dpbug.common.domain.Result;
import com.dpbug.common.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器基类
 * <p>
 * 注意：此类需要在使用的模块中配合 @RestControllerAdvice 注解使用
 * 由于 write-common 模块不包含 Spring Web 依赖，具体的异常处理器需要在各业务模块中实现
 * </p>
 */
@Slf4j
public abstract class BaseExceptionHandler {

    /**
     * 处理业务异常
     */
    protected Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常：code={}, message={}", e.getCode(), e.getMessage(), e);
        return Result.result(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    protected Result<?> handleValidationException(Exception e) {
        log.error("参数校验异常：{}", e.getMessage(), e);
        return Result.result(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理参数类型异常
     */
    protected Result<?> handleTypeMismatchException(Exception e) {
        log.error("参数类型异常：{}", e.getMessage(), e);
        return Result.failed(ResultCode.PARAM_TYPE_ERROR);
    }

    /**
     * 处理方法参数无效异常
     */
    protected Result<?> handleMethodArgumentNotValidException(Exception e) {
        log.error("方法参数无效异常：{}", e.getMessage(), e);
        return Result.result(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理请求方法不支持异常
     */
    protected Result<?> handleMethodNotSupportedException(Exception e) {
        log.error("请求方法不支持：{}", e.getMessage(), e);
        return Result.failed(ResultCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理系统异常
     */
    protected Result<?> handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return Result.failed(ResultCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理空指针异常
     */
    protected Result<?> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常：{}", e.getMessage(), e);
        return Result.result(ResultCode.INTERNAL_SERVER_ERROR.getCode(), "系统异常：空指针");
    }

    /**
     * 处理数据库异常
     */
    protected Result<?> handleDatabaseException(Exception e) {
        log.error("数据库异常：{}", e.getMessage(), e);
        return Result.failed(ResultCode.DATABASE_ERROR);
    }
}