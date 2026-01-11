package com.dpbug.server.service.novel.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.server.ai.ChatClientFactory;
import com.dpbug.server.ai.prompt.PromptTemplates;
import com.dpbug.server.mapper.novel.ChapterMapper;
import com.dpbug.server.mapper.novel.PlotAnalysisMapper;
import com.dpbug.server.model.entity.novel.NovelChapter;
import com.dpbug.server.model.entity.novel.NovelPlotAnalysis;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.vo.novel.PlotAnalysisVO;
import com.dpbug.server.service.novel.PlotAnalysisService;
import com.dpbug.server.service.novel.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 剧情分析服务实现（参考 learn1：非流式 JSON 输出 + 清洗解析 + 落库）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlotAnalysisServiceImpl implements PlotAnalysisService {

    private static final int CONTENT_TRUNCATE_LENGTH = 8000;
    private static final int MIN_TOKENS_FOR_ANALYSIS = 8000;

    private final PlotAnalysisMapper plotAnalysisMapper;
    private final ChapterMapper chapterMapper;
    private final ProjectService projectService;
    private final ChatClientFactory chatClientFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlotAnalysisVO analyzeChapter(Long userId, Long chapterId, Boolean force) {
        boolean normalizedForce = Boolean.TRUE.equals(force);
        NovelChapter chapter = getChapterWithPermissionCheck(userId, chapterId);

        NovelPlotAnalysis existing = getEntityByChapterId(chapterId);
        if (existing != null && !normalizedForce) {
            return convertToVO(existing);
        }

        String content = chapter.getContent();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "章节内容为空，无法分析");
        }

        NovelProject project = projectService.checkOwnership(userId, chapter.getProjectId());
        String truncated = content.length() > CONTENT_TRUNCATE_LENGTH ? content.substring(0, CONTENT_TRUNCATE_LENGTH) : content;

        String prompt = PromptTemplates.PLOT_ANALYSIS
                .replace("{chapter_number}", String.valueOf(chapter.getChapterNumber() == null ? 0 : chapter.getChapterNumber()))
                .replace("{title}", nullToEmpty(chapter.getTitle()))
                .replace("{word_count}", String.valueOf(chapter.getWordCount() == null ? content.length() : chapter.getWordCount()))
                .replace("{content}", truncated);

        ChatClient chatClient = chatClientFactory.createForUserWithMinTokens(userId, MIN_TOKENS_FOR_ANALYSIS);
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        JSONObject root = parseStrictJsonObject(response);
        NovelPlotAnalysis analysis = buildEntityFromJson(project.getId(), chapterId, userId, root);

        if (existing != null) {
            analysis.setId(existing.getId());
            plotAnalysisMapper.updateById(analysis);
            return convertToVO(plotAnalysisMapper.selectById(existing.getId()));
        }

        plotAnalysisMapper.insert(analysis);
        return convertToVO(plotAnalysisMapper.selectById(analysis.getId()));
    }

    @Override
    public PlotAnalysisVO getByChapterId(Long userId, Long chapterId) {
        NovelChapter chapter = getChapterWithPermissionCheck(userId, chapterId);
        NovelPlotAnalysis analysis = getEntityByChapterId(chapter.getId());
        if (analysis == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "该章节暂无分析报告");
        }
        return convertToVO(analysis);
    }

    private NovelPlotAnalysis getEntityByChapterId(Long chapterId) {
        LambdaQueryWrapper<NovelPlotAnalysis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelPlotAnalysis::getChapterId, chapterId);
        return plotAnalysisMapper.selectOne(wrapper);
    }

    private NovelChapter getChapterWithPermissionCheck(Long userId, Long chapterId) {
        NovelChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "章节不存在");
        }
        projectService.checkOwnership(userId, chapter.getProjectId());
        return chapter;
    }

    private NovelPlotAnalysis buildEntityFromJson(Long projectId, Long chapterId, Long userId, JSONObject root) {
        NovelPlotAnalysis entity = new NovelPlotAnalysis();
        entity.setProjectId(projectId);
        entity.setChapterId(chapterId);
        entity.setAiModel(chatClientFactory.getCurrentModelName(userId));

        entity.setPlotStage(readString(root, "plotStage", "plot_stage", "plotStageName"));

        JSONObject conflict = readObject(root, "conflict");
        entity.setConflictLevel(readInt(conflict, "level", "conflictLevel"));
        entity.setConflictTypes(readStringList(conflict, "types", "conflictTypes"));

        JSONObject emotional = readObject(root, "emotional");
        entity.setEmotionalTone(readString(emotional, "tone", "emotionalTone"));
        entity.setEmotionalIntensity(readBigDecimal(emotional, "intensity", "emotionalIntensity"));
        entity.setEmotionalCurve(emotional != null ? emotional.get("curve") : null);

        List<Map<String, Object>> hooks = readHookList(root);
        entity.setHooks(hooks);
        entity.setHooksCount(hooks == null ? 0 : hooks.size());
        entity.setHooksAvgStrength(calculateHooksAvgStrength(hooks));

        JSONObject foreshadows = readObject(root, "foreshadows");
        entity.setForeshadows(foreshadows);
        entity.setForeshadowsPlanted(countArray(foreshadows, "planted"));
        entity.setForeshadowsResolved(countArray(foreshadows, "resolved"));

        JSONObject scores = readObject(root, "scores");
        entity.setPacingScore(readBigDecimal(scores, "pacing"));
        entity.setEngagementScore(readBigDecimal(scores, "engagement"));
        entity.setCoherenceScore(readBigDecimal(scores, "coherence"));
        entity.setOverallQualityScore(readBigDecimal(scores, "overall"));

        entity.setSuggestions(readStringList(root, "suggestions"));
        entity.setAnalysisReport(readString(root, "analysisReport", "analysis_report", "report"));

        // 兜底：如果 AI 未给出 analysisReport，则用 JSON 生成一个简要报告，避免前端空白
        if (!StringUtils.hasText(entity.getAnalysisReport())) {
            entity.setAnalysisReport(buildFallbackReport(entity));
        }

        return entity;
    }

    private String buildFallbackReport(NovelPlotAnalysis entity) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 剧情分析（简要）\n\n");
        if (StringUtils.hasText(entity.getPlotStage())) {
            sb.append("- 剧情阶段：").append(entity.getPlotStage()).append("\n");
        }
        if (entity.getConflictLevel() != null) {
            sb.append("- 冲突等级：").append(entity.getConflictLevel()).append("/10\n");
        }
        if (entity.getHooksCount() != null) {
            sb.append("- 钩子数量：").append(entity.getHooksCount()).append("\n");
        }
        if (entity.getOverallQualityScore() != null) {
            sb.append("- 整体评分：").append(entity.getOverallQualityScore()).append("/10\n");
        }
        return sb.toString();
    }

    private PlotAnalysisVO convertToVO(NovelPlotAnalysis entity) {
        PlotAnalysisVO vo = new PlotAnalysisVO();
        vo.setId(entity.getId());
        vo.setProjectId(entity.getProjectId());
        vo.setChapterId(entity.getChapterId());
        vo.setPlotStage(entity.getPlotStage());
        vo.setConflictLevel(entity.getConflictLevel());
        vo.setConflictTypes(entity.getConflictTypes());
        vo.setEmotionalTone(entity.getEmotionalTone());
        vo.setEmotionalIntensity(entity.getEmotionalIntensity());
        vo.setEmotionalCurve(entity.getEmotionalCurve());
        vo.setHooks(entity.getHooks());
        vo.setHooksCount(entity.getHooksCount());
        vo.setHooksAvgStrength(entity.getHooksAvgStrength());
        vo.setForeshadows(entity.getForeshadows());
        vo.setForeshadowsPlanted(entity.getForeshadowsPlanted());
        vo.setForeshadowsResolved(entity.getForeshadowsResolved());
        vo.setOverallQualityScore(entity.getOverallQualityScore());
        vo.setPacingScore(entity.getPacingScore());
        vo.setEngagementScore(entity.getEngagementScore());
        vo.setCoherenceScore(entity.getCoherenceScore());
        vo.setAnalysisReport(entity.getAnalysisReport());
        vo.setSuggestions(entity.getSuggestions());
        vo.setAiModel(entity.getAiModel());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private JSONObject parseStrictJsonObject(String response) {
        if (!StringUtils.hasText(response)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 返回为空，无法解析分析结果");
        }
        String json = cleanMarkdownCodeBlock(response);
        try {
            JSONObject obj = JSON.parseObject(json);
            if (obj == null || obj.isEmpty()) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "AI 返回的 JSON 为空对象");
            }
            return obj;
        } catch (Exception e) {
            log.warn("解析剧情分析 JSON 失败，原始内容(截断)：{}", truncateForLog(response), e);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "解析 AI 分析结果失败，请重试或调整提示词");
        }
    }

    private String cleanMarkdownCodeBlock(String text) {
        String cleaned = text.trim();
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

    private String truncateForLog(String text) {
        if (text == null) {
            return "";
        }
        int max = 500;
        String t = text.replace("\n", " ").replace("\r", " ");
        return t.length() > max ? t.substring(0, max) + "..." : t;
    }

    private BigDecimal calculateHooksAvgStrength(List<Map<String, Object>> hooks) {
        if (hooks == null || hooks.isEmpty()) {
            return null;
        }
        int count = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> h : hooks) {
            Object strength = h.get("strength");
            if (strength == null) {
                continue;
            }
            try {
                BigDecimal s = new BigDecimal(String.valueOf(strength));
                sum = sum.add(s);
                count++;
            } catch (Exception ignore) {
                // 忽略非数值
            }
        }
        if (count == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> readHookList(JSONObject root) {
        if (root == null) {
            return List.of();
        }
        Object hooksObj = root.get("hooks");
        if (hooksObj == null) {
            return List.of();
        }
        if (hooksObj instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        if (hooksObj instanceof JSONArray array) {
            @SuppressWarnings("rawtypes")
            List<Map> raw = array.toJavaList(Map.class);
            return raw.stream()
                    .filter(Objects::nonNull)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    private int countArray(JSONObject obj, String key) {
        if (obj == null) {
            return 0;
        }
        JSONArray array = obj.getJSONArray(key);
        return array == null ? 0 : array.size();
    }

    private String readString(JSONObject obj, String... keys) {
        if (obj == null) {
            return null;
        }
        for (String k : keys) {
            String v = obj.getString(k);
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return null;
    }

    private Integer readInt(JSONObject obj, String... keys) {
        if (obj == null) {
            return null;
        }
        for (String k : keys) {
            Object v = obj.get(k);
            if (v == null) {
                continue;
            }
            try {
                return Integer.valueOf(String.valueOf(v));
            } catch (Exception ignore) {
                // ignore
            }
        }
        return null;
    }

    private BigDecimal readBigDecimal(JSONObject obj, String... keys) {
        if (obj == null) {
            return null;
        }
        for (String k : keys) {
            Object v = obj.get(k);
            if (v == null) {
                continue;
            }
            try {
                return new BigDecimal(String.valueOf(v));
            } catch (Exception ignore) {
                // ignore
            }
        }
        return null;
    }

    private JSONObject readObject(JSONObject obj, String key) {
        if (obj == null) {
            return null;
        }
        Object v = obj.get(key);
        if (v instanceof JSONObject json) {
            return json;
        }
        if (v instanceof Map<?, ?> map) {
            return new JSONObject(map);
        }
        return null;
    }

    private List<String> readStringList(JSONObject obj, String... keys) {
        if (obj == null) {
            return null;
        }
        for (String key : keys) {
            JSONArray array = obj.getJSONArray(key);
            if (array != null) {
                return array.toJavaList(String.class);
            }
        }
        return null;
    }

    private String nullToEmpty(String str) {
        return str != null ? str : "";
    }
}
