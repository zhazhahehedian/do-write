package com.dpbug.server.service.novel;

import com.dpbug.server.model.entity.novel.NovelStoryMemory;
import com.dpbug.server.model.vo.novel.StoryMemoryVO;

import java.util.List;

/**
 * 故事记忆服务接口
 *
 * @author dpbug
 */
public interface StoryMemoryService {

    /**
     * 从章节内容中提取记忆
     *
     * @param userId        用户ID（用于获取AI配置）
     * @param projectId     项目ID
     * @param chapterId     章节ID
     * @param chapterNumber 章节序号（用于时间线）
     * @param content       章节内容
     * @return 记忆列表
     */
    List<NovelStoryMemory> extractMemories(Long userId, Long projectId, Long chapterId,
                                           Integer chapterNumber, String content);

    /**
     * 批量保存记忆(含向量化)
     *
     * @param projectId 项目ID
     * @param memories  记忆列表
     */
    void saveMemories(Long projectId, List<NovelStoryMemory> memories);

    /**
     * 语义检索相关记忆
     *
     * @param projectId 项目ID
     * @param query     查询文本
     * @param topK      返回数量
     * @return 记忆列表
     */
    List<StoryMemoryVO> searchRelatedMemories(Long projectId, String query, int topK);

    /**
     * 获取未完结伏笔
     *
     * @param projectId 项目ID
     * @return 伏笔列表
     */
    List<StoryMemoryVO> getPendingForeshadows(Long projectId);

    /**
     * 获取角色状态记忆
     *
     * @param projectId    项目ID
     * @param characterIds 角色ID列表
     * @return 角色状态记忆列表
     */
    List<StoryMemoryVO> getCharacterStates(Long projectId, List<Long> characterIds);

    /**
     * 标记伏笔为已回收
     *
     * @param memoryId            记忆ID
     * @param resolvedAtChapterId 回收章节ID
     */
    void resolveForeshadow(Long memoryId, Long resolvedAtChapterId);

    /**
     * 删除章节的所有记忆
     *
     * @param chapterId 章节ID
     */
    void deleteByChapter(Long chapterId);
}