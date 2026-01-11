// 项目状态
export type ProjectStatus = 'planning' | 'writing' | 'completed'

// 向导状态
export type WizardStatus = 'not_started' | 'world_generated' | 'characters_generated' | 'outlines_generated' | 'completed'

// 大纲模式
export type OutlineMode = 'one-to-one' | 'one-to-many'

// 项目实体
export interface Project {
  id: number
  userId: number
  title: string
  description?: string
  theme?: string
  genre?: string
  targetWords: number
  currentWords: number
  status: ProjectStatus

  // 世界观设定
  worldTimePeriod?: string
  worldLocation?: string
  worldAtmosphere?: string
  worldRules?: string

  // 创作配置
  chapterCount: number
  narrativePerspective?: string
  characterCount: number
  outlineMode: OutlineMode
  wizardStatus: WizardStatus
  wizardStep: number
  aiModel?: string
  writingStyleCode?: string

  // 时间戳
  createTime: string
  updateTime: string
}

// 项目列表项 (精简版)
export interface ProjectListVO {
  id: number
  title: string
  genre?: string
  theme?: string
  narrativePerspective?: string
  status: ProjectStatus
  targetWords: number
  currentWords: number
  wizardStatus: WizardStatus
  wizardStep: number
  createTime: string
  updateTime: string
  progressPercent: number
}

// 项目统计
export interface ProjectStatistics {
  projectId: number
  title: string

  // 字数统计
  targetWords: number
  currentWords: number
  progressPercent: number

  // 章节统计
  totalChapters: number
  draftChapters: number
  publishedChapters: number

  // 角色统计
  totalCharacters: number
  protagonists: number
  supportingRoles: number
  antagonists: number
  organizations: number

  // 大纲统计
  totalOutlines: number
  completedOutlines: number

  // 记忆统计
  totalMemories: number
  plantedForeshadows: number
  resolvedForeshadows: number
}

// 创建项目请求
export interface ProjectCreateRequest {
  title: string
  description?: string
  theme?: string
  genre?: string
  targetWords?: number
  chapterCount?: number
  narrativePerspective?: string
  outlineMode?: OutlineMode
  aiModel?: string
  writingStyleCode?: string
}

// 更新项目请求
export interface ProjectUpdateRequest {
  id: number
  title?: string
  description?: string
  theme?: string
  genre?: string
  targetWords?: number
  status?: ProjectStatus
  narrativePerspective?: string
  outlineMode?: OutlineMode
  aiModel?: string
  writingStyleCode?: string

  // 世界观更新
  worldTimePeriod?: string
  worldLocation?: string
  worldAtmosphere?: string
  worldRules?: string
}
