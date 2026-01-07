import { request } from './client'
import type { Project, ProjectCreateRequest, ProjectUpdateRequest } from '@/lib/types/project'
import type { PageResult, PageRequest } from '@/lib/types/api'
import type { ProjectStatistics } from '@/lib/types/project'

export const projectApi = {
  // 创建项目
  create: (data: ProjectCreateRequest) =>
    request<Project>({
      method: 'POST',
      url: '/novel/project/create',
      data,
    }),

  // 获取项目详情
  getById: (id: string) =>
    request<Project>({
      method: 'GET',
      url: '/novel/project/detail',
      params: { id },
    }),

  // 更新项目
  update: (id: string, data: ProjectUpdateRequest) =>
    request<void>({
      method: 'POST',
      url: '/novel/project/update',
      data: { ...data, id },
    }),

  // 删除项目
  delete: (id: string) =>
    request<void>({
      method: 'POST',
      url: '/novel/project/delete',
      params: { id },
    }),

  // 获取项目列表
  list: (params: PageRequest & { status?: string }) =>
    request<PageResult<Project>>({
      method: 'GET',
      url: '/novel/project/list',
      params,
    }),

  // 获取项目统计
  getStatistics: (id: string) =>
    request<ProjectStatistics>({
      method: 'GET',
      url: `/novel/project/${id}/statistics`,
    }),
}
