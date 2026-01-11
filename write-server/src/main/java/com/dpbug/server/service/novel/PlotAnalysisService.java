package com.dpbug.server.service.novel;

import com.dpbug.server.model.vo.novel.PlotAnalysisVO;

/**
 * 剧情分析服务
 */
public interface PlotAnalysisService {

    PlotAnalysisVO analyzeChapter(Long userId, Long chapterId, Boolean force);

    PlotAnalysisVO getByChapterId(Long userId, Long chapterId);
}

