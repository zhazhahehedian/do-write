import { request } from './client'
import type { AuthUser, LoginRequest, LoginResponse, RegisterRequest } from '@/lib/types/auth'

export const authApi = {
  login: (data: LoginRequest) =>
    request<LoginResponse>({
      method: 'POST',
      url: '/auth/login',
      data,
    }),

  register: (data: RegisterRequest) =>
    request<void>({
      method: 'POST',
      url: '/auth/register',
      data,
    }),

  logout: () =>
    request<void>({
      method: 'POST',
      url: '/auth/logout',
    }),

  getUserInfo: () =>
    request<AuthUser>({
      method: 'GET',
      url: '/auth/user/info',
    }),
}
