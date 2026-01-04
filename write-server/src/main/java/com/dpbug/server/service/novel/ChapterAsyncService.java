package com.dpbug.server.service.novel;

import com.dpbug.server.model.dto.novel.BatchGenerateRequest;
import com.dpbug.server.model.entity.novel.NovelGenerationTask;

/**
 * 章节异步服务接口
 *
 * <p>将异步方法抽取到单独的接口中，解决 @Async 在同类内部调用无效的问题</p>
 *
 * @author dpbug
 */
public interface ChapterAsyncService {

    /**
     * 异步提取章节记忆
     *
     * @param userId        用户ID（用于获取AI配置）
     * @param projectId     项目ID
     * @param chapterId     章节ID
     * @param chapterNumber 章节序号
     * @param content       章节内容
     */
    void asyncExtractMemories(Long userId, Long projectId, Long chapterId, Integer chapterNumber, String content);

    /**
     * 异步生成章节摘要
     *
     * @param chapterId 章节ID
     * @param content   章节内容
     */
    void asyncGenerateSummary(Long chapterId, String content);

    /**
     * 异步执行批量章节生成
     *
     * @param userId  用户ID
     * @param task    任务实体
     * @param request 批量生成请求
     */
    void asyncBatchGenerate(Long userId, NovelGenerationTask task, BatchGenerateRequest request);
}
