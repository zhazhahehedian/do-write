package com.dpbug.server.service.novel;

import com.dpbug.server.model.dto.novel.CharacterGenerateRequest;
import com.dpbug.server.model.dto.novel.OutlineGenerateRequest;
import com.dpbug.server.model.dto.novel.WorldGenerateRequest;
import com.dpbug.server.model.vo.novel.CharacterVO;
import com.dpbug.server.model.vo.novel.GenerationTaskVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 创作向导服务接口
 * <p>
 * 提供分步骤的AI辅助创作流程，包括：
 * 1. 世界观生成（流式）
 * 2. 角色批量生成（非流式）
 * 3. 大纲生成（流式）
 * </p>
 *
 * @author dpbug
 */
public interface WizardService {

    /**
     * 生成世界观（流式）
     * <p>
     * 根据项目基本信息，AI流式生成世界观设定，
     * 包括时间背景、地理位置、氛围基调、世界规则。
     * </p>
     *
     * @param userId  用户ID
     * @param request 世界观生成请求
     * @return SSE流（Server-Sent Events）
     */
    Flux<String> generateWorld(Long userId, WorldGenerateRequest request);

    /**
     * 生成角色（非流式，返回结构化JSON）
     * <p>
     * 根据已生成的世界观，AI批量生成角色和组织，
     * 返回结构化的角色列表。
     * </p>
     *
     * @param userId  用户ID
     * @param request 角色生成请求
     * @return 角色列表
     */
    List<CharacterVO> generateCharacters(Long userId, CharacterGenerateRequest request);

    /**
     * 生成大纲（流式）
     * <p>
     * 根据世界观和角色信息，AI流式生成章节大纲。
     * </p>
     *
     * @param userId  用户ID
     * @param request 大纲生成请求
     * @return SSE流
     */
    Flux<String> generateOutlines(Long userId, OutlineGenerateRequest request);

    /**
     * 更新向导状态
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param status    向导状态（not_started/in_progress/completed）
     * @param step      向导步骤（0-3）
     */
    void updateWizardStatus(Long userId, Long projectId, String status, Integer step);

    /**
     * 重置向导步骤
     * <p>
     * 删除指定步骤及之后的所有生成内容，并重置向导状态。
     * </p>
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param step      要重置到的步骤
     */
    void resetWizardStep(Long userId, Long projectId, Integer step);

    /**
     * 获取向导进度摘要
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return 向导进度信息
     */
    WizardProgressVO getWizardProgress(Long userId, Long projectId);

    /**
     * 提交异步角色生成任务
     * <p>
     * 创建后台任务执行角色生成，立即返回任务信息，
     * 前端通过轮询任务状态获取结果。
     * </p>
     *
     * @param userId  用户ID
     * @param request 角色生成请求
     * @return 任务信息
     */
    GenerationTaskVO submitCharacterGenerationTask(Long userId, CharacterGenerateRequest request);

    /**
     * 提交异步大纲生成任务
     * <p>
     * 创建后台任务执行大纲生成，立即返回任务信息，
     * 前端通过轮询任务状态获取结果。
     * </p>
     *
     * @param userId  用户ID
     * @param request 大纲生成请求
     * @return 任务信息
     */
    GenerationTaskVO submitOutlineGenerationTask(Long userId, OutlineGenerateRequest request);

    /**
     * 向导进度信息
     */
    record WizardProgressVO(
            String status,
            Integer currentStep,
            Boolean worldGenerated,
            Integer characterCount,
            Integer outlineCount
    ) {}
}
