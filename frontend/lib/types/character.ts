// 角色类型
export type RoleType = 'protagonist' | 'supporting' | 'antagonist'

// 角色/组织实体 - 对应 CharacterVO
export interface Character {
  id: number
  projectId: number
  name: string
  isOrganization: number  // 0=角色, 1=组织

  // 角色信息
  roleType?: RoleType
  age?: number
  gender?: string
  appearance?: string
  personality?: string
  background?: string
  relationships?: Record<string, unknown>

  // 组织信息
  organizationType?: string
  organizationPurpose?: string
  organizationMembers?: string[]

  createTime: string
}

// 角色统计 - 对应 CharacterStatisticsVO
export interface CharacterStatistics {
  total: number
  protagonistCount: number
  supportingCount: number
  antagonistCount: number
  organizationCount: number
}

// 角色生成请求 - 对应 CharacterGenerateRequest
export interface CharacterGenerateRequest {
  projectId: number
  protagonistCount?: number
  supportingCount?: number
  antagonistCount?: number
  organizationCount?: number
  customRequirements?: string
}

// 角色创建请求
export interface CharacterCreateRequest {
  projectId: number
  name: string
  isOrganization?: number
  roleType?: RoleType
  age?: number
  gender?: string
  appearance?: string
  personality?: string
  background?: string
  relationships?: Record<string, unknown>
  organizationType?: string
  organizationPurpose?: string
  organizationMembers?: string[]
}

// 角色更新请求
export interface CharacterUpdateRequest {
  id: number
  name?: string
  roleType?: RoleType
  age?: number
  gender?: string
  appearance?: string
  personality?: string
  background?: string
  relationships?: Record<string, unknown>
  organizationType?: string
  organizationPurpose?: string
  organizationMembers?: string[]
}
