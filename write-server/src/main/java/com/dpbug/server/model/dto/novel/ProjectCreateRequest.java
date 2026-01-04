package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 项目创建请求
 *
 * @author dpbug
 */
@Data
public class ProjectCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 书名
     */
    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名长度不能超过200个字符")
    private String title;

    /**
     * 简介
     */
    @Size(max = 2000, message = "简介长度不能超过2000个字符")
    private String description;

    /**
     * 主题（如：复仇、成长、爱情）
     */
    @Size(max = 100, message = "主题长度不能超过100个字符")
    private String theme;

    /**
     * 类型（玄幻、都市、科幻等）
     */
    private String genre;

    /**
     * 目标字数
     */
    @Min(value = 0, message = "目标字数不能为负数")
    private Integer targetWords;

    /**
     * 计划章节数
     */
    private Integer chapterCount;

    /**
     * 叙事视角（第一人称/第三人称）
     */
    private String narrativePerspective;

    /**
     * 大纲模式（one-to-one/one-to-many）
     */
    private String outlineMode;

    /**
     * AI模型名称
     */
    private String aiModel;

    /**
     * 写作风格编码
     */
    private String writingStyleCode;
}