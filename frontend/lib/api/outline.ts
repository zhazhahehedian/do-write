import { request } from './client'
import type { Outline, OutlineExpandApplyRequest } from '@/lib/types/outline'
import type { Chapter } from '@/lib/types/chapter'
import type { GenerationTask } from '@/lib/types/wizard'

export const outlineApi = {
  list: (projectId: string) =>
    request<Outline[]>({
      method: 'GET',
      url: '/novel/outline/list',
      params: { projectId },
    }),

  getById: (id: string) =>
    request<Outline>({
      method: 'GET',
      url: `/novel/outline/${id}`,
    }),

  create: (data: Partial<Outline>) =>
    request<Outline>({
      method: 'POST',
      url: '/novel/outline/create',
      data,
    }),

  update: (id: string, data: Partial<Outline>) =>
    request<Outline>({
      method: 'POST',
      url: `/novel/outline/${id}/update`,
      data,
    }),

  delete: (id: string) =>
    request<void>({
      method: 'POST',
      url: `/novel/outline/${id}/delete`,
    }),

  // ==================== 大纲展开 (one-to-many) ====================

  /**
   * 获取大纲已展开的子章节列表
   */
  getExpandedChapters: (outlineId: string) =>
    request<Chapter[]>({
      method: 'GET',
      url: `/novel/outline/${outlineId}/chapters`,
    }),

  /**
   * 检查大纲是否已展开
   */
  isExpanded: (outlineId: string) =>
    request<boolean>({
      method: 'GET',
      url: `/novel/outline/${outlineId}/expanded`,
    }),

  /**
   * 应用展开（创建章节记录）
   */
  applyExpansion: (outlineId: string, data: OutlineExpandApplyRequest) =>
    request<Chapter[]>({
      method: 'POST',
      url: `/novel/outline/${outlineId}/expand/apply`,
      data,
    }),

  /**
   * 重置展开（删除已展开的章节）
   */
  resetExpansion: (outlineId: string) =>
    request<void>({
      method: 'POST',
      url: `/novel/outline/${outlineId}/expand/reset`,
    }),

  /**
   * 一纲多章：批量生成已展开的子章节（后台任务）
   */
  generateExpandedChaptersAsync: (
    outlineId: string,
    data?: {
      styleCode?: string
      targetWordCount?: number
      narrativePerspective?: string
      customRequirements?: string
      temperature?: number
      topP?: number
      enableMemoryRetrieval?: boolean
    }
  ) =>
    request<GenerationTask>({
      method: 'POST',
      url: `/novel/outline/${outlineId}/expand/generate/async`,
      data: data ?? {},
    }),
}
