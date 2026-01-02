package com.dpbug.common.utils;

import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 断言工具类
 * <p>用于参数校验，校验失败时抛出 BusinessException</p>
 */
public class Assert {

    private Assert() {
    }

    /**
     * 断言对象不为空
     *
     * @param object  待判断对象
     * @param message 异常信息
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(ResultCode.PARAM_IS_NULL, message);
        }
    }

    /**
     * 断言对象不为空（使用默认消息）
     */
    public static void notNull(Object object) {
        notNull(object, "参数不能为空");
    }

    /**
     * 断言对象为空
     *
     * @param object  待判断对象
     * @param message 异常信息
     */
    public static void isNull(Object object, String message) {
        if (object != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言字符串不为空
     *
     * @param str     待判断字符串
     * @param message 异常信息
     */
    public static void notEmpty(String str, String message) {
        if (StringUtils.isEmpty(str)) {
            throw new BusinessException(ResultCode.PARAM_IS_NULL, message);
        }
    }

    /**
     * 断言字符串不为空（使用默认消息）
     */
    public static void notEmpty(String str) {
        notEmpty(str, "字符串参数不能为空");
    }

    /**
     * 断言字符串不为空白
     *
     * @param str     待判断字符串
     * @param message 异常信息
     */
    public static void notBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new BusinessException(ResultCode.PARAM_IS_NULL, message);
        }
    }

    /**
     * 断言字符串不为空白（使用默认消息）
     */
    public static void notBlank(String str) {
        notBlank(str, "字符串参数不能为空");
    }

    /**
     * 断言集合不为空
     *
     * @param collection 待判断集合
     * @param message    异常信息
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_IS_NULL, message);
        }
    }

    /**
     * 断言集合不为空（使用默认消息）
     */
    public static void notEmpty(Collection<?> collection) {
        notEmpty(collection, "集合参数不能为空");
    }

    /**
     * 断言Map不为空
     *
     * @param map     待判断Map
     * @param message 异常信息
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_IS_NULL, message);
        }
    }

    /**
     * 断言Map不为空（使用默认消息）
     */
    public static void notEmpty(Map<?, ?> map) {
        notEmpty(map, "Map参数不能为空");
    }

    /**
     * 断言数组不为空
     *
     * @param array   待判断数组
     * @param message 异常信息
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new BusinessException(ResultCode.PARAM_IS_NULL, message);
        }
    }

    /**
     * 断言数组不为空（使用默认消息）
     */
    public static void notEmpty(Object[] array) {
        notEmpty(array, "数组参数不能为空");
    }

    /**
     * 断言布尔表达式为true
     *
     * @param expression 布尔表达式
     * @param message    异常信息
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(ResultCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言布尔表达式为true（使用ResultCode）
     */
    public static void isTrue(boolean expression, ResultCode resultCode) {
        if (!expression) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * 断言布尔表达式为false
     *
     * @param expression 布尔表达式
     * @param message    异常信息
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BusinessException(ResultCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言两个对象相等
     *
     * @param obj1    对象1
     * @param obj2    对象2
     * @param message 异常信息
     */
    public static void equals(Object obj1, Object obj2, String message) {
        if (!Objects.equals(obj1, obj2)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言两个对象不相等
     *
     * @param obj1    对象1
     * @param obj2    对象2
     * @param message 异常信息
     */
    public static void notEquals(Object obj1, Object obj2, String message) {
        if (Objects.equals(obj1, obj2)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, message);
        }
    }

    /**
     * 直接抛出业务异常
     *
     * @param message 异常信息
     */
    public static void fail(String message) {
        throw new BusinessException(message);
    }

    /**
     * 直接抛出业务异常（使用ResultCode）
     */
    public static void fail(ResultCode resultCode) {
        throw new BusinessException(resultCode);
    }

    /**
     * 直接抛出业务异常（使用ResultCode和自定义消息）
     */
    public static void fail(ResultCode resultCode, String message) {
        throw new BusinessException(resultCode, message);
    }
}