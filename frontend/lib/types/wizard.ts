// 世界观信息 - 对应 WorldVO
export interface WorldInfo {
  worldTimePeriod?: string
  worldLocation?: string
  worldAtmosphere?: string
  worldRules?: string
}

// 世界观生成请求 - 对应 WorldGenerateRequest
export interface WorldGenerateRequest {
  projectId: number
  customRequirements?: string
}

// 向导状态更新请求 - 对应 WizardStatusUpdateRequest
export interface WizardStatusUpdateRequest {
  projectId: number
  status: string  // not_started/in_progress/completed
  step: number    // 0-3
}

// 向导进度信息 - 对应 WizardProgressVO
export interface WizardProgress {
  status: string
  currentStep: number
  worldGenerated: boolean
  characterCount: number
  outlineCount: number
}

// 向导专用任务类型 (扩展 chapter 中的 TaskType)
export type WizardTaskType = 'characters' | 'outlines'

// 向导专用的任务状态
export type TaskStatus = 'pending' | 'running' | 'completed' | 'failed' | 'cancelled'

// 向导生成任务信息 - 对应 GenerationTaskVO (向导专用版本)
export interface GenerationTask {
  id: string
  projectId: string
  taskType: WizardTaskType | 'single_chapter' | 'batch_chapter' | 'analysis'
  status: TaskStatus
  progress: number
  currentStep: string
  result?: Record<string, any>
  errorMessage?: string
  startedAt?: string
  completedAt?: string
  createTime: string
  chapterIds?: string[]
  totalChapters?: number
  completedChapters?: number
}

// 重新导出 character 和 outline 的生成请求
export type { CharacterGenerateRequest } from './character'
export type { OutlineGenerateRequest } from './outline'
