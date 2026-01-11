import { request } from './client'
import type { UserApiConfig, UserApiConfigCreateRequest, UserApiConfigUpdateRequest } from '@/lib/types/user-config'

export const userConfigApi = {
  list: (userId: string) =>
    request<UserApiConfig[]>({
      method: 'GET',
      url: '/userConfig/list',
      params: { userId },
    }),

  create: (data: UserApiConfigCreateRequest) =>
    request<number>({
      method: 'POST',
      url: '/userConfig/save',
      data,
    }),

  update: (data: UserApiConfigUpdateRequest) =>
    request<boolean>({
      method: 'POST',
      url: '/userConfig/update',
      data,
    }),

  delete: (userId: string, id: string) =>
    request<boolean>({
      method: 'POST',
      url: '/userConfig/delete',
      data: { userId, id },
    }),
    
  setActive: (userId: string, id: string) =>
    request<boolean>({
        method: 'POST',
        url: '/userConfig/setDefault',
        data: { userId, id }
    }),

  validate: (data: UserApiConfigCreateRequest) =>
    request<boolean>({
      method: 'POST',
      url: '/userConfig/validate',
      data,
    }),
}
