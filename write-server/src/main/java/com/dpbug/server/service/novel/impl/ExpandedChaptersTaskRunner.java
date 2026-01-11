package com.dpbug.server.service.novel.impl;

import com.dpbug.common.constant.NovelConstants;
import com.dpbug.server.mapper.novel.ChapterMapper;
import com.dpbug.server.mapper.novel.GenerationTaskMapper;
import com.dpbug.server.model.dto.novel.ChapterGenerateRequest;
import com.dpbug.server.model.dto.novel.ExpandedChaptersGenerateRequest;
import com.dpbug.server.model.entity.novel.NovelChapter;
import com.dpbug.server.model.entity.novel.NovelGenerationTask;
import com.dpbug.server.service.novel.ChapterService;
import com.dpbug.server.service.novel.GenerationTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一纲多章：已展开子章节批量生成任务执行器
 *
 * <p>独立于 ChapterAsyncService，避免与 ChapterService 形成循环依赖</p>
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpandedChaptersTaskRunner {

    private final ChapterService chapterService;
    private final GenerationTaskService taskService;
    private final GenerationTaskMapper taskMapper;
    private final ChapterMapper chapterMapper;

    @Async("chapterExecutor")
    public void run(Long userId,
                    Long taskId,
                    Long projectId,
                    Long outlineId,
                    List<Long> chapterIds,
                    ExpandedChaptersGenerateRequest request) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return;
        }

        int total = chapterIds.size();
        int completed = 0;
        int failed = 0;

        taskService.updateProgress(taskId, 0, "开始批量生成子章节");

        for (int i = 0; i < chapterIds.size(); i++) {
            // 检查是否被取消
            NovelGenerationTask task = taskMapper.selectById(taskId);
            if (task == null) {
                return;
            }
            if (NovelConstants.TaskStatus.CANCELLED.equals(task.getStatus())) {
                log.info("任务已取消，停止执行: taskId={}", taskId);
                return;
            }

            Long chapterId = chapterIds.get(i);
            int progress = (i * 100) / total;
            taskService.updateProgress(taskId, progress,
                    String.format("正在生成子章节 %d/%d", i + 1, total));

            try {
                NovelChapter chapter = chapterMapper.selectById(chapterId);
                if (chapter == null) {
                    failed++;
                    continue;
                }
                if (!outlineId.equals(chapter.getOutlineId())
                        || chapter.getSubIndex() == null
                        || chapter.getSubIndex() <= 0) {
                    failed++;
                    continue;
                }

                ChapterGenerateRequest generateRequest = new ChapterGenerateRequest();
                generateRequest.setProjectId(projectId);
                generateRequest.setOutlineId(outlineId);
                generateRequest.setSubIndex(chapter.getSubIndex());
                generateRequest.setStyleCode(request.getStyleCode() != null ? request.getStyleCode() : chapter.getStyleCode());
                generateRequest.setTargetWordCount(request.getTargetWordCount());
                generateRequest.setNarrativePerspective(request.getNarrativePerspective());
                generateRequest.setCustomRequirements(request.getCustomRequirements());
                generateRequest.setTemperature(request.getTemperature());
                generateRequest.setTopP(request.getTopP());
                generateRequest.setEnableMemoryRetrieval(request.getEnableMemoryRetrieval());

                // 复用单章生成闭环：章节记录会被复用并在完成/失败时正确落库
                chapterService.generateChapter(userId, generateRequest).then().block();
                completed++;
            } catch (Exception e) {
                failed++;
                log.error("生成子章节失败: taskId={}, chapterId={}", taskId, chapterId, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("outlineId", outlineId);
        result.put("totalChapters", total);
        result.put("completedChapters", completed);
        result.put("failedChapters", failed);

        if (completed == 0) {
            taskService.failTask(taskId, "所有子章节生成失败");
        } else {
            taskService.completeTask(taskId, result);
        }

        log.info("批量生成子章节完成: taskId={}, completed={}, failed={}", taskId, completed, failed);
    }
}

