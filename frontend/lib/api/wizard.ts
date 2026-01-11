import { request } from './client'
import { ssePost, type SSEOptions } from '@/lib/utils/sse-client'
import type { Character } from '@/lib/types/character'
import type { WizardProgress, GenerationTask } from '@/lib/types/wizard'

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

  // 生成角色 (非流式，返回结构化数据) - 已废弃，保留向后兼容
  /** @deprecated 请使用 generateCharactersAsync */
  generateCharacters: (projectId: string, params: {
    protagonistCount?: number
    supportingCount?: number
    antagonistCount?: number
    organizationCount?: number
  }) =>
    request<Character[]>({
      method: 'POST',
      url: '/novel/wizard/characters/generate',
      data: { projectId, ...params },
    }),

  // 异步生成角色（返回任务信息，前端需轮询获取结果）
  generateCharactersAsync: (projectId: string, params: {
    protagonistCount?: number
    supportingCount?: number
    antagonistCount?: number
    organizationCount?: number
  }) =>
    request<GenerationTask>({
      method: 'POST',
      url: '/novel/wizard/characters/generate/async',
      data: { projectId, ...params },
    }),

  // 获取任务状态
  getTask: (taskId: string) =>
    request<GenerationTask>({
      method: 'GET',
      url: '/novel/wizard/task',
      params: { taskId },
    }),

  // 生成大纲 (SSE 流式) - 已废弃，保留向后兼容
  /** @deprecated 请使用 generateOutlinesAsync */
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

  // 异步生成大纲（返回任务信息，前端需轮询获取结果）
  generateOutlinesAsync: (projectId: string, params: {
    outlineCount?: number
    customRequirements?: string
  }) =>
    request<GenerationTask>({
      method: 'POST',
      url: '/novel/wizard/outlines/generate/async',
      data: { projectId, ...params },
    }),

  // 更新向导状态
  updateStatus: (projectId: string, status: string, step: number) =>
    request<void>({
      method: 'POST',
      url: '/novel/wizard/status',
      data: { projectId, status, step },
    }),

  // 获取向导进度
  getProgress: (projectId: string) =>
    request<WizardProgress>({
      method: 'GET',
      url: '/novel/wizard/progress',
      params: { projectId },
    }),

  // 重置向导步骤
  resetStep: (projectId: string, step: number) =>
    request<void>({
      method: 'POST',
      url: '/novel/wizard/reset',
      params: { projectId, step },
    }),
}
