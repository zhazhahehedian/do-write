package com.dpbug.server.service.novel;

import com.dpbug.server.model.dto.novel.CharacterGenerateRequest;
import com.dpbug.server.model.dto.novel.OutlineGenerateRequest;
import com.dpbug.server.model.entity.novel.NovelGenerationTask;

/**
 * 向导异步服务接口
 *
 * <p>将异步方法抽取到单独的接口中，解决 @Async 在同类内部调用无效的问题</p>
 *
 * @author dpbug
 */
public interface WizardAsyncService {

    /**
     * 异步生成角色
     *
     * @param userId  用户ID
     * @param task    任务实体
     * @param request 角色生成请求
     * @param lockKey Redis锁的key，任务完成后需要释放
     */
    void asyncGenerateCharacters(Long userId, NovelGenerationTask task, CharacterGenerateRequest request, String lockKey);

    /**
     * 异步生成大纲
     *
     * @param userId  用户ID
     * @param task    任务实体
     * @param request 大纲生成请求
     * @param lockKey Redis锁的key，任务完成后需要释放
     */
    void asyncGenerateOutlines(Long userId, NovelGenerationTask task, OutlineGenerateRequest request, String lockKey);
}
