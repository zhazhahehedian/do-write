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
