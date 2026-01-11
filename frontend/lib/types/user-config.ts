// API类型
export type ApiType = 'OPENAI' | 'AZURE_OPENAI' | 'OLLAMA' | 'CUSTOM'

// 用户API配置 - 对应 UserApiConfigVO
export interface UserApiConfig {
  id: number
  userId: number
  configName: string
  apiType: ApiType
  apiKeyMasked: string  // 脱敏后的API Key
  baseUrl?: string
  modelName?: string
  maxTokens?: number
  temperature?: number
  isDefault: number  // 0-否, 1-是
  status: number     // 0-禁用, 1-启用
  remark?: string
  createTime: string
  updateTime: string
}

// 用户API配置创建请求 - 对应 UserApiConfigRequest
export interface UserApiConfigCreateRequest {
  userId: string  // 使用 string 避免大整数精度丢失
  configName: string
  apiType: ApiType
  apiKey: string      // 明文，保存时会自动加密
  baseUrl?: string
  modelName?: string
  maxTokens?: number
  temperature?: number
  isDefault?: number
  remark?: string
}

// 用户API配置更新请求
export interface UserApiConfigUpdateRequest {
  userId: string  // 使用 string 避免大整数精度丢失
  id: string      // 使用 string 避免大整数精度丢失
  configName?: string
  apiType?: ApiType
  apiKey?: string      // 可选，不传则保持原值
  baseUrl?: string
  modelName?: string
  maxTokens?: number
  temperature?: number
  isDefault?: number
  status?: number
  remark?: string
}
