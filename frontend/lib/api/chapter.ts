import { request } from './client'
import { ssePost, type SSEOptions } from '@/lib/utils/sse-client'
import type { Chapter, ChapterDetail } from '@/lib/types/chapter'
import type { PageResult, PageRequest } from '@/lib/types/api'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api'

export const chapterApi = {
  // 获取章节列表
  list: (projectId: string, params?: PageRequest) =>
    request<PageResult<Chapter>>({
      method: 'POST',
      url: '/novel/chapter/list',
      data: { projectId, ...params },
    }),

  // 获取章节详情
  getDetail: (chapterId: string) =>
    request<ChapterDetail>({
      method: 'GET',
      url: `/novel/chapter/${chapterId}`,
    }),

  // 生成章节 (SSE 流式)
  generate: <T = any>(
    outlineId: string,
    params: {
      writingStyleId?: string
      subIndex?: number
    },
    options: SSEOptions<T>
  ) => {
    return ssePost<T>(
      `${API_BASE_URL}/novel/chapter/generate`,
      { outlineId, ...params },
      options
    )
  },

  // 重新生成章节 (SSE 流式)
  regenerate: <T = any>(
    chapterId: string,
    params: any,
    options?: SSEOptions<T>
  ) => {
    // If options is provided as the second argument (legacy support)
    if (typeof params === 'object' && (params.onProgress || params.onChunk || params.onResult)) {
        return ssePost<T>(
            `${API_BASE_URL}/novel/chapter/regenerate/${chapterId}`,
            {},
            params as SSEOptions<T>
        )
    }
    
    return ssePost<T>(
      `${API_BASE_URL}/novel/chapter/regenerate/${chapterId}`,
      params || {},
      options || {}
    )
  },

  // 润色章节 (SSE 流式)
  polish: <T = any>(
    chapterId: string,
    instruction: string,
    options: SSEOptions<T>
  ) => {
    return ssePost<T>(
      `${API_BASE_URL}/novel/chapter/polish`,
      { chapterId, instruction },
      options
    )
  },

  // AI去味 (SSE 流式)
  denoise: <T = any>(
    chapterId: string,
    options: SSEOptions<T>
  ) => {
    return ssePost<T>(
      `${API_BASE_URL}/novel/chapter/denoise/${chapterId}`,
      {},
      options
    )
  },

  // 更新章节
  update: (chapterId: string, data: { title?: string; content?: string }) =>
    request<void>({
      method: 'POST',
      url: '/novel/chapter/update',
      data: { ...data, id: chapterId },
    }),

  // 删除章节
  delete: (chapterId: string) =>
    request<void>({
      method: 'POST',
      url: `/novel/chapter/delete/${chapterId}`,
    }),

  // 获取生成上下文
  getContext: (projectId: string, outlineId: string) =>
    request<{
        world?: string
        characters?: string
        outline?: string
        histories?: string[]
    }>({
        method: 'GET',
        url: '/novel/chapter/context',
        params: { projectId, outlineId }
    }),

  // 批量生成
  batchGenerate: (projectId: string, outlineIds: string[]) =>
    request<{ taskId: string }>({
      method: 'POST',
      url: '/novel/chapter/batch-generate',
      data: { projectId, outlineIds },
    }),

  // 获取任务状态
  getTaskStatus: (taskId: string) =>
    request<{
      status: string
      progress: number
      currentStep: string
      completedChapters: number
      totalChapters: number
    }>({
      method: 'GET',
      url: `/novel/task/${taskId}`,
    }),
}
