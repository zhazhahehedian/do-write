package com.dpbug.server.service.novel.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.dpbug.common.constant.NovelConstants;
import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.server.model.dto.novel.ProjectUpdateRequest;
import com.dpbug.server.model.entity.novel.NovelOutline;
import com.dpbug.server.service.novel.OutlineService;
import com.dpbug.server.service.novel.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向导事务辅助类
 * <p>
 * 用于处理流式生成完成后的事务性数据库操作。
 * 由于 doOnComplete 回调不在 Spring 事务管理范围内，
 * 需要通过独立的 Bean 来确保事务的正确性。
 * </p>
 *
 * @author dpbug
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WizardTransactionHelper {

    private final ProjectService projectService;
    private final OutlineService outlineService;

    /**
     * 保存世界观并更新向导状态（事务性操作）
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param content   AI生成的世界观JSON内容
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveWorldAndUpdateStatus(Long userId, Long projectId, String content) {
        try {
            // 解析并保存世界观
            String cleanContent = cleanJsonContent(content);
            JSONObject jsonObject = JSON.parseObject(cleanContent);

            ProjectUpdateRequest updateRequest = new ProjectUpdateRequest();
            updateRequest.setId(projectId);
            updateRequest.setWorldTimePeriod(jsonObject.getString("time_period"));
            updateRequest.setWorldLocation(jsonObject.getString("location"));
            updateRequest.setWorldAtmosphere(jsonObject.getString("atmosphere"));
            updateRequest.setWorldRules(jsonObject.getString("rules"));

            projectService.update(userId, updateRequest);

            // 更新向导状态
            projectService.updateWizardStatus(userId, projectId,
                    NovelConstants.WizardStatus.IN_PROGRESS,
                    NovelConstants.WizardStep.WORLD);

            log.info("世界观保存成功: projectId={}", projectId);
        } catch (Exception e) {
            log.error("保存世界观失败: projectId={}, content={}", projectId, content, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "世界观保存失败: " + e.getMessage());
        }
    }

    /**
     * 保存大纲并更新向导状态（事务性操作）
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param content   AI生成的大纲JSON内容
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOutlinesAndUpdateStatus(Long userId, Long projectId, String content) {
        try {
            String cleanContent = cleanJsonContent(content);
            JSONArray jsonArray = JSON.parseArray(cleanContent);

            List<NovelOutline> outlines = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject outlineJson = jsonArray.getJSONObject(i);
                NovelOutline outline = new NovelOutline();
                outline.setProjectId(projectId);

                Integer chapterNumber = outlineJson.getInteger("chapter_number");
                outline.setOrderIndex(chapterNumber != null ? chapterNumber : i + 1);
                outline.setTitle(outlineJson.getString("title"));
                outline.setContent(outlineJson.getString("summary"));

                // 构建结构化数据
                Map<String, Object> structure = new HashMap<>();
                JSONArray scenes = outlineJson.getJSONArray("scenes");
                if (scenes != null) {
                    structure.put("scenes", scenes.toJavaList(String.class));
                }
                JSONArray characters = outlineJson.getJSONArray("characters");
                if (characters != null) {
                    structure.put("characters", characters.toJavaList(String.class));
                }
                JSONArray keyPoints = outlineJson.getJSONArray("key_points");
                if (keyPoints != null) {
                    structure.put("key_points", keyPoints.toJavaList(String.class));
                }
                structure.put("emotion", outlineJson.getString("emotion"));
                structure.put("goal", outlineJson.getString("goal"));

                outline.setStructure(structure);
                outlines.add(outline);
            }

            // 删除旧大纲并创建新大纲
            outlineService.deleteByProject(userId, projectId);
            outlineService.createBatch(userId, projectId, outlines);

            // 更新向导状态为已完成
            projectService.updateWizardStatus(userId, projectId,
                    NovelConstants.WizardStatus.COMPLETED,
                    NovelConstants.WizardStep.OUTLINES);

            // 更新项目状态为 writing
            ProjectUpdateRequest updateRequest = new ProjectUpdateRequest();
            updateRequest.setId(projectId);
            updateRequest.setStatus(NovelConstants.ProjectStatus.WRITING);
            projectService.update(userId, updateRequest);

            log.info("大纲保存成功: projectId={}, count={}", projectId, outlines.size());
        } catch (Exception e) {
            log.error("保存大纲失败: projectId={}, content={}", projectId, content, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "大纲保存失败: " + e.getMessage());
        }
    }

    /**
     * 清理JSON内容中的markdown标记
     */
    private String cleanJsonContent(String content) {
        if (content == null) {
            return "{}";
        }
        String cleaned = content.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}
