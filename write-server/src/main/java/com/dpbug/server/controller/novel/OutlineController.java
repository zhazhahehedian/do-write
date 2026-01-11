package com.dpbug.server.controller.novel;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.vo.novel.OutlineVO;
import com.dpbug.server.service.novel.OutlineService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 大纲控制器
 *
 * @author dpbug
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/outline")
@RequiredArgsConstructor
public class OutlineController {

    private final OutlineService outlineService;

    /**
     * 获取项目所有大纲列表
     */
    @GetMapping("/list")
    public Result<List<OutlineVO>> list(
            @RequestParam @NotNull(message = "项目ID不能为空") Long projectId) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<OutlineVO> outlines = outlineService.listByProject(userId, projectId);
        return Result.success(outlines);
    }

    /**
     * 获取大纲详情
     */
    @GetMapping("/{id}")
    public Result<OutlineVO> getById(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        OutlineVO outline = outlineService.getById(userId, id);
        return Result.success(outline);
    }

    /**
     * 删除大纲
     */
    @PostMapping("/{id}/delete")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        outlineService.delete(userId, id);
        return Result.success();
    }

    /**
     * 获取项目大纲数量
     */
    @GetMapping("/count")
    public Result<Integer> count(
            @RequestParam @NotNull(message = "项目ID不能为空") Long projectId) {
        int count = outlineService.countByProject(projectId);
        return Result.success(count);
    }
}
