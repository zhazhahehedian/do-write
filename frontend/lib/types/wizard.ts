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

// 重新导出 character 和 outline 的生成请求
export type { CharacterGenerateRequest } from './character'
export type { OutlineGenerateRequest } from './outline'
