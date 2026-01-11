package com.dpbug.server.service.novel.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.dpbug.common.constant.NovelConstants;
import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.server.ai.ChatClientFactory;
import com.dpbug.server.ai.prompt.PromptTemplates;
import com.dpbug.server.model.dto.novel.CharacterGenerateRequest;
import com.dpbug.server.model.dto.novel.OutlineGenerateRequest;
import com.dpbug.server.model.dto.novel.ProjectUpdateRequest;
import com.dpbug.server.model.dto.novel.WorldGenerateRequest;
import com.dpbug.server.model.entity.novel.NovelCharacter;
import com.dpbug.server.model.entity.novel.NovelGenerationTask;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.vo.novel.CharacterVO;
import com.dpbug.server.model.vo.novel.GenerationTaskVO;
import com.dpbug.server.service.novel.CharacterService;
import com.dpbug.server.service.novel.GenerationTaskService;
import com.dpbug.server.service.novel.OutlineService;
import com.dpbug.server.service.novel.ProjectService;
import com.dpbug.server.service.novel.WizardAsyncService;
import com.dpbug.server.service.novel.WizardService;
import com.dpbug.server.util.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 创作向导服务实现类
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WizardServiceImpl implements WizardService {

    private final ProjectService projectService;
    private final CharacterService characterService;
    private final OutlineService outlineService;
    private final ChatClientFactory chatClientFactory;
    private final WizardTransactionHelper transactionHelper;
    private final RedisLockUtil redisLockUtil;
    private final GenerationTaskService taskService;
    private final WizardAsyncService wizardAsyncService;

    /**
     * 流式生成超时时间（5分钟）
     */
    private static final Duration STREAM_TIMEOUT = Duration.ofMinutes(5);

    /**
     * 世界观生成
     * @param userId  用户ID
     * @param request 世界观生成请求
     * @return 对应世界观文字流
     */
    @Override
    public Flux<String> generateWorld(Long userId, WorldGenerateRequest request) {
        // 检查项目权限
        NovelProject project = projectService.checkOwnership(userId, request.getProjectId());

        // 防止重复提交
        String lockKey = RedisLockUtil.wizardLockKey(userId, project.getId(), "world");
        if (!redisLockUtil.tryLock(lockKey)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "正在生成中，请勿重复提交");
        }

        // 获取用户对应api的ChatClient
        ChatClient chatClient = chatClientFactory.createForUser(userId);

        String systemPrompt = "你是一位资深小说世界观设计师。";
        String userPrompt = buildWorldPrompt(project, request.getCustomRequirements());

        log.info("开始生成世界观: userId={}, projectId={}", userId, request.getProjectId());

        // 使用 final 变量确保闭包安全
        final Long finalUserId = userId;
        final Long finalProjectId = project.getId();
        final String finalLockKey = lockKey;
        StringBuilder fullContent = new StringBuilder();
        AtomicBoolean saveSucceeded = new AtomicBoolean(false);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .timeout(STREAM_TIMEOUT)
                .doOnNext(fullContent::append)
                .doOnComplete(() -> {
                    try {
                        // 使用事务辅助类保存数据
                        transactionHelper.saveWorldAndUpdateStatus(finalUserId, finalProjectId, fullContent.toString());
                        saveSucceeded.set(true);
                        log.info("世界观生成完成: projectId={}", finalProjectId);
                    } catch (Exception e) {
                        log.error("世界观保存失败: projectId={}, error={}", finalProjectId, e.getMessage(), e);
                        // 异常会在 concatWith 中通过 error 事件发送给前端
                    }
                })
                .doOnError(e -> {
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        log.error("世界观生成超时: projectId={}", finalProjectId);
                    } else {
                        log.error("世界观生成失败: projectId={}, error={}", finalProjectId, e.getMessage());
                    }
                })
                .doFinally(signal -> {
                    // 无论成功、失败还是取消，都释放锁
                    redisLockUtil.unlock(finalLockKey);
                })
                .concatWith(Flux.defer(() -> {
                    // 如果保存失败，返回错误信息
                    if (!saveSucceeded.get() && fullContent.length() > 0) {
                        return Flux.just("\n\n[ERROR] 数据保存失败，请重试");
                    }
                    return Flux.empty();
                }));
    }

    /**
     * 角色生成
     * @param userId  用户ID
     * @param request 角色生成请求
     * @return 角色列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CharacterVO> generateCharacters(Long userId, CharacterGenerateRequest request) {

        NovelProject project = projectService.checkOwnership(userId, request.getProjectId());

        // 校验世界观是否已生成（需要四个字段都有内容）
        if (!isWorldGenerated(project)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先生成世界观");
        }

        // 防止重复提交
        String lockKey = RedisLockUtil.wizardLockKey(userId, project.getId(), "characters");
        if (!redisLockUtil.tryLock(lockKey)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "正在生成中，请勿重复提交");
        }

        try {
            // 使用复杂任务的ChatClient（更高的maxTokens）
            ChatClient chatClient = chatClientFactory.createForComplexTask(userId);

            String systemPrompt = "你是一位专业的小说角色设计师。";
            String userPrompt = buildCharacterPrompt(project, request);

            log.info("开始生成角色: userId={}, projectId={}, count={}",
                    userId, request.getProjectId(), getTotalCharacterCount(request));

            // 调用AI生成（非流式）
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            // 解析JSON并批量保存
            List<NovelCharacter> characters = parseCharacters(response, project.getId());

            // 删除旧角色并创建新角色
            characterService.deleteByProject(userId, project.getId());
            characterService.createBatch(userId, project.getId(), characters);

            // 更新向导状态
            updateWizardStatus(userId, project.getId(),
                    NovelConstants.WizardStatus.IN_PROGRESS,
                    NovelConstants.WizardStep.CHARACTERS);

            log.info("角色生成完成: projectId={}, count={}", project.getId(), characters.size());

            return characterService.listByProject(userId, project.getId());
        } finally {
            // 释放锁
            redisLockUtil.unlock(lockKey);
        }
    }

    /**
     * 大纲生成
     * @param userId  用户ID
     * @param request 大纲生成请求
     * @return 大纲文字流
     */
    @Override
    public Flux<String> generateOutlines(Long userId, OutlineGenerateRequest request) {

        NovelProject project = projectService.checkOwnership(userId, request.getProjectId());

        // 检查角色是否已生成
        List<CharacterVO> characters = characterService.listByProject(userId, project.getId());
        if (characters.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先生成角色");
        }

        // 防止重复提交
        String lockKey = RedisLockUtil.wizardLockKey(userId, project.getId(), "outlines");
        if (!redisLockUtil.tryLock(lockKey)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "正在生成中，请勿重复提交");
        }

        ChatClient chatClient = chatClientFactory.createForUser(userId);

        String systemPrompt = "你是一位资深小说大纲设计师。";
        String userPrompt = buildOutlinePrompt(project, characters, request);

        log.info("开始生成大纲: userId={}, projectId={}, count={}",
                userId, request.getProjectId(), request.getOutlineCount());

        // 使用 final 变量确保闭包安全
        final Long finalUserId = userId;
        final Long finalProjectId = project.getId();
        final String finalLockKey = lockKey;
        StringBuilder fullContent = new StringBuilder();
        AtomicBoolean saveSucceeded = new AtomicBoolean(false);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .timeout(STREAM_TIMEOUT)
                .doOnNext(fullContent::append)
                .doOnComplete(() -> {
                    try {
                        // 使用事务辅助类保存数据
                        transactionHelper.saveOutlinesAndUpdateStatus(finalUserId, finalProjectId, fullContent.toString());
                        saveSucceeded.set(true);
                        log.info("大纲生成完成: projectId={}", finalProjectId);
                    } catch (Exception e) {
                        log.error("大纲保存失败: projectId={}, error={}", finalProjectId, e.getMessage(), e);
                    }
                })
                .doOnError(e -> {
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        log.error("大纲生成超时: projectId={}", finalProjectId);
                    } else {
                        log.error("大纲生成失败: projectId={}, error={}", finalProjectId, e.getMessage());
                    }
                })
                .doFinally(signal -> {
                    // 无论成功、失败还是取消，都释放锁
                    redisLockUtil.unlock(finalLockKey);
                })
                .concatWith(Flux.defer(() -> {
                    // 如果保存失败，返回错误信息
                    if (!saveSucceeded.get() && fullContent.length() > 0) {
                        return Flux.just("\n\n[ERROR] 数据保存失败，请重试");
                    }
                    return Flux.empty();
                }));
    }

    /**
     * 向导状态管理
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param status    向导状态（not_started/in_progress/completed）
     * @param step      向导步骤（0-3）
     */
    @Override
    public void updateWizardStatus(Long userId, Long projectId, String status, Integer step) {
        projectService.updateWizardStatus(userId, projectId, status, step);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetWizardStep(Long userId, Long projectId, Integer step) {
        // 检查项目权限
        projectService.checkOwnership(userId, projectId);

        // 根据步骤删除相应数据
        if (step <= NovelConstants.WizardStep.WORLD) {
            // 重置到世界观步骤，清空世界观数据
            ProjectUpdateRequest updateRequest = new ProjectUpdateRequest();
            updateRequest.setId(projectId);
            updateRequest.setWorldTimePeriod("");
            updateRequest.setWorldLocation("");
            updateRequest.setWorldAtmosphere("");
            updateRequest.setWorldRules("");
            projectService.update(userId, updateRequest);
        }

        if (step <= NovelConstants.WizardStep.CHARACTERS) {
            // 重置到角色步骤，删除所有角色
            characterService.deleteByProject(userId, projectId);
        }

        if (step <= NovelConstants.WizardStep.OUTLINES) {
            // 重置到大纲步骤，删除所有大纲
            outlineService.deleteByProject(userId, projectId);
        }

        // 更新向导状态
        String status = step == NovelConstants.WizardStep.INIT
                ? NovelConstants.WizardStatus.NOT_STARTED
                : NovelConstants.WizardStatus.IN_PROGRESS;

        updateWizardStatus(userId, projectId, status, step);

        log.info("重置向导步骤: projectId={}, step={}", projectId, step);
    }

    @Override
    public WizardProgressVO getWizardProgress(Long userId, Long projectId) {
        NovelProject project = projectService.checkOwnership(userId, projectId);

        boolean worldGenerated = isWorldGenerated(project);
        int characterCount = characterService.getStatistics(projectId).getTotal().intValue();
        int outlineCount = outlineService.countByProject(projectId);

        return new WizardProgressVO(
                project.getWizardStatus(),
                project.getWizardStep(),
                worldGenerated,
                characterCount,
                outlineCount
        );
    }

    @Override
    public GenerationTaskVO submitCharacterGenerationTask(Long userId, CharacterGenerateRequest request) {
        Long projectId = request.getProjectId();

        // 检查项目权限
        NovelProject project = projectService.checkOwnership(userId, projectId);

        // 校验世界观是否已生成
        if (!isWorldGenerated(project)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先生成世界观");
        }

        // 防止重复提交
        String lockKey = RedisLockUtil.wizardLockKey(userId, projectId, "characters");
        if (!redisLockUtil.tryLock(lockKey)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "正在生成中，请勿重复提交");
        }

        try {
            // 创建任务参数
            Map<String, Object> params = new HashMap<>();
            params.put("protagonistCount", request.getProtagonistCount());
            params.put("supportingCount", request.getSupportingCount());
            params.put("antagonistCount", request.getAntagonistCount());
            params.put("organizationCount", request.getOrganizationCount());
            params.put("customRequirements", request.getCustomRequirements());

            // 创建任务
            NovelGenerationTask task = taskService.createTask(
                    userId, projectId, NovelConstants.TaskType.CHARACTERS, params);

            // 异步执行角色生成（传入lockKey，由异步服务在完成后释放）
            wizardAsyncService.asyncGenerateCharacters(userId, task, request, lockKey);

            log.info("提交角色生成任务: userId={}, projectId={}, taskId={}",
                    userId, projectId, task.getId());

            // 返回任务信息
            GenerationTaskVO vo = new GenerationTaskVO();
            vo.setId(task.getId());
            vo.setProjectId(projectId);
            vo.setTaskType(task.getTaskType());
            vo.setStatus(task.getStatus());
            vo.setProgress(task.getProgress());
            vo.setCurrentStep(task.getCurrentStep());
            vo.setCreateTime(task.getCreateTime());

            return vo;
        } catch (Exception e) {
            // 发生异常时释放锁
            redisLockUtil.unlock(lockKey);
            throw e;
        }
    }

    @Override
    public GenerationTaskVO submitOutlineGenerationTask(Long userId, OutlineGenerateRequest request) {
        Long projectId = request.getProjectId();

        // 检查角色是否已生成
        List<CharacterVO> characters = characterService.listByProject(userId, projectId);
        if (characters.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先生成角色");
        }

        // 防止重复提交
        String lockKey = RedisLockUtil.wizardLockKey(userId, projectId, "outlines");
        if (!redisLockUtil.tryLock(lockKey)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "正在生成中，请勿重复提交");
        }

        try {
            // 创建任务参数
            Map<String, Object> params = new HashMap<>();
            params.put("outlineCount", request.getOutlineCount());
            params.put("customRequirements", request.getCustomRequirements());

            // 创建任务
            NovelGenerationTask task = taskService.createTask(
                    userId, projectId, NovelConstants.TaskType.OUTLINES, params);

            // 异步执行大纲生成（传入lockKey，由异步服务在完成后释放）
            wizardAsyncService.asyncGenerateOutlines(userId, task, request, lockKey);

            log.info("提交大纲生成任务: userId={}, projectId={}, taskId={}",
                    userId, projectId, task.getId());

            // 返回任务信息
            GenerationTaskVO vo = new GenerationTaskVO();
            vo.setId(task.getId());
            vo.setProjectId(projectId);
            vo.setTaskType(task.getTaskType());
            vo.setStatus(task.getStatus());
            vo.setProgress(task.getProgress());
            vo.setCurrentStep(task.getCurrentStep());
            vo.setCreateTime(task.getCreateTime());

            return vo;
        } catch (Exception e) {
            // 发生异常时释放锁
            redisLockUtil.unlock(lockKey);
            throw e;
        }
    }

    /**
     * 检查世界观是否已生成（至少有一个字段有内容）
     *
     * @param project 项目实体
     * @return true 如果世界观已生成
     */
    private boolean isWorldGenerated(NovelProject project) {
        // 只要有任一字段有内容就认为已生成
        // 因为 AI 可能生成部分字段，用户可以选择继续或重新生成
        return StringUtils.hasText(project.getWorldTimePeriod())
                || StringUtils.hasText(project.getWorldLocation())
                || StringUtils.hasText(project.getWorldAtmosphere())
                || StringUtils.hasText(project.getWorldRules());
    }

    /**
     * 提示词构建
     * @param project
     * @param customRequirements
     * @return 完整提示词
     */
    private String buildWorldPrompt(NovelProject project, String customRequirements) {
        String prompt = PromptTemplates.WORLD_BUILDING
                .replace("{title}", nullToEmpty(project.getTitle()))
                .replace("{theme}", nullToEmpty(project.getTheme()))
                .replace("{genre}", nullToEmpty(project.getGenre()))
                .replace("{description}", nullToEmpty(project.getDescription()));

        if (StringUtils.hasText(customRequirements)) {
            prompt = prompt + "\n\n# 用户自定义要求\n" + customRequirements;
        }

        return prompt;
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

    private List<NovelCharacter> parseCharacters(String json, Long projectId) {
        try {
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
        } catch (Exception e) {
            log.error("解析角色JSON失败: projectId={}, json={}", projectId, json, e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "角色解析失败，请重试");
        }
    }

    /**
     * 获取纯净返回的json数据
     * @param content
     * @return 纯净json字符串
     */
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
}
