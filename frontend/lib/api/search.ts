import { request } from './client'

export interface GlobalSearchResult {
  projects: Array<{
    id: number
    title: string
    description?: string
  }>
  chapters: Array<{
    id: number
    projectId: number
    chapterNumber?: number
    title?: string
    summary?: string
  }>
  outlines: Array<{
    id: number
    projectId: number
    orderIndex?: number
    title?: string
    content?: string
  }>
  characters: Array<{
    id: number
    projectId: number
    name: string
    roleType?: string
    isOrganization?: number
  }>
  memories: Array<{
    id: number
    projectId: number
    chapterId: number
    memoryType: string
    title: string
    content?: string
  }>
}

export const searchApi = {
  search: (keyword: string, limit = 5) =>
    request<GlobalSearchResult>({
      method: 'GET',
      url: '/novel/search',
      params: { keyword, limit },
    }),
}

