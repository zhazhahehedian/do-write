// 大纲实体 - 对应 OutlineVO
export interface Outline {
  id: number
  projectId: number
  orderIndex: number
  title: string
  content?: string
  structure?: Record<string, unknown>
  createTime: string
}

// 大纲生成请求 - 对应 OutlineGenerateRequest
export interface OutlineGenerateRequest {
  projectId: number
  outlineCount?: number
  customRequirements?: string
}

// 大纲创建请求
export interface OutlineCreateRequest {
  projectId: number
  orderIndex: number
  title: string
  content?: string
  structure?: Record<string, unknown>
}

// 大纲更新请求
export interface OutlineUpdateRequest {
  id: number
  orderIndex?: number
  title?: string
  content?: string
  structure?: Record<string, unknown>
}

// 大纲列表项 (简化版)
export interface OutlineListItem {
  id: number
  orderIndex: number
  title: string
  hasChapter: boolean  // 是否已生成章节
}
