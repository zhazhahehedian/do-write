package com.dpbug.server.mapper.novel;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dpbug.server.model.entity.novel.NovelOutline;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface OutlineMapper extends BaseMapper<NovelOutline> {

    /**
     * 查询项目大纲（按序号排序）
     */
    @Select("SELECT * FROM novel_outline WHERE project_id = #{projectId} AND is_deleted = 0 ORDER BY order_index ASC")
    List<NovelOutline> selectByProjectIdOrdered(@Param("projectId") Long projectId);
}
