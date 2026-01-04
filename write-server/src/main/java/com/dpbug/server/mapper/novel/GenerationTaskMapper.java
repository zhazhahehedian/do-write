package com.dpbug.server.mapper.novel;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dpbug.server.model.entity.novel.NovelGenerationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 生成任务Mapper接口
 *
 * @author dpbug
 */
@Mapper
public interface GenerationTaskMapper extends BaseMapper<NovelGenerationTask> {

    /**
     * 查询用户进行中的任务
     *
     * @param userId 用户ID
     * @return 任务列表
     */
    List<NovelGenerationTask> selectRunningTasks(@Param("userId") Long userId);

    /**
     * 更新任务进度
     *
     * @param taskId      任务ID
     * @param progress    进度
     * @param currentStep 当前步骤
     * @return 影响行数
     */
    int updateProgress(
            @Param("taskId") Long taskId,
            @Param("progress") Integer progress,
            @Param("currentStep") String currentStep
    );
}