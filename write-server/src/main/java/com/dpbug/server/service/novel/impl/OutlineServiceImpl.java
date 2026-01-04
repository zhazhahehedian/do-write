package com.dpbug.server.service.novel.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.server.mapper.novel.OutlineMapper;
import com.dpbug.server.model.entity.novel.NovelOutline;
import com.dpbug.server.model.vo.novel.OutlineVO;
import com.dpbug.server.service.novel.OutlineService;
import com.dpbug.server.service.novel.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 大纲服务实现类
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutlineServiceImpl implements OutlineService {

    private final OutlineMapper outlineMapper;
    private final ProjectService projectService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBatch(Long userId, Long projectId, List<NovelOutline> outlines) {
        // 检查项目权限
        projectService.checkOwnership(userId, projectId);

        if (outlines == null || outlines.isEmpty()) {
            return;
        }

        // 获取当前最大序号
        int maxOrderIndex = getNextOrderIndex(projectId) - 1;

        // 设置项目ID和序号
        for (int i = 0; i < outlines.size(); i++) {
            NovelOutline outline = outlines.get(i);
            outline.setProjectId(projectId);
            // 如果没有设置序号，则自动分配
            if (outline.getOrderIndex() == null) {
                outline.setOrderIndex(maxOrderIndex + i + 1);
            }
            outlineMapper.insert(outline);
        }

        log.info("批量创建大纲成功: userId={}, projectId={}, count={}", userId, projectId, outlines.size());
    }

    @Override
    public List<OutlineVO> listByProject(Long userId, Long projectId) {
        // 检查项目权限
        projectService.checkOwnership(userId, projectId);

        List<NovelOutline> outlines = outlineMapper.selectByProjectIdOrdered(projectId);

        return outlines.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public OutlineVO getById(Long userId, Long outlineId) {
        OutlineVO vo = getByIdInternal(outlineId);

        // 检查项目权限
        projectService.checkOwnership(userId, vo.getProjectId());

        return vo;
    }

    @Override
    public OutlineVO getByIdInternal(Long outlineId) {
        NovelOutline outline = outlineMapper.selectById(outlineId);

        if (outline == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "大纲不存在");
        }

        return convertToVO(outline);
    }

    @Override
    public void update(Long userId, NovelOutline outline) {
        // 先查询原大纲，检查权限
        NovelOutline existing = outlineMapper.selectById(outline.getId());

        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "大纲不存在");
        }

        // 检查项目权限
        projectService.checkOwnership(userId, existing.getProjectId());

        // 不允许修改projectId
        outline.setProjectId(null);

        outlineMapper.updateById(outline);

        log.info("更新大纲成功: userId={}, outlineId={}", userId, outline.getId());
    }

    @Override
    public void delete(Long userId, Long outlineId) {
        NovelOutline outline = outlineMapper.selectById(outlineId);

        if (outline == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "大纲不存在");
        }

        // 检查项目权限
        projectService.checkOwnership(userId, outline.getProjectId());

        outlineMapper.deleteById(outlineId);

        log.info("删除大纲成功: userId={}, outlineId={}", userId, outlineId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProject(Long userId, Long projectId) {
        // 检查项目权限
        projectService.checkOwnership(userId, projectId);

        LambdaUpdateWrapper<NovelOutline> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NovelOutline::getProjectId, projectId);

        outlineMapper.delete(wrapper);

        log.info("删除项目所有大纲: userId={}, projectId={}", userId, projectId);
    }

    @Override
    public int countByProject(Long projectId) {
        LambdaQueryWrapper<NovelOutline> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelOutline::getProjectId, projectId);

        return Math.toIntExact(outlineMapper.selectCount(wrapper));
    }

    @Override
    public int getNextOrderIndex(Long projectId) {
        LambdaQueryWrapper<NovelOutline> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelOutline::getProjectId, projectId)
                .orderByDesc(NovelOutline::getOrderIndex)
                .last("LIMIT 1");

        NovelOutline lastOutline = outlineMapper.selectOne(wrapper);

        if (lastOutline == null || lastOutline.getOrderIndex() == null) {
            return 1;
        }

        return lastOutline.getOrderIndex() + 1;
    }

    @Override
    public void updateOrder(Long userId, Long outlineId, Integer orderIndex) {
        NovelOutline outline = outlineMapper.selectById(outlineId);

        if (outline == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "大纲不存在");
        }

        // 检查项目权限
        projectService.checkOwnership(userId, outline.getProjectId());

        NovelOutline update = new NovelOutline();
        update.setId(outlineId);
        update.setOrderIndex(orderIndex);

        outlineMapper.updateById(update);

        log.info("更新大纲顺序: userId={}, outlineId={}, orderIndex={}", userId, outlineId, orderIndex);
    }

    /**
     * 转换为VO
     */
    private OutlineVO convertToVO(NovelOutline outline) {
        OutlineVO vo = new OutlineVO();
        BeanUtils.copyProperties(outline, vo);
        return vo;
    }
}
