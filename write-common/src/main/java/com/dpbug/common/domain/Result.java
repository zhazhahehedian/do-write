package com.dpbug.common.domain;

import com.dpbug.common.enums.ResultCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装类
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public Result(ResultCode resultCode) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
        this.timestamp = System.currentTimeMillis();
    }

    public Result(ResultCode resultCode, T data) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS);
    }

    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS, data);
    }

    /**
     * 成功返回（自定义消息）
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message);
    }

    /**
     * 成功返回（自定义消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> failed() {
        return new Result<>(ResultCode.FAILED);
    }

    /**
     * 失败返回（自定义消息）
     */
    public static <T> Result<T> failed(String message) {
        return new Result<>(ResultCode.FAILED.getCode(), message);
    }

    /**
     * 失败返回（带数据）
     */
    public static <T> Result<T> failed(T data) {
        return new Result<>(ResultCode.FAILED, data);
    }

    /**
     * 失败返回（自定义消息和数据）
     */
    public static <T> Result<T> failed(String message, T data) {
        return new Result<>(ResultCode.FAILED.getCode(), message, data);
    }

    /**
     * 失败返回（使用ResultCode）
     */
    public static <T> Result<T> failed(ResultCode resultCode) {
        return new Result<>(resultCode);
    }

    /**
     * 失败返回（使用ResultCode和数据）
     */
    public static <T> Result<T> failed(ResultCode resultCode, T data) {
        return new Result<>(resultCode, data);
    }

    /**
     * 自定义返回
     */
    public static <T> Result<T> result(Integer code, String message) {
        return new Result<>(code, message);
    }

    /**
     * 自定义返回（带数据）
     */
    public static <T> Result<T> result(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /**
     * 自定义返回（使用ResultCode）
     */
    public static <T> Result<T> result(ResultCode resultCode) {
        return new Result<>(resultCode);
    }

    /**
     * 自定义返回（使用ResultCode和数据）
     */
    public static <T> Result<T> result(ResultCode resultCode, T data) {
        return new Result<>(resultCode, data);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}