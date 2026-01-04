package com.dpbug.server.mapper.novel;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dpbug.server.model.entity.novel.NovelChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 章节Mapper接口
 *
 * @author dpbug
 */
@Mapper
public interface ChapterMapper extends BaseMapper<NovelChapter> {

    /**
     * 查询项目章节列表(按章节号排序)
     *
     * @param projectId 项目ID
     * @return 章节列表
     */
    List<NovelChapter> selectByProjectIdOrdered(@Param("projectId") Long projectId);

    /**
     * 查询最近N章(完整内容)
     *
     * @param projectId           项目ID
     * @param currentChapterNumber 当前章节号
     * @param limit               限制数量
     * @return 章节列表(倒序)
     */
    List<NovelChapter> selectRecentChapters(
            @Param("projectId") Long projectId,
            @Param("currentChapterNumber") Integer currentChapterNumber,
            @Param("limit") Integer limit
    );

    /**
     * 查询章节摘要(用于上下文构建,不包含content字段)
     *
     * @param projectId            项目ID
     * @param currentChapterNumber 当前章节号
     * @return 章节摘要列表
     */
    List<NovelChapter> selectChapterSummaries(
            @Param("projectId") Long projectId,
            @Param("currentChapterNumber") Integer currentChapterNumber
    );

    /**
     * 获取项目最大章节号
     *
     * @param projectId 项目ID
     * @return 最大章节号
     */
    Integer selectMaxChapterNumber(@Param("projectId") Long projectId);

    /**
     * 统计项目章节数和总字数
     *
     * @param projectId 项目ID
     * @return 统计结果 {chapterCount, totalWords}
     */
    Map<String, Object> selectProjectStatistics(@Param("projectId") Long projectId);
}