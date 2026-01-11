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

// ==================== 大纲展开 (one-to-many) ====================

// 大纲展开请求
export interface OutlineExpandRequest {
  outlineId: number
  targetChapterCount?: number  // 目标子章节数量 (2-10)
  strategy?: 'balanced' | 'climax' | 'detail'  // 展开策略
  enableSceneAnalysis?: boolean  // 是否启用场景分析
  customRequirements?: string  // 自定义要求
}

// 场景信息
export interface SceneInfo {
  location: string
  characters: string[]
  purpose: string
}

// 章节规划
export interface ChapterPlan {
  subIndex: number
  title: string
  plotSummary: string
  keyEvents?: string[]
  characterFocus?: string[]
  emotionalTone?: string
  narrativeGoal?: string
  conflictType?: string
  estimatedWords?: number
  scenes?: SceneInfo[]
}

// 大纲展开预览结果
export interface OutlineExpandPreview {
  outlineId: number
  outlineTitle: string
  outlineContent?: string
  chapterPlans: ChapterPlan[]
}

// 大纲展开应用请求
export interface OutlineExpandApplyRequest {
  outlineId: number
  chapterPlans: ChapterPlan[]
  force?: boolean  // 是否强制覆盖
}
