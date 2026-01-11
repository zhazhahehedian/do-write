package com.dpbug.server.service.novel.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.server.mapper.novel.CharacterMapper;
import com.dpbug.server.model.entity.novel.NovelCharacter;
import com.dpbug.server.model.vo.novel.CharacterStatisticsVO;
import com.dpbug.server.model.vo.novel.CharacterVO;
import com.dpbug.server.service.novel.CharacterService;
import com.dpbug.server.service.novel.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务实现类
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterServiceImpl implements CharacterService {

    private final CharacterMapper characterMapper;
    private final ProjectService projectService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBatch(Long userId, Long projectId, List<NovelCharacter> characters) {
        // 检查项目权限
        projectService.checkOwnership(userId, projectId);

        if (characters == null || characters.isEmpty()) {
            return;
        }

        // 设置项目ID
        characters.forEach(character -> character.setProjectId(projectId));

        // 批量插入
        for (NovelCharacter character : characters) {
            characterMapper.insert(character);
        }

        log.info("批量创建角色成功: userId={}, projectId={}, count={}", userId, projectId, characters.size());
    }

    @Override
    public List<CharacterVO> listByProject(Long userId, Long projectId) {
        // 检查项目权限
        projectService.checkOwnership(userId, projectId);

        LambdaQueryWrapper<NovelCharacter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelCharacter::getProjectId, projectId)
                .orderByAsc(NovelCharacter::getCreateTime);

        List<NovelCharacter> characters = characterMapper.selectList(wrapper);

        return characters.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public List<CharacterVO> listByProjectAndType(Long userId, Long projectId, String roleType) {
        // 检查项目权限
        projectService.checkOwnership(userId, projectId);

        LambdaQueryWrapper<NovelCharacter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelCharacter::getProjectId, projectId)
                .eq(NovelCharacter::getIsOrganization, 0)
                .eq(NovelCharacter::getRoleType, roleType)
                .orderByAsc(NovelCharacter::getCreateTime);

        List<NovelCharacter> characters = characterMapper.selectList(wrapper);

        return characters.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public List<CharacterVO> listOrganizationsByProject(Long userId, Long projectId) {
        // 检查项目权限
        projectService.checkOwnership(userId, projectId);

        LambdaQueryWrapper<NovelCharacter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelCharacter::getProjectId, projectId)
                .eq(NovelCharacter::getIsOrganization, 1)
                .orderByAsc(NovelCharacter::getCreateTime);

        List<NovelCharacter> characters = characterMapper.selectList(wrapper);

        return characters.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public CharacterVO getById(Long userId, Long characterId) {
        NovelCharacter character = characterMapper.selectById(characterId);

        if (character == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在");
        }

        // 检查项目权限
        projectService.checkOwnership(userId, character.getProjectId());

        return convertToVO(character);
    }

    @Override
    public void update(Long userId, NovelCharacter character) {
        // 先查询原角色，检查权限
        NovelCharacter existing = characterMapper.selectById(character.getId());

        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在");
        }

        // 检查项目权限
        projectService.checkOwnership(userId, existing.getProjectId());

        // 不允许修改projectId
        character.setProjectId(null);

        characterMapper.updateById(character);

        log.info("更新角色成功: userId={}, characterId={}", userId, character.getId());
    }

    @Override
    public void delete(Long userId, Long characterId) {
        NovelCharacter character = characterMapper.selectById(characterId);

        if (character == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在");
        }

        // 检查项目权限
        projectService.checkOwnership(userId, character.getProjectId());

        characterMapper.deleteById(characterId);

        log.info("删除角色成功: userId={}, characterId={}", userId, characterId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProject(Long userId, Long projectId) {
        // 检查项目权限
        projectService.checkOwnership(userId, projectId);

        LambdaUpdateWrapper<NovelCharacter> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NovelCharacter::getProjectId, projectId);

        characterMapper.delete(wrapper);

        log.info("删除项目所有角色: userId={}, projectId={}", userId, projectId);
    }

    @Override
    public CharacterStatisticsVO getStatistics(Long projectId) {
        return characterMapper.selectStatistics(projectId);
    }

    @Override
    public List<CharacterVO> listByProjectInternal(Long projectId) {
        LambdaQueryWrapper<NovelCharacter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelCharacter::getProjectId, projectId)
                .orderByAsc(NovelCharacter::getCreateTime);

        List<NovelCharacter> characters = characterMapper.selectList(wrapper);

        return characters.stream()
                .map(this::convertToVO)
                .toList();
    }

    /**
     * 转换为VO
     */
    private CharacterVO convertToVO(NovelCharacter character) {
        CharacterVO vo = new CharacterVO();
        BeanUtils.copyProperties(character, vo);
        return vo;
    }
}
