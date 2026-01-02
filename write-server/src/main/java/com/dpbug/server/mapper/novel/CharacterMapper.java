package com.dpbug.server.mapper.novel;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dpbug.server.model.entity.novel.NovelCharacter;
import com.dpbug.server.model.vo.novel.CharacterStatisticsVO;
import org.apache.ibatis.annotations.Param;

public interface CharacterMapper extends BaseMapper<NovelCharacter> {

    CharacterStatisticsVO selectStatistics(@Param("projectId") Long projectId);
}
