export interface LoginRequest {
  username: string
  password?: string
}

export interface LoginResponse {
  token: string
  tokenType: string
}

export interface RegisterRequest {
  username: string
  password?: string
  email?: string
}

/**
 * 登录态用户信息
 * 与后端 `/auth/user/info` 返回结构保持一致（至少包含这些字段）。
 */
export interface AuthUser {
  id: string
  username: string
  email?: string
  avatar?: string
  userType: 'NORMAL' | 'ADMIN'
}

// ==================== OAuth 相关类型 ====================

/**
 * OAuth 提供商
 */
export type OAuthProvider = 'linuxdo' | 'fishpi' | 'github'

/**
 * OAuth 登录 URL 响应
 */
export interface OAuthLoginUrlResponse {
  authorizeUrl: string
  state: string | null
  provider: string
}

/**
 * OAuth 绑定信息
 */
export interface OAuthBinding {
  id: string
  userId: string
  oauthType: string
  oauthId: string
  oauthUserName?: string
  oauthNickname?: string
  oauthAvatar?: string
  oauthEmail?: string
  trustLevel?: number
  status: number
  bindTime: string
}
