import { request } from './client'
import { ssePost, type SSEOptions } from '@/lib/utils/sse-client'
import type { Character } from '@/lib/types/character'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api'

export const wizardApi = {
  // 生成世界观 (SSE 流式)
  generateWorld: <T = any>(
    projectId: string,
    options: SSEOptions<T>
  ) => {
    return ssePost<T>(
      `${API_BASE_URL}/novel/wizard/world/generate`,
      { projectId },
      options
    )
  },

  // 生成角色 (非流式，返回结构化数据)
  generateCharacters: (projectId: string, params: {
    protagonistCount?: number
    supportingCount?: number
    antagonistCount?: number
  }) =>
    request<Character[]>({
      method: 'POST',
      url: '/novel/wizard/characters/generate',
      data: { projectId, ...params },
    }),

  // 生成大纲 (SSE 流式)
  generateOutlines: <T = any>(
    projectId: string,
    params: {
      chapterCount?: number
      outlineMode?: 'ONE_TO_ONE' | 'ONE_TO_MANY'
    },
    options: SSEOptions<T>
  ) => {
    return ssePost<T>(
      `${API_BASE_URL}/novel/wizard/outlines/generate`,
      { projectId, ...params },
      options
    )
  },

  // 更新向导状态
  updateStatus: (projectId: string, status: string, step: number) =>
    request<void>({
      method: 'POST',
      url: '/novel/wizard/status',
      data: { projectId, status, step },
    }),
}
