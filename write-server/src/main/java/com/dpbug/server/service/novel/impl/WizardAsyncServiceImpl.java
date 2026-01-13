package com.dpbug.server.service.novel.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.dpbug.common.constant.NovelConstants;
import com.dpbug.server.ai.ChatClientFactory;
import com.dpbug.server.ai.prompt.PromptTemplates;
import com.dpbug.server.model.dto.novel.CharacterGenerateRequest;
import com.dpbug.server.model.dto.novel.OutlineGenerateRequest;
import com.dpbug.server.model.entity.novel.NovelCharacter;
import com.dpbug.server.model.entity.novel.NovelGenerationTask;
import com.dpbug.server.model.entity.novel.NovelOutline;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.vo.novel.CharacterVO;
import com.dpbug.server.service.novel.CharacterService;
import com.dpbug.server.service.novel.GenerationTaskService;
import com.dpbug.server.service.novel.OutlineService;
import com.dpbug.server.service.novel.ProjectService;
import com.dpbug.server.service.novel.WizardAsyncService;
import com.dpbug.server.util.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向导异步服务实现类
 *
 * <p>将异步方法抽取到单独的类中，确保 Spring AOP 能够正确拦截 @Async 方法</p>
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WizardAsyncServiceImpl implements WizardAsyncService {

    private final ProjectService projectService;
    private final CharacterService characterService;
    private final OutlineService outlineService;
    private final ChatClientFactory chatClientFactory;
    private final GenerationTaskService taskService;
    private final RedisLockUtil redisLockUtil;

    /**
     * 异步任务调用 AI 的超时时间：避免任务线程无限阻塞。
     */
    private static final Duration ASYNC_AI_TIMEOUT = Duration.ofMinutes(10);

    private String callAiByStreaming(ChatClient chatClient, String systemPrompt, String userPrompt) {
        Mono<String> responseMono = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .collectList()
                .map(list -> String.join("", list))
                .timeout(ASYNC_AI_TIMEOUT);

        String response = responseMono.block(ASYNC_AI_TIMEOUT);
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("AI 返回内容为空");
        }
        return response;
    }

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void asyncGenerateCharacters(Long userId, NovelGenerationTask task, CharacterGenerateRequest request, String lockKey) {
        Long taskId = task.getId();
        Long projectId = request.getProjectId();

        try {
            log.info("开始异步生成角色: taskId={}, projectId={}", taskId, projectId);

            // 更新任务状态为执行中
            taskService.updateProgress(taskId, 10, "正在准备生成...");

            // 获取项目信息
            NovelProject project = projectService.checkOwnership(userId, projectId);

            // 更新进度
            taskService.updateProgress(taskId, 20, "正在调用AI生成角色...");

            // 使用复杂任务的ChatClient（更高的maxTokens）
            ChatClient chatClient = chatClientFactory.createForComplexTask(userId);

            String systemPrompt = "你是一位专业的小说角色设计师。";
            String userPrompt = buildCharacterPrompt(project, request);

            int totalCount = getTotalCharacterCount(request);
            log.info("调用AI生成角色: taskId={}, count={}", taskId, totalCount);

            // 调用AI生成：异步任务同样使用流式请求，降低网关超时(504)概率
            String response = callAiByStreaming(chatClient, systemPrompt, userPrompt);

            // 更新进度
            taskService.updateProgress(taskId, 70, "正在解析角色数据...");

            // 解析JSON
            List<NovelCharacter> characters = parseCharacters(response, projectId);

            // 更新进度
            taskService.updateProgress(taskId, 85, "正在保存角色...");

            // 删除旧角色并创建新角色
            characterService.deleteByProject(userId, projectId);
            characterService.createBatch(userId, projectId, characters);

            // 更新向导状态
            projectService.updateWizardStatus(userId, projectId,
                    NovelConstants.WizardStatus.IN_PROGRESS,
                    NovelConstants.WizardStep.CHARACTERS);

            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("characterCount", characters.size());
            result.put("projectId", projectId);

            // 标记任务完成
            taskService.completeTask(taskId, result);

            log.info("异步角色生成完成: taskId={}, count={}", taskId, characters.size());

        } catch (Exception e) {
            log.error("异步角色生成失败: taskId={}, error={}", taskId, e.getMessage(), e);
            taskService.failTask(taskId, e.getMessage());
        } finally {
            // 无论成功还是失败都要释放锁
            if (lockKey != null) {
                redisLockUtil.unlock(lockKey);
            }
        }
    }

    private String buildCharacterPrompt(NovelProject project, CharacterGenerateRequest request) {
        int totalCount = getTotalCharacterCount(request);

        String requirements = String.format("主角%d个、配角%d个、反派%d个、组织%d个",
                request.getProtagonistCount(),
                request.getSupportingCount(),
                request.getAntagonistCount(),
                request.getOrganizationCount());

        if (StringUtils.hasText(request.getCustomRequirements())) {
            requirements = requirements + "。" + request.getCustomRequirements();
        }

        return PromptTemplates.CHARACTERS_BATCH_GENERATION
                .replace("{count}", String.valueOf(totalCount))
                .replace("{time_period}", nullToEmpty(project.getWorldTimePeriod()))
                .replace("{location}", nullToEmpty(project.getWorldLocation()))
                .replace("{atmosphere}", nullToEmpty(project.getWorldAtmosphere()))
                .replace("{rules}", nullToEmpty(project.getWorldRules()))
                .replace("{theme}", nullToEmpty(project.getTheme()))
                .replace("{genre}", nullToEmpty(project.getGenre()))
                .replace("{requirements}", requirements);
    }

    private List<NovelCharacter> parseCharacters(String json, Long projectId) {
        String cleanContent = cleanJsonContent(json);
        JSONArray jsonArray = JSON.parseArray(cleanContent);

        List<NovelCharacter> result = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject charJson = jsonArray.getJSONObject(i);
            NovelCharacter character = new NovelCharacter();
            character.setProjectId(projectId);
            character.setName(charJson.getString("name"));

            // 判断是角色还是组织
            Boolean isOrg = charJson.getBoolean("is_organization");
            character.setIsOrganization(Boolean.TRUE.equals(isOrg) ? 1 : 0);

            if (Boolean.TRUE.equals(isOrg)) {
                // 组织信息
                character.setOrganizationType(charJson.getString("organization_type"));
                character.setOrganizationPurpose(charJson.getString("organization_purpose"));
                JSONArray members = charJson.getJSONArray("organization_members");
                if (members != null) {
                    character.setOrganizationMembers(members.toJavaList(String.class));
                }
                character.setPersonality(charJson.getString("personality"));
                character.setBackground(charJson.getString("background"));
                character.setAppearance(charJson.getString("appearance"));
            } else {
                // 角色信息
                character.setRoleType(charJson.getString("role_type"));
                character.setAge(charJson.getInteger("age"));
                character.setGender(charJson.getString("gender"));
                character.setAppearance(charJson.getString("appearance"));
                character.setPersonality(charJson.getString("personality"));
                character.setBackground(charJson.getString("background"));

                // 解析关系
                JSONArray relationships = charJson.getJSONArray("relationships_array");
                if (relationships != null && !relationships.isEmpty()) {
                    Map<String, Object> relMap = new HashMap<>();
                    for (int j = 0; j < relationships.size(); j++) {
                        JSONObject rel = relationships.getJSONObject(j);
                        relMap.put(rel.getString("target_character_name"), rel);
                    }
                    character.setRelationships(relMap);
                }
            }

            result.add(character);
        }

        log.debug("解析角色成功: projectId={}, count={}", projectId, result.size());
        return result;
    }

    private String cleanJsonContent(String content) {
        if (content == null) {
            return "{}";
        }
        // 去除可能的 markdown 代码块标记
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

    private int getTotalCharacterCount(CharacterGenerateRequest request) {
        return request.getProtagonistCount()
                + request.getSupportingCount()
                + request.getAntagonistCount()
                + request.getOrganizationCount();
    }

    private String nullToEmpty(String str) {
        return str != null ? str : "";
    }

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void asyncGenerateOutlines(Long userId, NovelGenerationTask task, OutlineGenerateRequest request, String lockKey) {
        Long taskId = task.getId();
        Long projectId = request.getProjectId();

        try {
            log.info("开始异步生成大纲: taskId={}, projectId={}", taskId, projectId);

            // 更新任务状态为执行中
            taskService.updateProgress(taskId, 10, "正在准备生成...");

            // 获取项目信息
            NovelProject project = projectService.checkOwnership(userId, projectId);

            // 获取角色列表
            List<CharacterVO> characters = characterService.listByProject(userId, projectId);

            // 更新进度
            taskService.updateProgress(taskId, 20, "正在调用AI生成大纲...");

            // 使用复杂任务的ChatClient（更高的maxTokens）
            ChatClient chatClient = chatClientFactory.createForComplexTask(userId);

            String systemPrompt = "你是一位资深小说大纲设计师。";
            String userPrompt = buildOutlinePrompt(project, characters, request);

            log.info("调用AI生成大纲: taskId={}, count={}", taskId, request.getOutlineCount());

            // 调用AI生成：异步任务同样使用流式请求，降低网关超时(504)概率
            String response = callAiByStreaming(chatClient, systemPrompt, userPrompt);

            // 更新进度
            taskService.updateProgress(taskId, 70, "正在解析大纲数据...");

            // 解析JSON
            List<NovelOutline> outlines = parseOutlines(response, projectId);

            // 更新进度
            taskService.updateProgress(taskId, 85, "正在保存大纲...");

            // 删除旧大纲并创建新大纲
            outlineService.deleteByProject(userId, projectId);
            outlineService.createBatch(userId, projectId, outlines);

            // 更新向导状态为已完成
            projectService.updateWizardStatus(userId, projectId,
                    NovelConstants.WizardStatus.COMPLETED,
                    NovelConstants.WizardStep.OUTLINES);

            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("outlineCount", outlines.size());
            result.put("projectId", projectId);

            // 标记任务完成
            taskService.completeTask(taskId, result);

            log.info("异步大纲生成完成: taskId={}, count={}", taskId, outlines.size());

        } catch (Exception e) {
            log.error("异步大纲生成失败: taskId={}, error={}", taskId, e.getMessage(), e);
            taskService.failTask(taskId, e.getMessage());
        } finally {
            // 无论成功还是失败都要释放锁
            if (lockKey != null) {
                redisLockUtil.unlock(lockKey);
            }
        }
    }

    private String buildOutlinePrompt(NovelProject project, List<CharacterVO> characters,
                                      OutlineGenerateRequest request) {
        return PromptTemplates.OUTLINE_CREATE
                .replace("{title}", nullToEmpty(project.getTitle()))
                .replace("{theme}", nullToEmpty(project.getTheme()))
                .replace("{genre}", nullToEmpty(project.getGenre()))
                .replace("{chapter_count}", String.valueOf(request.getOutlineCount()))
                .replace("{narrative_perspective}", nullToEmpty(project.getNarrativePerspective()))
                .replace("{target_words}", String.valueOf(project.getTargetWords() != null ? project.getTargetWords() : 100000))
                .replace("{time_period}", nullToEmpty(project.getWorldTimePeriod()))
                .replace("{location}", nullToEmpty(project.getWorldLocation()))
                .replace("{atmosphere}", nullToEmpty(project.getWorldAtmosphere()))
                .replace("{rules}", nullToEmpty(project.getWorldRules()))
                .replace("{characters_info}", formatCharacters(characters))
                .replace("{requirements}", nullToEmpty(request.getCustomRequirements()));
    }

    private String formatCharacters(List<CharacterVO> characters) {
        if (characters == null || characters.isEmpty()) {
            return "暂无角色信息";
        }

        StringBuilder sb = new StringBuilder();
        for (CharacterVO c : characters) {
            if (c.getIsOrganization() != null && c.getIsOrganization() == 1) {
                sb.append(String.format("- [组织] %s: %s\n",
                        c.getName(),
                        nullToEmpty(c.getOrganizationPurpose())));
            } else {
                sb.append(String.format("- [%s] %s (%s): %s\n",
                        translateRoleType(c.getRoleType()),
                        c.getName(),
                        c.getGender() != null ? c.getGender() : "未知",
                        nullToEmpty(c.getBackground())));
            }
        }
        return sb.toString();
    }

    private String translateRoleType(String roleType) {
        if (roleType == null) return "角色";
        return switch (roleType) {
            case "protagonist" -> "主角";
            case "supporting" -> "配角";
            case "antagonist" -> "反派";
            default -> "角色";
        };
    }

    private List<NovelOutline> parseOutlines(String json, Long projectId) {
        String cleanContent = cleanJsonContent(json);
        JSONArray jsonArray = JSON.parseArray(cleanContent);

        List<NovelOutline> result = new ArrayList<>();
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
            result.add(outline);
        }

        log.debug("解析大纲成功: projectId={}, count={}", projectId, result.size());
        return result;
    }
}
