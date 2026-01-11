import { request } from './client'
import { ssePost, type SSEOptions } from '@/lib/utils/sse-client'
import type { Chapter, ChapterDetail } from '@/lib/types/chapter'
import type { PageResult, PageRequest } from '@/lib/types/api'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api'

export type WritingStyleCode =
  | 'natural'
  | 'classical'
  | 'modern'
  | 'poetic'
  | 'concise'
  | 'vivid'

export interface ChapterGeneratePayload {
  projectId: string | number
  outlineId: string | number
  subIndex?: number
  styleCode?: WritingStyleCode
  targetWordCount?: number
  narrativePerspective?: string
  customRequirements?: string
  temperature?: number
  topP?: number
  enableMemoryRetrieval?: boolean
}

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
  generate: <T = unknown>(payload: ChapterGeneratePayload, options: SSEOptions<T>) => {
    return ssePost<T>(
      `${API_BASE_URL}/novel/chapter/generate`,
      payload,
      options
    )
  },

  // 重新生成章节 (SSE 流式)
  regenerate: <T = unknown>(
    chapterId: string,
    payload: Partial<ChapterGeneratePayload>,
    options: SSEOptions<T>
  ) => {
    return ssePost<T>(
      `${API_BASE_URL}/novel/chapter/regenerate/${chapterId}`,
      payload,
      options
    )
  },

  // 润色章节 (SSE 流式)
  polish: <T = unknown>(
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
  denoise: <T = unknown>(
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
      url: `/novel/chapter/task/${taskId}`,
    }),

  // 取消任务
  cancelTask: (taskId: string) =>
    request<void>({
      method: 'POST',
      url: `/novel/chapter/task/cancel/${taskId}`,
    }),
}
