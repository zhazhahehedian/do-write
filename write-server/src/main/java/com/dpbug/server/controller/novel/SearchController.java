package com.dpbug.server.controller.novel;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.vo.novel.GlobalSearchResultVO;
import com.dpbug.server.service.novel.SearchService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统级全局搜索（当前用户范围内）
 */
@RestController
@RequestMapping("/api/novel/search")
@Validated
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public Result<GlobalSearchResultVO> search(
            @RequestParam("keyword") @NotBlank(message = "搜索关键词不能为空") String keyword,
            @RequestParam(value = "limit", required = false, defaultValue = "5") Integer limit
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(searchService.search(userId, keyword, limit));
    }
}
