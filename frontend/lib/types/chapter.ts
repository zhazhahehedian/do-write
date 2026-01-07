// 章节内容状态
export type ChapterStatus = 'draft' | 'published' | 'archived'

// 生成状态
export type GenerationStatus = 'pending' | 'generating' | 'completed' | 'failed'

// 润色类型
export type PolishType = 'enhance_description' | 'fix_grammar' | 'adjust_pacing' | 'all'

// 任务类型
export type TaskType = 'single_chapter' | 'batch_chapter' | 'analysis'

// 任务状态
export type TaskStatus = 'pending' | 'running' | 'completed' | 'failed' | 'cancelled'

// 章节基本信息 (列表项) - 对应 ChapterVO
export interface Chapter {
  id: number
  projectId: number
  outlineId?: number
  chapterNumber: number
  subIndex: number
  title: string
  wordCount: number
  status: ChapterStatus
  generationStatus: GenerationStatus
  aiModel?: string
  styleCode?: string
  styleName?: string
  version: number
  createTime: string
  updateTime: string
}

// 章节详情 (包含完整内容) - 对应 ChapterDetailVO
export interface ChapterDetail extends Chapter {
  content?: string
  summary?: string
  expansionPlan?: Record<string, unknown>
  generationParams?: Record<string, unknown>

  // 关联信息
  outlineTitle?: string
  outlineContent?: string

  // 上下文信息
  previousChapter?: Chapter
  nextChapter?: Chapter
}

// 章节摘要 (用于上下文构建) - 对应 ChapterSummaryVO
export interface ChapterSummary {
  id: number
  chapterNumber: number
  subIndex: number
  title: string
  summary?: string
  wordCount: number
}

// 章节生成请求 - 对应 ChapterGenerateRequest
export interface ChapterGenerateRequest {
  projectId: number
  outlineId: number
  subIndex?: number
  styleCode?: string
  targetWordCount?: number
  narrativePerspective?: string
  customRequirements?: string
  temperature?: number
  topP?: number
  enableMemoryRetrieval?: boolean
}

// 批量生成请求 - 对应 BatchGenerateRequest
export interface BatchGenerateRequest {
  projectId: number
  outlineIds: number[]
  styleCode?: string
  targetWordCount?: number
  enableAnalysis?: boolean
  maxRetries?: number
}

// 章节更新请求 - 对应 ChapterUpdateRequest
export interface ChapterUpdateRequest {
  id: number
  title?: string
  content?: string
  status?: ChapterStatus
  createNewVersion?: boolean
}

// 章节润色请求 - 对应 ChapterPolishRequest
export interface ChapterPolishRequest {
  chapterId: number
  polishType?: PolishType
  styleCode?: string
  customInstructions?: string
}

// 章节查询请求 - 对应 ChapterQueryRequest
export interface ChapterQueryRequest {
  projectId: number
  status?: ChapterStatus
  generationStatus?: GenerationStatus
  outlineId?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// 生成任务响应 - 对应 GenerationTaskVO
export interface GenerationTask {
  id: number
  projectId: number
  taskType: TaskType
  status: TaskStatus
  progress: number
  currentStep?: string
  result?: Record<string, unknown>
  errorMessage?: string
  startedAt?: string
  completedAt?: string
  createTime: string

  // 关联信息
  chapterIds?: number[]
  totalChapters?: number
  completedChapters?: number
}
