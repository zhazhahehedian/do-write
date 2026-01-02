package com.dpbug.common.constant;

/**
 * 通用常量
 */
public interface CommonConstants {

    /**
     * 成功标记
     */
    Integer SUCCESS = 200;

    /**
     * 失败标记
     */
    Integer FAIL = 500;

    /**
     * 编码格式
     */
    String UTF8 = "UTF-8";

    /**
     * JSON 内容类型
     */
    String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

    /**
     * 默认页码
     */
    Integer DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页大小
     */
    Integer DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页大小
     */
    Integer MAX_PAGE_SIZE = 1000;

    /**
     * 是（通用标识）
     */
    Integer YES = 1;

    /**
     * 否（通用标识）
     */
    Integer NO = 0;

    /**
     * 启用状态
     */
    Integer STATUS_ENABLE = 1;

    /**
     * 禁用状态
     */
    Integer STATUS_DISABLE = 0;

    /**
     * 删除标记 - 已删除
     */
    Integer DELETED = 1;

    /**
     * 删除标记 - 未删除
     */
    Integer NOT_DELETED = 0;
}