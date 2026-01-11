package com.dpbug.server.service.novel;

import com.dpbug.server.model.vo.novel.GlobalSearchResultVO;

/**
 * 全局搜索服务
 */
public interface SearchService {

    GlobalSearchResultVO search(Long userId, String keyword, Integer limit);
}

