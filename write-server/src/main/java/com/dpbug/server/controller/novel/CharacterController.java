package com.dpbug.server.controller.novel;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.PageResult;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.entity.novel.NovelCharacter;
import com.dpbug.server.model.vo.novel.CharacterVO;
import com.dpbug.server.service.novel.CharacterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色控制器
 *
 * @author dpbug
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/character")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    /**
     * 获取角色列表（分页）
     */
    @GetMapping("/list")
    public Result<PageResult<CharacterVO>> list(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<CharacterVO> characters = characterService.listByProject(userId, projectId);

        // 简单分页处理
        int total = characters.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<CharacterVO> pagedList = characters.subList(fromIndex, toIndex);

        PageResult<CharacterVO> result = PageResult.of(pageNum, pageSize, (long) total, pagedList);
        return Result.success(result);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{characterId}")
    public Result<CharacterVO> getById(@PathVariable Long characterId) {
        Long userId = StpUtil.getLoginIdAsLong();
        CharacterVO character = characterService.getById(userId, characterId);
        return Result.success(character);
    }

    /**
     * 创建角色
     */
    @PostMapping("/create")
    public Result<CharacterVO> create(@RequestBody NovelCharacter character) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 使用批量创建方法创建单个角色
        characterService.createBatch(userId, character.getProjectId(), List.of(character));
        // 返回创建的角色（通过查询获取）
        List<CharacterVO> list = characterService.listByProject(userId, character.getProjectId());
        CharacterVO created = list.stream()
                .filter(c -> c.getName().equals(character.getName()))
                .findFirst()
                .orElse(null);
        return Result.success(created);
    }

    /**
     * 更新角色
     */
    @PostMapping("/{characterId}/update")
    public Result<CharacterVO> update(
            @PathVariable Long characterId,
            @RequestBody NovelCharacter character) {
        Long userId = StpUtil.getLoginIdAsLong();
        character.setId(characterId);
        characterService.update(userId, character);
        CharacterVO updated = characterService.getById(userId, characterId);
        return Result.success(updated);
    }

    /**
     * 删除角色
     */
    @PostMapping("/{characterId}/delete")
    public Result<Void> delete(@PathVariable Long characterId) {
        Long userId = StpUtil.getLoginIdAsLong();
        characterService.delete(userId, characterId);
        return Result.success();
    }

    /**
     * 获取项目角色统计
     */
    @GetMapping("/statistics")
    public Result<?> getStatistics(@RequestParam Long projectId) {
        return Result.success(characterService.getStatistics(projectId));
    }
}
