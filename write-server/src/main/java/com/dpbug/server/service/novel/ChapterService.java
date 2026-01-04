package com.dpbug.server.service.novel;

import com.dpbug.common.domain.PageResult;
import com.dpbug.server.model.dto.novel.BatchGenerateRequest;
import com.dpbug.server.model.dto.novel.ChapterGenerateRequest;
import com.dpbug.server.model.dto.novel.ChapterPolishRequest;
import com.dpbug.server.model.dto.novel.ChapterQueryRequest;
import com.dpbug.server.model.dto.novel.ChapterUpdateRequest;
import com.dpbug.server.model.vo.novel.ChapterContextVO;
import com.dpbug.server.model.vo.novel.ChapterDetailVO;
import com.dpbug.server.model.vo.novel.ChapterVO;
import reactor.core.publisher.Flux;

/**
 * 章节服务接口
 *
 * @author dpbug
 */
public interface ChapterService {

    /**
     * 生成单个章节(流式)
     *
     * @param userId  用户ID
     * @param request 生成请求
     * @return 章节内容流
     */
    Flux<String> generateChapter(Long userId, ChapterGenerateRequest request);

    /**
     * 批量生成章节(返回任务ID)
     *
     * @param userId  用户ID
     * @param request 批量生成请求
     * @return 任务ID
     */
    Long batchGenerate(Long userId, BatchGenerateRequest request);

    /**
     * 重新生成章节(流式)
     *
     * @param userId    用户ID
     * @param chapterId 章节ID
     * @param request   生成请求
     * @return 章节内容流
     */
    Flux<String> regenerateChapter(Long userId, Long chapterId, ChapterGenerateRequest request);

    /**
     * 润色章节(流式)
     *
     * @param userId  用户ID
     * @param request 润色请求
     * @return 润色后的内容流
     */
    Flux<String> polishChapter(Long userId, ChapterPolishRequest request);

    /**
     * 获取章节详情
     *
     * @param userId    用户ID
     * @param chapterId 章节ID
     * @return 章节详情
     */
    ChapterDetailVO getDetail(Long userId, Long chapterId);

    /**
     * 获取章节列表
     *
     * @param userId      用户ID
     * @param pageRequest 查询请求
     * @return 章节列表
     */
    PageResult<ChapterVO> list(Long userId, ChapterQueryRequest pageRequest);

    /**
     * 更新章节
     *
     * @param userId  用户ID
     * @param request 更新请求
     */
    void update(Long userId, ChapterUpdateRequest request);

    /**
     * 删除章节
     *
     * @param userId    用户ID
     * @param chapterId 章节ID
     */
    void delete(Long userId, Long chapterId);

    /**
     * 获取生成上下文(调试/预览用)
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param outlineId 大纲ID
     * @return 上下文信息
     */
    ChapterContextVO getGenerationContext(Long userId, Long projectId, Long outlineId);

    /**
     * AI去味(流式)
     *
     * 将AI生成的文本改写得更像人类作家的手笔，
     * 去除AI痕迹、增加人性化表达、优化叙事节奏。
     *
     * @param userId    用户ID
     * @param chapterId 章节ID
     * @return 去味后的内容流
     */
    Flux<String> denoiseChapter(Long userId, Long chapterId);
}