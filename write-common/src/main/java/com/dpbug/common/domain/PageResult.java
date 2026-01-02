package com.dpbug.common.domain;

import com.dpbug.common.constant.CommonConstants;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 *
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 数据列表
     */
    private List<T> list;

    public PageResult() {
        this.pageNum = CommonConstants.DEFAULT_PAGE_NUM;
        this.pageSize = CommonConstants.DEFAULT_PAGE_SIZE;
        this.total = 0L;
        this.pages = 0;
    }

    public PageResult(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
        this.pages = calculatePages(total, pageSize);
    }

    /**
     * 计算总页数
     */
    private Integer calculatePages(Long total, Integer pageSize) {
        if (total == 0 || pageSize == 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        return new PageResult<>(pageNum, pageSize, total, list);
    }

    /**
     * 创建空的分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>();
    }

    /**
     * 创建空的分页结果（指定页码和每页大小）
     */
    public static <T> PageResult<T> empty(Integer pageNum, Integer pageSize) {
        return new PageResult<>(pageNum, pageSize, 0L, null);
    }

    /**
     * 是否有数据
     */
    public boolean hasData() {
        return list != null && !list.isEmpty();
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return pageNum < pages;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return pageNum > 1;
    }
}