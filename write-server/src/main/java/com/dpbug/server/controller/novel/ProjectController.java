package com.dpbug.server.controller.novel;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.PageResult;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.dto.novel.ProjectCreateRequest;
import com.dpbug.server.model.dto.novel.ProjectQueryRequest;
import com.dpbug.server.model.dto.novel.ProjectUpdateRequest;
import com.dpbug.server.model.vo.novel.ProjectListVO;
import com.dpbug.server.model.vo.novel.ProjectStatisticsVO;
import com.dpbug.server.model.vo.novel.ProjectVO;
import com.dpbug.server.service.novel.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/novel/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 创建项目
     */
    @PostMapping("/create")
    public Result<Long> create(@RequestBody @Valid ProjectCreateRequest dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long projectId = projectService.create(userId, dto);
        return Result.success(projectId);
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/detail")
    public Result<ProjectVO> getById(@RequestParam("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ProjectVO project = projectService.getById(userId, id);
        return Result.success(project);
    }

    /**
     * 更新项目
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody @Valid ProjectUpdateRequest dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        projectService.update(userId, dto);
        return Result.success();
    }

    /**
     * 删除项目
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        projectService.delete(userId, id);
        return Result.success();
    }

    /**
     * 项目列表（分页）
     */
    @GetMapping("/list")
    public Result<PageResult<ProjectListVO>> list(ProjectQueryRequest query) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<ProjectListVO> result = projectService.list(userId, query);
        return Result.success(result);
    }

    /**
     * 获取项目统计信息
     */
    @GetMapping("/{id}/statistics")
    public Result<ProjectStatisticsVO> getStatistics(@PathVariable("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ProjectStatisticsVO statistics = projectService.getStatistics(userId, id);
        return Result.success(statistics);
    }
}
