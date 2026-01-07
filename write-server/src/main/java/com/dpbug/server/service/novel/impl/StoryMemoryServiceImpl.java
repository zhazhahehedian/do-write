package com.dpbug.server.service.novel.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dpbug.common.constant.NovelConstants;
import com.dpbug.common.domain.PageResult;
import com.dpbug.server.ai.ChatClientFactory;
import com.dpbug.server.ai.ChromaVectorStoreFactory;
import com.dpbug.server.mapper.novel.ChapterMapper;
import com.dpbug.server.mapper.novel.StoryMemoryMapper;
import com.dpbug.server.model.dto.novel.StoryMemoryQueryRequest;
import com.dpbug.server.model.entity.novel.NovelChapter;
import com.dpbug.server.model.entity.novel.NovelStoryMemory;
import com.dpbug.server.model.vo.novel.MemoryStatisticsVO;
import com.dpbug.server.model.vo.novel.StoryMemoryVO;
import com.dpbug.server.service.novel.StoryMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 故事记忆服务实现类
 *
 * <p>使用 ChromaDB 作为向量数据库，通过 ChromaVectorStoreFactory 实现用户-项目级别的数据隔离。</p>
 * <p>每个用户的每个项目拥有独立的 ChromaDB Collection。</p>
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryMemoryServiceImpl implements StoryMemoryService {

    private final StoryMemoryMapper storyMemoryMapper;
    private final ChapterMapper chapterMapper;
    private final ChatClientFactory chatClientFactory;
    private final ChromaVectorStoreFactory chromaVectorStoreFactory;
    private final EmbeddingModel embeddingModel;

    private static final String MEMORY_EXTRACTION_PROMPT = """
            分析以下章节内容,提取关键记忆点。请以JSON格式返回,包含以下类型:
            - plot_point: 重要情节点
            - hook: 悬念钩子
            - foreshadow: 伏笔(需要后续回收)
            - character_event: 角色重要事件
            - location_event: 地点重要事件

            每个记忆点包含:
            - type: 记忆类型
            - title: 简短标题(10字以内)
            - content: 内容描述(50字以内)
            - importance: 重要性 0.0-1.0
            - is_foreshadow: 是否为伏笔(true/false)

            章节内容:
            {content}

            请返回JSON格式:
            {"memories": [...]}
            """;

    /**
     * 获取项目专属的 VectorStore
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return VectorStore 实例
     */
    private VectorStore getVectorStore(Long userId, Long projectId) {
        return chromaVectorStoreFactory.getVectorStore(userId, projectId);
    }

    @Override
    public List<NovelStoryMemory> extractMemories(Long userId, Long projectId, Long chapterId,
                                                   Integer chapterNumber, String content) {
        try {
            // 构建提示词，截断过长内容
            String truncatedContent = content.length() > 3000 ? content.substring(0, 3000) : content;
            String prompt = MEMORY_EXTRACTION_PROMPT.replace("{content}", truncatedContent);

            // 调用AI提取记忆
            ChatClient chatClient = chatClientFactory.createForUser(userId);
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 解析AI返回的JSON
            List<NovelStoryMemory> memories = parseMemories(response, projectId, chapterId, chapterNumber);
            log.info("章节记忆提取完成: chapterId={}, count={}", chapterId, memories.size());
            return memories;

        } catch (Exception e) {
            log.error("提取记忆失败: chapterId={}", chapterId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析AI返回的记忆JSON
     */
    private List<NovelStoryMemory> parseMemories(String json, Long projectId,
                                                  Long chapterId, Integer chapterNumber) {
        List<NovelStoryMemory> result = new ArrayList<>();
        try {
            JSONObject jsonObject = JSON.parseObject(json);
            JSONArray memoriesArray = jsonObject.getJSONArray("memories");

            if (memoriesArray == null) {
                return result;
            }

            for (int i = 0; i < memoriesArray.size(); i++) {
                JSONObject m = memoriesArray.getJSONObject(i);
                NovelStoryMemory memory = new NovelStoryMemory();
                memory.setProjectId(projectId);
                memory.setChapterId(chapterId);
                memory.setMemoryType(m.getString("type"));
                memory.setTitle(m.getString("title"));
                memory.setContent(m.getString("content"));
                memory.setImportanceScore(m.getBigDecimal("importance"));
                memory.setStoryTimeline(chapterNumber);
                memory.setIsForeshadow(m.getBooleanValue("is_foreshadow") ?
                        NovelConstants.ForeshadowStatus.PLANTED :
                        NovelConstants.ForeshadowStatus.NORMAL);
                result.add(memory);
            }
        } catch (Exception e) {
            log.warn("解析记忆JSON失败: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public void saveMemories(Long userId, Long projectId, List<NovelStoryMemory> memories) {
        if (memories == null || memories.isEmpty()) {
            return;
        }

        // 获取项目专属的 VectorStore
        VectorStore vectorStore = getVectorStore(userId, projectId);

        for (NovelStoryMemory memory : memories) {
            // 保存到MySQL
            storyMemoryMapper.insert(memory);

            // 向量化并存储到VectorStore
            try {
                String vectorId = storeToVectorStore(vectorStore, projectId, memory);
                memory.setVectorId(vectorId);
                memory.setEmbeddingModel(embeddingModel.getClass().getSimpleName());
                storyMemoryMapper.updateById(memory);
            } catch (Exception e) {
                log.warn("向量存储失败: memoryId={}", memory.getId(), e);
            }
        }

        log.info("保存记忆成功: projectId={}, count={}", projectId, memories.size());
    }

    /**
     * 存储记忆到向量数据库
     *
     * @param vectorStore 向量存储实例
     * @param projectId   项目ID
     * @param memory      记忆实体
     * @return 向量ID
     */
    private String storeToVectorStore(VectorStore vectorStore, Long projectId, NovelStoryMemory memory) {
        // 生成唯一向量ID
        String vectorId = UUID.randomUUID().toString();

        // 构建向量文档内容：标题 + 内容
        String textContent = memory.getTitle() + "\n" + memory.getContent();

        // 构建元数据，用于后续过滤检索
        Map<String, Object> metadata = Map.of(
                "project_id", projectId.toString(),
                "memory_id", memory.getId().toString(),
                "chapter_id", memory.getChapterId().toString(),
                "chapter_number", memory.getStoryTimeline(),
                "memory_type", memory.getMemoryType(),
                "importance_score", memory.getImportanceScore().doubleValue(),
                "is_foreshadow", memory.getIsForeshadow()
        );

        // 创建Document并存储
        Document document = new Document(vectorId, textContent, metadata);
        vectorStore.add(List.of(document));

        return vectorId;
    }

    @Override
    public List<StoryMemoryVO> searchRelatedMemories(Long userId, Long projectId, String query, int topK) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        try {
            // 获取项目专属的 VectorStore
            VectorStore vectorStore = getVectorStore(userId, projectId);

            // 构建过滤表达式：只检索当前项目的记忆
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            var filterExpression = builder.eq("project_id", projectId.toString()).build();

            // 执行向量相似度检索
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .similarityThreshold(NovelConstants.ChapterConfig.SIMILARITY_THRESHOLD)
                            .filterExpression(filterExpression)
                            .build()
            );

            if (results.isEmpty()) {
                // 降级方案：返回重要性高的记忆
                log.debug("向量检索无结果，使用降级方案: projectId={}", projectId);
                List<NovelStoryMemory> memories = storyMemoryMapper.selectImportantMemories(
                        projectId, new BigDecimal("0.5"), topK);
                return memories.stream()
                        .map(this::convertToVO)
                        .toList();
            }

            // 根据向量检索结果查询完整记忆
            List<Long> memoryIds = results.stream()
                    .map(doc -> Long.parseLong(doc.getMetadata().get("memory_id").toString()))
                    .toList();

            List<NovelStoryMemory> memories = storyMemoryMapper.selectByIds(memoryIds);
            return memories.stream()
                    .map(this::convertToVO)
                    .toList();

        } catch (Exception e) {
            log.error("向量检索失败，使用降级方案: projectId={}", projectId, e);
            // 降级方案：返回重要性高的记忆
            List<NovelStoryMemory> memories = storyMemoryMapper.selectImportantMemories(
                    projectId, new BigDecimal("0.5"), topK);
            return memories.stream()
                    .map(this::convertToVO)
                    .toList();
        }
    }

    @Override
    public List<StoryMemoryVO> getPendingForeshadows(Long projectId) {
        List<NovelStoryMemory> foreshadows = storyMemoryMapper.selectPendingForeshadows(projectId);
        return foreshadows.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public List<StoryMemoryVO> getCharacterStates(Long projectId, List<Long> characterIds) {
        // 查询角色相关的最近事件
        List<NovelStoryMemory> memories = storyMemoryMapper.selectByType(
                projectId, NovelConstants.MemoryType.CHARACTER_EVENT, 20);

        return memories.stream()
                .filter(m -> {
                    if (m.getRelatedCharacters() == null) {
                        return false;
                    }
                    return m.getRelatedCharacters().stream()
                            .anyMatch(characterIds::contains);
                })
                .limit(10)
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public void resolveForeshadow(Long memoryId, Long resolvedAtChapterId) {
        NovelStoryMemory memory = new NovelStoryMemory();
        memory.setId(memoryId);
        memory.setIsForeshadow(NovelConstants.ForeshadowStatus.RESOLVED);
        memory.setForeshadowResolvedAt(resolvedAtChapterId);
        storyMemoryMapper.updateById(memory);

        log.info("标记伏笔已回收: memoryId={}, resolvedAtChapterId={}", memoryId, resolvedAtChapterId);
    }

    @Override
    public void deleteByChapter(Long userId, Long projectId, Long chapterId) {
        // 查询该章节的所有记忆
        List<NovelStoryMemory> memories = storyMemoryMapper.selectByChapterId(chapterId);

        // 从向量存储删除
        List<String> vectorIds = memories.stream()
                .map(NovelStoryMemory::getVectorId)
                .filter(Objects::nonNull)
                .toList();

        if (!vectorIds.isEmpty()) {
            try {
                VectorStore vectorStore = getVectorStore(userId, projectId);
                vectorStore.delete(vectorIds);
                log.debug("向量存储删除成功: count={}", vectorIds.size());
            } catch (Exception e) {
                log.warn("向量存储删除失败: chapterId={}", chapterId, e);
            }
        }

        // 从MySQL删除
        LambdaQueryWrapper<NovelStoryMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelStoryMemory::getChapterId, chapterId);
        storyMemoryMapper.delete(wrapper);

        log.info("删除章节记忆: chapterId={}, count={}", chapterId, memories.size());
    }

    @Override
    public void deleteByProject(Long userId, Long projectId) {
        log.info("删除项目所有记忆: userId={}, projectId={}", userId, projectId);

        // 1. 删除 ChromaDB Collection（包含所有向量数据）
        boolean deleted = chromaVectorStoreFactory.deleteCollection(userId, projectId);
        if (deleted) {
            log.info("ChromaDB Collection 删除成功: userId={}, projectId={}", userId, projectId);
        }

        // 2. 从 MySQL 删除所有记忆
        LambdaQueryWrapper<NovelStoryMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelStoryMemory::getProjectId, projectId);
        int count = storyMemoryMapper.delete(wrapper);

        log.info("项目记忆删除完成: projectId={}, mysqlCount={}", projectId, count);
    }

    @Override
    public PageResult<StoryMemoryVO> listByProject(StoryMemoryQueryRequest request) {

        Page<NovelStoryMemory> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<NovelStoryMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelStoryMemory::getProjectId, request.getProjectId())
                .eq(NovelStoryMemory::getIsDeleted, 0);
        if (request.getChapterId() != null) {
            wrapper.eq(NovelStoryMemory::getChapterId, request.getChapterId());
        }
        if (StringUtils.isNotEmpty(request.getMemoryType())) {
            wrapper.eq(NovelStoryMemory::getMemoryType, request.getMemoryType());
        }
        if (request.getForeshadowStatus() != null) {
            wrapper.eq(NovelStoryMemory::getIsForeshadow, request.getForeshadowStatus());
        }
        if (request.getMinImportance() != null) {
            wrapper.ge(NovelStoryMemory::getImportanceScore, request.getMinImportance());
        }
        if (request.getStartTimeline() != null && request.getEndTimeline() != null) {
            wrapper.between(NovelStoryMemory::getStoryTimeline,
                    request.getStartTimeline(), request.getEndTimeline());
        }
        wrapper.orderByDesc(NovelStoryMemory::getStoryTimeline)
                .orderByDesc(NovelStoryMemory::getImportanceScore);
        Page<NovelStoryMemory> resultPage = storyMemoryMapper.selectPage(page, wrapper);


        List<StoryMemoryVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();

        return PageResult.of(
                request.getPageNum(),
                request.getPageSize(),
                resultPage.getTotal(),
                voList
        );
    }

    @Override
    public List<StoryMemoryVO> listByChapter(Long chapterId) {
        List<NovelStoryMemory> memories = storyMemoryMapper.selectByChapterId(chapterId);
        return memories.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public MemoryStatisticsVO getStatistics(Long projectId) {
        MemoryStatisticsVO vo = new MemoryStatisticsVO();
        vo.setProjectId(projectId);

        // 按类型统计
        List<Map<String, Object>> typeCounts = storyMemoryMapper.countByType(projectId);
        Map<String, Integer> typeCountMap = new HashMap<>();
        int total = 0;
        for (Map<String, Object> tc : typeCounts) {
            String type = (String) tc.get("memoryType");
            int count = ((Number) tc.get("count")).intValue();
            typeCountMap.put(type, count);
            total += count;
        }
        vo.setTypeCount(typeCountMap);
        vo.setTotalCount(total);

        // 伏笔统计
        LambdaQueryWrapper<NovelStoryMemory> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(NovelStoryMemory::getProjectId, projectId)
                .eq(NovelStoryMemory::getIsForeshadow, NovelConstants.ForeshadowStatus.PLANTED)
                .eq(NovelStoryMemory::getIsDeleted, 0);
        vo.setPendingForeshadowCount(Math.toIntExact(storyMemoryMapper.selectCount(pendingWrapper)));

        LambdaQueryWrapper<NovelStoryMemory> resolvedWrapper = new LambdaQueryWrapper<>();
        resolvedWrapper.eq(NovelStoryMemory::getProjectId, projectId)
                .eq(NovelStoryMemory::getIsForeshadow, NovelConstants.ForeshadowStatus.RESOLVED)
                .eq(NovelStoryMemory::getIsDeleted, 0);
        vo.setResolvedForeshadowCount(Math.toIntExact(storyMemoryMapper.selectCount(resolvedWrapper)));

        // 覆盖章节数
        LambdaQueryWrapper<NovelStoryMemory> chapterWrapper = new LambdaQueryWrapper<>();
        chapterWrapper.eq(NovelStoryMemory::getProjectId, projectId)
                .eq(NovelStoryMemory::getIsDeleted, 0)
                .select(NovelStoryMemory::getChapterId)
                .groupBy(NovelStoryMemory::getChapterId);
        vo.setCoveredChapterCount(Math.toIntExact(storyMemoryMapper.selectCount(chapterWrapper)));

        return vo;
    }

    @Override
    public List<StoryMemoryVO> listByTimelineRange(Long projectId, Integer start, Integer end) {
        List<NovelStoryMemory> memories = storyMemoryMapper.selectByTimelineRange(projectId, start, end);
        return memories.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public void reExtractMemories(Long userId, Long chapterId) {
        // 1. 直接查询章节信息
        NovelChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            log.warn("章节不存在: chapterId={}", chapterId);
            return;
        }

        // 2. 删除旧记忆
        deleteByChapter(userId, chapter.getProjectId(), chapterId);

        // 3. 重新提取
        List<NovelStoryMemory> memories = extractMemories(
                userId,
                chapter.getProjectId(),
                chapterId,
                chapter.getChapterNumber(),
                chapter.getContent()
        );

        // 4. 保存新记忆
        if (!memories.isEmpty()) {
            saveMemories(userId, chapter.getProjectId(), memories);
        }

        log.info("重新提取章节记忆完成: chapterId={}, count={}", chapterId, memories.size());
    }

    /**
     * 转换为VO
     */
    private StoryMemoryVO convertToVO(NovelStoryMemory memory) {
        StoryMemoryVO vo = new StoryMemoryVO();
        BeanUtils.copyProperties(memory, vo);
        return vo;
    }
}
