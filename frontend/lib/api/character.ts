import { request } from './client'
import type { Character } from '@/lib/types/character'
import type { PageResult, PageRequest } from '@/lib/types/api'

export const characterApi = {
  list: (projectId: string, params?: PageRequest) =>
    request<PageResult<Character>>({
      method: 'GET',
      url: '/novel/character/list',
      params: { projectId, ...params },
    }),

  getById: (id: string) =>
    request<Character>({
      method: 'GET',
      url: `/novel/character/${id}`,
    }),

  create: (data: Partial<Character>) =>
    request<Character>({
      method: 'POST',
      url: '/novel/character/create',
      data,
    }),

  update: (id: string, data: Partial<Character>) =>
    request<Character>({
      method: 'POST',
      url: `/novel/character/${id}/update`,
      data,
    }),

  delete: (id: string) =>
    request<void>({
      method: 'POST',
      url: `/novel/character/${id}/delete`,
    }),
}
