package com.dpbug.server.service.novel;

import com.dpbug.server.model.entity.novel.NovelOutline;
import com.dpbug.server.model.vo.novel.OutlineVO;

import java.util.List;

/**
 * 大纲服务接口
 *
 * @author dpbug
 */
public interface OutlineService {

    /**
     * 批量创建大纲
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param outlines  大纲列表
     */
    void createBatch(Long userId, Long projectId, List<NovelOutline> outlines);

    /**
     * 获取项目所有大纲（按序号排序）
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return 大纲列表
     */
    List<OutlineVO> listByProject(Long userId, Long projectId);

    /**
     * 获取大纲详情
     *
     * @param userId    用户ID
     * @param outlineId 大纲ID
     * @return 大纲详情
     */
    OutlineVO getById(Long userId, Long outlineId);

    /**
     * 更新大纲
     *
     * @param userId  用户ID
     * @param outline 大纲实体
     */
    void update(Long userId, NovelOutline outline);

    /**
     * 删除大纲
     *
     * @param userId    用户ID
     * @param outlineId 大纲ID
     */
    void delete(Long userId, Long outlineId);

    /**
     * 删除项目所有大纲
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     */
    void deleteByProject(Long userId, Long projectId);

    /**
     * 获取项目大纲数量
     *
     * @param projectId 项目ID
     * @return 大纲数量
     */
    int countByProject(Long projectId);

    /**
     * 获取项目下一个大纲序号
     *
     * @param projectId 项目ID
     * @return 下一个序号
     */
    int getNextOrderIndex(Long projectId);

    /**
     * 调整大纲顺序
     *
     * @param userId     用户ID
     * @param outlineId  大纲ID
     * @param orderIndex 新的序号
     */
    void updateOrder(Long userId, Long outlineId, Integer orderIndex);
}
