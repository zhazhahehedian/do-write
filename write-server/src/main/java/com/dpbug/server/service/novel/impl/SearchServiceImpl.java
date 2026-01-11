package com.dpbug.server.service.novel.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dpbug.server.mapper.novel.CharacterMapper;
import com.dpbug.server.mapper.novel.ChapterMapper;
import com.dpbug.server.mapper.novel.OutlineMapper;
import com.dpbug.server.mapper.novel.ProjectMapper;
import com.dpbug.server.mapper.novel.StoryMemoryMapper;
import com.dpbug.server.model.entity.novel.NovelChapter;
import com.dpbug.server.model.entity.novel.NovelCharacter;
import com.dpbug.server.model.entity.novel.NovelOutline;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.entity.novel.NovelStoryMemory;
import com.dpbug.server.model.vo.novel.GlobalSearchResultVO;
import com.dpbug.server.service.novel.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统级全局搜索实现：在“当前用户拥有的项目范围内”搜索项目、章节、大纲、角色、记忆。
 */
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ProjectMapper projectMapper;
    private final ChapterMapper chapterMapper;
    private final OutlineMapper outlineMapper;
    private final CharacterMapper characterMapper;
    private final StoryMemoryMapper storyMemoryMapper;

    @Override
    public GlobalSearchResultVO search(Long userId, String keyword, Integer limit) {
        String normalized = keyword == null ? "" : keyword.trim();
        int normalizedLimit = limit == null ? 5 : Math.max(1, Math.min(20, limit));

        if (!StringUtils.hasText(normalized)) {
            GlobalSearchResultVO empty = new GlobalSearchResultVO();
            empty.setProjects(List.of());
            empty.setChapters(List.of());
            empty.setOutlines(List.of());
            empty.setCharacters(List.of());
            empty.setMemories(List.of());
            return empty;
        }

        GlobalSearchResultVO result = new GlobalSearchResultVO();
        result.setProjects(searchProjects(userId, normalized, normalizedLimit));
        result.setChapters(searchChapters(userId, normalized, normalizedLimit));
        result.setOutlines(searchOutlines(userId, normalized, normalizedLimit));
        result.setCharacters(searchCharacters(userId, normalized, normalizedLimit));
        result.setMemories(searchMemories(userId, normalized, normalizedLimit));
        return result;
    }

    private List<GlobalSearchResultVO.ProjectHitVO> searchProjects(Long userId, String keyword, int limit) {
        LambdaQueryWrapper<NovelProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelProject::getUserId, userId)
                .and(w -> w.like(NovelProject::getTitle, keyword)
                        .or()
                        .like(NovelProject::getDescription, keyword))
                .orderByDesc(NovelProject::getUpdateTime);

        Page<NovelProject> page = projectMapper.selectPage(new Page<>(1, limit), wrapper);
        return page.getRecords().stream().map(p -> {
            GlobalSearchResultVO.ProjectHitVO vo = new GlobalSearchResultVO.ProjectHitVO();
            vo.setId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setDescription(p.getDescription());
            return vo;
        }).toList();
    }

    private List<GlobalSearchResultVO.ChapterHitVO> searchChapters(Long userId, String keyword, int limit) {
        LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.inSql(NovelChapter::getProjectId,
                        "SELECT id FROM novel_project WHERE user_id = " + userId + " AND is_deleted = 0")
                .and(w -> w.like(NovelChapter::getTitle, keyword)
                        .or()
                        .like(NovelChapter::getSummary, keyword))
                .orderByDesc(NovelChapter::getUpdateTime);

        Page<NovelChapter> page = chapterMapper.selectPage(new Page<>(1, limit), wrapper);
        return page.getRecords().stream().map(c -> {
            GlobalSearchResultVO.ChapterHitVO vo = new GlobalSearchResultVO.ChapterHitVO();
            vo.setId(c.getId());
            vo.setProjectId(c.getProjectId());
            vo.setChapterNumber(c.getChapterNumber());
            vo.setTitle(c.getTitle());
            vo.setSummary(c.getSummary());
            return vo;
        }).toList();
    }

    private List<GlobalSearchResultVO.OutlineHitVO> searchOutlines(Long userId, String keyword, int limit) {
        LambdaQueryWrapper<NovelOutline> wrapper = new LambdaQueryWrapper<>();
        wrapper.inSql(NovelOutline::getProjectId,
                        "SELECT id FROM novel_project WHERE user_id = " + userId + " AND is_deleted = 0")
                .and(w -> w.like(NovelOutline::getTitle, keyword)
                        .or()
                        .like(NovelOutline::getContent, keyword))
                .orderByAsc(NovelOutline::getOrderIndex);

        Page<NovelOutline> page = outlineMapper.selectPage(new Page<>(1, limit), wrapper);
        return page.getRecords().stream().map(o -> {
            GlobalSearchResultVO.OutlineHitVO vo = new GlobalSearchResultVO.OutlineHitVO();
            vo.setId(o.getId());
            vo.setProjectId(o.getProjectId());
            vo.setOrderIndex(o.getOrderIndex());
            vo.setTitle(o.getTitle());
            vo.setContent(o.getContent());
            return vo;
        }).toList();
    }

    private List<GlobalSearchResultVO.CharacterHitVO> searchCharacters(Long userId, String keyword, int limit) {
        LambdaQueryWrapper<NovelCharacter> wrapper = new LambdaQueryWrapper<>();
        wrapper.inSql(NovelCharacter::getProjectId,
                        "SELECT id FROM novel_project WHERE user_id = " + userId + " AND is_deleted = 0")
                .and(w -> w.like(NovelCharacter::getName, keyword)
                        .or()
                        .like(NovelCharacter::getBackground, keyword))
                .orderByDesc(NovelCharacter::getUpdateTime);

        Page<NovelCharacter> page = characterMapper.selectPage(new Page<>(1, limit), wrapper);
        return page.getRecords().stream().map(c -> {
            GlobalSearchResultVO.CharacterHitVO vo = new GlobalSearchResultVO.CharacterHitVO();
            vo.setId(c.getId());
            vo.setProjectId(c.getProjectId());
            vo.setName(c.getName());
            vo.setRoleType(c.getRoleType());
            vo.setIsOrganization(c.getIsOrganization());
            return vo;
        }).toList();
    }

    private List<GlobalSearchResultVO.MemoryHitVO> searchMemories(Long userId, String keyword, int limit) {
        LambdaQueryWrapper<NovelStoryMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.inSql(NovelStoryMemory::getProjectId,
                        "SELECT id FROM novel_project WHERE user_id = " + userId + " AND is_deleted = 0")
                .and(w -> w.like(NovelStoryMemory::getTitle, keyword)
                        .or()
                        .like(NovelStoryMemory::getContent, keyword))
                .orderByDesc(NovelStoryMemory::getUpdateTime);

        Page<NovelStoryMemory> page = storyMemoryMapper.selectPage(new Page<>(1, limit), wrapper);
        return page.getRecords().stream().map(m -> {
            GlobalSearchResultVO.MemoryHitVO vo = new GlobalSearchResultVO.MemoryHitVO();
            vo.setId(m.getId());
            vo.setProjectId(m.getProjectId());
            vo.setChapterId(m.getChapterId());
            vo.setMemoryType(m.getMemoryType());
            vo.setTitle(m.getTitle());
            vo.setContent(m.getContent());
            return vo;
        }).toList();
    }
}

