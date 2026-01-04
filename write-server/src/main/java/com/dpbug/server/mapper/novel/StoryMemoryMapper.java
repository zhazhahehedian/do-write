package com.dpbug.server.mapper.novel;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dpbug.server.model.entity.novel.NovelStoryMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 故事记忆Mapper接口
 *
 * @author dpbug
 */
@Mapper
public interface StoryMemoryMapper extends BaseMapper<NovelStoryMemory> {

    /**
     * 查询章节的所有记忆
     *
     * @param chapterId 章节ID
     * @return 记忆列表
     */
    List<NovelStoryMemory> selectByChapterId(@Param("chapterId") Long chapterId);

    /**
     * 查询项目的未完结伏笔
     *
     * @param projectId 项目ID
     * @return 伏笔列表
     */
    List<NovelStoryMemory> selectPendingForeshadows(@Param("projectId") Long projectId);

    /**
     * 按记忆类型查询
     *
     * @param projectId  项目ID
     * @param memoryType 记忆类型
     * @param limit      限制数量
     * @return 记忆列表
     */
    List<NovelStoryMemory> selectByType(
            @Param("projectId") Long projectId,
            @Param("memoryType") String memoryType,
            @Param("limit") Integer limit
    );

    /**
     * 按重要性查询高分记忆
     *
     * @param projectId 项目ID
     * @param minScore  最低分数
     * @param limit     限制数量
     * @return 记忆列表
     */
    List<NovelStoryMemory> selectImportantMemories(
            @Param("projectId") Long projectId,
            @Param("minScore") BigDecimal minScore,
            @Param("limit") Integer limit
    );
}