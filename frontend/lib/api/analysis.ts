import { request } from './client'

export interface PlotAnalysisResult {
  id: number
  projectId: number
  chapterId: number
  plotStage?: string
  conflictLevel?: number
  conflictTypes?: string[]
  emotionalTone?: string
  emotionalIntensity?: number
  hooks?: Array<Record<string, unknown>>
  hooksCount?: number
  hooksAvgStrength?: number
  foreshadows?: unknown
  foreshadowsPlanted?: number
  foreshadowsResolved?: number
  overallQualityScore?: number
  pacingScore?: number
  engagementScore?: number
  coherenceScore?: number
  analysisReport?: string
  suggestions?: string[]
  aiModel?: string
  createTime?: string
  updateTime?: string
}

export const analysisApi = {
  analyzeChapter: (chapterId: string | number, force = false) =>
    request<PlotAnalysisResult>({
      method: 'POST',
      url: '/novel/analysis/analyze-chapter',
      data: { chapterId, force },
    }),

  getByChapterId: (chapterId: string | number) =>
    request<PlotAnalysisResult>({
      method: 'GET',
      url: `/novel/analysis/${chapterId}`,
    }),
}

