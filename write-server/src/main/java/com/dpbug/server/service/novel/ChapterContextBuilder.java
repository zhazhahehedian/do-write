package com.dpbug.server.service.novel;

import com.dpbug.server.model.dto.novel.ChapterGenerateRequest;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.vo.novel.ChapterContextVO;
import com.dpbug.server.model.vo.novel.OutlineVO;

/**
 * 章节上下文构建器接口
 *
 * @author dpbug
 */
public interface ChapterContextBuilder {

    /**
     * 构建完整的生成上下文
     *
     * @param userId        用户ID（用于向量存储访问）
     * @param project       项目信息
     * @param outline       当前大纲
     * @param chapterNumber 当前章节号
     * @param request       生成请求
     * @return 上下文VO
     */
    ChapterContextVO buildContext(
            Long userId,
            NovelProject project,
            OutlineVO outline,
            Integer chapterNumber,
            ChapterGenerateRequest request
    );

    /**
     * 构建智能历史上下文(支持海量章节)
     *
     * @param projectId            项目ID
     * @param currentChapterNumber 当前章节号
     * @return 历史上下文字符串
     */
    String buildHistoryContext(Long projectId, Integer currentChapterNumber);

    /**
     * 构建RAG记忆上下文
     *
     * @param userId         用户ID（用于向量存储访问）
     * @param projectId      项目ID
     * @param outlineContent 大纲内容
     * @return 记忆上下文字符串
     */
    String buildMemoryContext(Long userId, Long projectId, String outlineContent);
}