package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.util.List;

/**
 * 全局搜索结果（按类型聚合）
 */
@Data
public class GlobalSearchResultVO {

    private List<ProjectHitVO> projects;
    private List<ChapterHitVO> chapters;
    private List<OutlineHitVO> outlines;
    private List<CharacterHitVO> characters;
    private List<MemoryHitVO> memories;

    @Data
    public static class ProjectHitVO {
        private Long id;
        private String title;
        private String description;
    }

    @Data
    public static class ChapterHitVO {
        private Long id;
        private Long projectId;
        private Integer chapterNumber;
        private String title;
        private String summary;
    }

    @Data
    public static class OutlineHitVO {
        private Long id;
        private Long projectId;
        private Integer orderIndex;
        private String title;
        private String content;
    }

    @Data
    public static class CharacterHitVO {
        private Long id;
        private Long projectId;
        private String name;
        private String roleType;
        private Integer isOrganization;
    }

    @Data
    public static class MemoryHitVO {
        private Long id;
        private Long projectId;
        private Long chapterId;
        private String memoryType;
        private String title;
        private String content;
    }
}

