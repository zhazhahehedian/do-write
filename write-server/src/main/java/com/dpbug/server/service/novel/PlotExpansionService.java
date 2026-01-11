package com.dpbug.server.service.novel;

import com.dpbug.server.model.dto.novel.OutlineExpandApplyRequest;
import com.dpbug.server.model.dto.novel.OutlineExpandRequest;
import com.dpbug.server.model.dto.novel.ExpandedChaptersGenerateRequest;
import com.dpbug.server.model.vo.novel.ChapterVO;
import com.dpbug.server.model.vo.novel.GenerationTaskVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 大纲展开服务接口
 * <p>
 * 负责 one-to-many 模式下的大纲展开规划生成与章节记录创建
 *
 * @author dpbug
 */
public interface PlotExpansionService {

    /**
     * 展开规划预览（SSE流式）
     * <p>
     * 根据大纲内容生成N个子章节规划，不创建章节记录
     *
     * @param userId  用户ID
     * @param request 展开请求
     * @return SSE消息流
     */
    Flux<String> previewExpansion(Long userId, OutlineExpandRequest request);

    /**
     * 应用展开（创建章节记录）
     * <p>
     * 将预览生成的规划写入数据库，创建章节记录
     *
     * @param userId  用户ID
     * @param request 应用请求
     * @return 创建的章节列表
     */
    List<ChapterVO> applyExpansion(Long userId, OutlineExpandApplyRequest request);

    /**
     * 获取大纲已展开的子章节列表
     *
     * @param userId    用户ID
     * @param outlineId 大纲ID
     * @return 子章节列表
     */
    List<ChapterVO> getExpandedChapters(Long userId, Long outlineId);

    /**
     * 检查大纲是否已展开
     *
     * @param outlineId 大纲ID
     * @return 是否已展开
     */
    boolean isExpanded(Long outlineId);

    /**
     * 一纲多章：批量生成已展开子章节（后台任务）
     *
     * @param userId  用户ID
     * @param outlineId 大纲ID
     * @param request 生成参数
     * @return 任务信息
     */
    GenerationTaskVO generateExpandedChaptersAsync(Long userId, Long outlineId, ExpandedChaptersGenerateRequest request);

    /**
     * 删除大纲的展开章节（重新展开前的清理）
     *
     * @param userId    用户ID
     * @param outlineId 大纲ID
     */
    void deleteExpandedChapters(Long userId, Long outlineId);
}
