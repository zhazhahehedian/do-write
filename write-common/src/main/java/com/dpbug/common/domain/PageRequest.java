package com.dpbug.common.domain;

import com.dpbug.common.constant.CommonConstants;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求参数
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从1开始）
     */
    private Integer pageNum = CommonConstants.DEFAULT_PAGE_NUM;

    /**
     * 每页大小
     */
    private Integer pageSize = CommonConstants.DEFAULT_PAGE_SIZE;

    /**
     * 排序字段
     */
    private String orderBy;

    /**
     * 排序方式（asc/desc）
     */
    private String order;

    public PageRequest() {
    }

    public PageRequest(Integer pageNum, Integer pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public PageRequest(Integer pageNum, Integer pageSize, String orderBy, String order) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.orderBy = orderBy;
        this.order = order;
    }

    /**
     * 获取页码（确保不小于1）
     */
    public Integer getPageNum() {
        if (pageNum == null || pageNum < 1) {
            return CommonConstants.DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    /**
     * 获取每页大小（确保在合理范围内）
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return CommonConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize > CommonConstants.MAX_PAGE_SIZE) {
            return CommonConstants.MAX_PAGE_SIZE;
        }
        return pageSize;
    }

    /**
     * 计算偏移量（用于SQL LIMIT）
     */
    public Integer getOffset() {
        return (getPageNum() - 1) * getPageSize();
    }

    /**
     * 创建分页请求
     */
    public static PageRequest of(Integer pageNum, Integer pageSize) {
        return new PageRequest(pageNum, pageSize);
    }

    /**
     * 创建默认分页请求
     */
    public static PageRequest defaultPage() {
        return new PageRequest();
    }
}