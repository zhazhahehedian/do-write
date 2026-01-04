package com.dpbug.server.service.novel.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dpbug.common.constant.NovelConstants;
import com.dpbug.common.domain.PageResult;
import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.server.mapper.novel.CharacterMapper;
import com.dpbug.server.mapper.novel.OutlineMapper;
import com.dpbug.server.mapper.novel.ProjectMapper;
import com.dpbug.server.model.dto.novel.ProjectCreateRequest;
import com.dpbug.server.model.dto.novel.ProjectQueryRequest;
import com.dpbug.server.model.dto.novel.ProjectUpdateRequest;
import com.dpbug.server.model.entity.novel.NovelCharacter;
import com.dpbug.server.model.entity.novel.NovelOutline;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.vo.novel.ProjectListVO;
import com.dpbug.server.model.vo.novel.ProjectStatisticsVO;
import com.dpbug.server.model.vo.novel.ProjectVO;
import com.dpbug.server.service.novel.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 项目服务实现类
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final CharacterMapper characterMapper;
    private final OutlineMapper outlineMapper;

    @Override
    public Long create(Long userId, ProjectCreateRequest dto) {
        NovelProject project = new NovelProject();
        BeanUtils.copyProperties(dto, project);

        project.setUserId(userId);
        project.setStatus(NovelConstants.ProjectStatus.PLANNING);
        project.setWizardStatus(NovelConstants.WizardStatus.NOT_STARTED);
        project.setWizardStep(NovelConstants.WizardStep.INIT);
        project.setCurrentWords(0);
        project.setCharacterCount(0);

        // 设置默认值
        if (project.getOutlineMode() == null) {
            project.setOutlineMode(NovelConstants.OutlineMode.ONE_TO_ONE);
        }

        projectMapper.insert(project);

        log.info("创建项目成功: userId={}, projectId={}, title={}",
                userId, project.getId(), project.getTitle());

        return project.getId();
    }

    @Override
    public ProjectVO getById(Long userId, Long projectId) {
        NovelProject project = checkOwnership(userId, projectId);

        ProjectVO vo = new ProjectVO();
        BeanUtils.copyProperties(project, vo);

        // 填充实际统计数据
        fillStatistics(vo, projectId);

        return vo;
    }

    /**
     * 填充统计数据
     */
    private void fillStatistics(ProjectVO vo, Long projectId) {
        // 查询角色数量
        LambdaQueryWrapper<NovelCharacter> charWrapper = new LambdaQueryWrapper<>();
        charWrapper.eq(NovelCharacter::getProjectId, projectId);
        vo.setActualCharacterCount((int) (long) characterMapper.selectCount(charWrapper));

        // 查询大纲数量
        LambdaQueryWrapper<NovelOutline> outlineWrapper = new LambdaQueryWrapper<>();
        outlineWrapper.eq(NovelOutline::getProjectId, projectId);
        vo.setActualOutlineCount((int) (long) outlineMapper.selectCount(outlineWrapper));

        // TODO: 章节数量统计（待章节模块实现后添加）
        vo.setActualChapterCount(0);
    }

    @Override
    public void update(Long userId, ProjectUpdateRequest dto) {
        // 检查权限
        checkOwnership(userId, dto.getId());

        NovelProject project = new NovelProject();
        BeanUtils.copyProperties(dto, project);

        projectMapper.updateById(project);

        log.info("更新项目成功: userId={}, projectId={}", userId, dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long projectId) {
        // 检查权限
        checkOwnership(userId, projectId);

        // 级联删除角色
        LambdaUpdateWrapper<NovelCharacter> charWrapper = new LambdaUpdateWrapper<>();
        charWrapper.eq(NovelCharacter::getProjectId, projectId);
        characterMapper.delete(charWrapper);

        // 级联删除大纲
        LambdaUpdateWrapper<NovelOutline> outlineWrapper = new LambdaUpdateWrapper<>();
        outlineWrapper.eq(NovelOutline::getProjectId, projectId);
        outlineMapper.delete(outlineWrapper);

        // TODO: 级联删除章节（待章节模块实现后添加）
        // TODO: 级联删除故事记忆（待记忆模块实现后添加）

        // 逻辑删除项目
        projectMapper.deleteById(projectId);

        log.info("删除项目成功（含级联数据）: userId={}, projectId={}", userId, projectId);
    }

    @Override
    public PageResult<ProjectListVO> list(Long userId, ProjectQueryRequest query) {
        // 构建查询条件
        LambdaQueryWrapper<NovelProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelProject::getUserId, userId);

        // 标题模糊搜索
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(NovelProject::getTitle, query.getTitle());
        }

        // 类型筛选
        if (StringUtils.hasText(query.getGenre())) {
            wrapper.eq(NovelProject::getGenre, query.getGenre());
        }

        // 状态筛选
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(NovelProject::getStatus, query.getStatus());
        }

        // 向导状态筛选
        if (StringUtils.hasText(query.getWizardStatus())) {
            wrapper.eq(NovelProject::getWizardStatus, query.getWizardStatus());
        }

        // 排序（默认按更新时间倒序，最新的在前）
        String orderBy = StringUtils.hasText(query.getOrderBy()) ? query.getOrderBy() : "update_time";
        switch (orderBy) {
            case "title" -> wrapper.orderByAsc(NovelProject::getTitle);
            case "create_time" -> wrapper.orderByDesc(NovelProject::getCreateTime);
            default -> wrapper.orderByDesc(NovelProject::getUpdateTime);
        }

        // 分页查询
        Page<NovelProject> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<NovelProject> result = projectMapper.selectPage(page, wrapper);

        // 转换为VO
        List<ProjectListVO> voList = result.getRecords().stream()
                .map(this::convertToListVO)
                .toList();

        return PageResult.of(
                query.getPageNum(),
                query.getPageSize(),
                result.getTotal(),
                voList
        );
    }

    @Override
    public ProjectStatisticsVO getStatistics(Long userId, Long projectId) {
        // 检查权限
        checkOwnership(userId, projectId);

        return projectMapper.selectStatistics(projectId, userId);
    }

    @Override
    public void refreshStatistics(Long projectId) {
        projectMapper.updateStatistics(projectId);
        log.debug("刷新项目统计: projectId={}", projectId);
    }

    @Override
    public void updateWizardStatus(Long userId, Long projectId, String status, Integer step) {
        checkOwnership(userId, projectId);

        NovelProject update = new NovelProject();
        update.setId(projectId);
        update.setWizardStatus(status);
        update.setWizardStep(step);

        projectMapper.updateById(update);

        log.info("更新向导状态: projectId={}, status={}, step={}", projectId, status, step);
    }

    @Override
    public NovelProject checkOwnership(Long userId, Long projectId) {

        NovelProject project = projectMapper.selectById(projectId);

        if (project == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "项目不存在");
        }

        if (!project.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该项目");
        }

        return project;
    }

    /**
     * 转换为列表VO
     */
    private ProjectListVO convertToListVO(NovelProject project) {
        ProjectListVO vo = new ProjectListVO();
        BeanUtils.copyProperties(project, vo);

        // 计算进度百分比
        if (project.getTargetWords() != null && project.getTargetWords() > 0) {
            int current = project.getCurrentWords() != null ? project.getCurrentWords() : 0;
            vo.setProgressPercent((int) Math.round(current * 100.0 / project.getTargetWords()));
        } else {
            vo.setProgressPercent(0);
        }

        return vo;
    }
}