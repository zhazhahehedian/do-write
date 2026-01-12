import { request } from './client'
import type {
  AuthUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  OAuthProvider,
  OAuthLoginUrlResponse,
  OAuthBinding
} from '@/lib/types/auth'

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

// ==================== OAuth API ====================

export const oauthApi = {
  /**
   * 获取 OAuth 授权 URL
   */
  getAuthorizeUrl: (provider: OAuthProvider) =>
    request<OAuthLoginUrlResponse>({
      method: 'GET',
      url: `/oauth/${provider}/authorize`,
    }),

  /**
   * 解绑 OAuth
   */
  unbind: (provider: OAuthProvider) =>
    request<void>({
      method: 'POST',
      url: '/oauth/unbind',
      params: { provider },
    }),

  /**
   * 获取用户的 OAuth 绑定列表
   */
  getBindings: () =>
    request<OAuthBinding[]>({
      method: 'GET',
      url: '/oauth/bindings',
    }),
}
