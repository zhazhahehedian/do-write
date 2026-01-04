package com.dpbug.server.service.novel;

import com.dpbug.server.model.entity.novel.NovelGenerationTask;
import com.dpbug.server.model.vo.novel.GenerationTaskVO;

import java.util.List;
import java.util.Map;

/**
 * 生成任务服务接口
 *
 * @author dpbug
 */
public interface GenerationTaskService {

    /**
     * 创建任务
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param taskType  任务类型
     * @param params    任务参数
     * @return 任务实体
     */
    NovelGenerationTask createTask(Long userId, Long projectId, String taskType, Map<String, Object> params);

    /**
     * 更新任务进度
     *
     * @param taskId      任务ID
     * @param progress    进度
     * @param currentStep 当前步骤
     */
    void updateProgress(Long taskId, Integer progress, String currentStep);

    /**
     * 标记任务完成
     *
     * @param taskId 任务ID
     * @param result 任务结果
     */
    void completeTask(Long taskId, Map<String, Object> result);

    /**
     * 标记任务失败
     *
     * @param taskId       任务ID
     * @param errorMessage 错误信息
     */
    void failTask(Long taskId, String errorMessage);

    /**
     * 获取任务详情
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 任务详情
     */
    GenerationTaskVO getTask(Long userId, Long taskId);

    /**
     * 获取用户进行中的任务
     *
     * @param userId 用户ID
     * @return 任务列表
     */
    List<GenerationTaskVO> getRunningTasks(Long userId);

    /**
     * 取消任务
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     */
    void cancelTask(Long userId, Long taskId);
}