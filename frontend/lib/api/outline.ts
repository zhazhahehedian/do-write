import { request } from './client'
import type { Outline } from '@/lib/types/outline'

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
}
