package com.dpbug.server.mapper.novel;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dpbug.server.model.entity.novel.NovelCharacter;
import com.dpbug.server.model.vo.novel.CharacterStatisticsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CharacterMapper extends BaseMapper<NovelCharacter> {

    CharacterStatisticsVO selectStatistics(@Param("projectId") Long projectId);

    /**
     * 根据项目ID和角色名称列表查询角色
     *
     * @param projectId 项目ID
     * @param names     角色名称列表
     * @return 匹配的角色列表
     */
    List<NovelCharacter> selectByProjectAndNames(@Param("projectId") Long projectId,
                                                  @Param("names") List<String> names);
}
