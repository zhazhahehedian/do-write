package com.dpbug.server.service.novel.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dpbug.common.constant.NovelConstants;
import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.server.mapper.novel.GenerationTaskMapper;
import com.dpbug.server.model.entity.novel.NovelGenerationTask;
import com.dpbug.server.model.vo.novel.GenerationTaskVO;
import com.dpbug.server.service.novel.GenerationTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 生成任务服务实现类
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationTaskServiceImpl implements GenerationTaskService {

    private final GenerationTaskMapper taskMapper;

    @Override
    public NovelGenerationTask createTask(Long userId, Long projectId, String taskType, Map<String, Object> params) {
        NovelGenerationTask task = new NovelGenerationTask();
        task.setUserId(userId);
        task.setProjectId(projectId);
        task.setTaskType(taskType);
        task.setStatus(NovelConstants.TaskStatus.PENDING);
        task.setParams(params);
        task.setProgress(0);
        task.setCurrentStep("等待开始");

        taskMapper.insert(task);
        log.info("创建生成任务: userId={}, projectId={}, taskType={}, taskId={}",
                userId, projectId, taskType, task.getId());

        return task;
    }

    @Override
    public void updateProgress(Long taskId, Integer progress, String currentStep) {
        taskMapper.updateProgress(taskId, progress, currentStep);
    }

    @Override
    public void completeTask(Long taskId, Map<String, Object> result) {
        NovelGenerationTask task = new NovelGenerationTask();
        task.setId(taskId);
        task.setStatus(NovelConstants.TaskStatus.COMPLETED);
        task.setProgress(100);
        task.setCurrentStep("已完成");
        task.setResult(result);
        task.setCompletedAt(LocalDateTime.now());

        taskMapper.updateById(task);
        log.info("任务完成: taskId={}", taskId);
    }

    @Override
    public void failTask(Long taskId, String errorMessage) {
        NovelGenerationTask task = new NovelGenerationTask();
        task.setId(taskId);
        task.setStatus(NovelConstants.TaskStatus.FAILED);
        task.setCurrentStep("失败");
        task.setErrorMessage(errorMessage);
        task.setCompletedAt(LocalDateTime.now());

        taskMapper.updateById(task);
        log.error("任务失败: taskId={}, error={}", taskId, errorMessage);
    }

    @Override
    public GenerationTaskVO getTask(Long userId, Long taskId) {
        NovelGenerationTask task = taskMapper.selectById(taskId);

        if (task == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "任务不存在");
        }

        // 检查权限
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该任务");
        }

        return convertToVO(task);
    }

    @Override
    public List<GenerationTaskVO> getRunningTasks(Long userId) {
        List<NovelGenerationTask> tasks = taskMapper.selectRunningTasks(userId);

        return tasks.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public void cancelTask(Long userId, Long taskId) {
        NovelGenerationTask task = taskMapper.selectById(taskId);

        if (task == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "任务不存在");
        }

        // 检查权限
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该任务");
        }

        // 只能取消待执行或执行中的任务
        if (!NovelConstants.TaskStatus.PENDING.equals(task.getStatus())
                && !NovelConstants.TaskStatus.RUNNING.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该任务无法取消");
        }

        // 更新任务状态
        LambdaUpdateWrapper<NovelGenerationTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(NovelGenerationTask::getId, taskId)
                .set(NovelGenerationTask::getStatus, NovelConstants.TaskStatus.CANCELLED)
                .set(NovelGenerationTask::getCurrentStep, "已取消")
                .set(NovelGenerationTask::getCompletedAt, LocalDateTime.now());

        taskMapper.update(null, updateWrapper);
        log.info("取消任务: userId={}, taskId={}", userId, taskId);
    }

    /**
     * 转换为VO
     */
    private GenerationTaskVO convertToVO(NovelGenerationTask task) {
        GenerationTaskVO vo = new GenerationTaskVO();
        BeanUtils.copyProperties(task, vo);

        // 计算章节统计
        if (task.getChapterIds() != null) {
            vo.setTotalChapters(task.getChapterIds().size());

            // 从结果中获取完成数量
            if (task.getResult() != null && task.getResult().containsKey("completedChapters")) {
                vo.setCompletedChapters((Integer) task.getResult().get("completedChapters"));
            } else {
                // 根据进度估算
                vo.setCompletedChapters(task.getProgress() * task.getChapterIds().size() / 100);
            }
        }

        return vo;
    }
}