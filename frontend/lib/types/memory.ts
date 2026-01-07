// 记忆类型
export type MemoryType = 'plot_point' | 'hook' | 'foreshadow' | 'character_event' | 'location_event'

// 伏笔状态
export type ForeshadowStatus = 0 | 1 | 2  // 0=普通, 1=已埋下, 2=已回收

// 故事记忆 - 对应 StoryMemoryVO
export interface StoryMemory {
  id: number
  projectId: number
  chapterId?: number
  chapterNumber?: number
  memoryType: MemoryType
  title: string
  content: string
  importanceScore: number  // 0.00-1.00
  storyTimeline?: number
  isForeshadow: ForeshadowStatus
  createTime: string

  // 关联信息
  relatedCharacterNames?: string[]
  relatedLocations?: string[]
}

// 记忆统计 - 对应 MemoryStatisticsVO
export interface MemoryStatistics {
  projectId: number
  totalCount: number
  typeCount: Record<string, number>
  pendingForeshadowCount: number
  resolvedForeshadowCount: number
  coveredChapterCount: number
}

// 记忆搜索请求 - 对应 MemorySearchRequest
export interface MemorySearchRequest {
  projectId: number
  query: string
  topK?: number
  threshold?: number
}

// 记忆查询请求 - 对应 StoryMemoryQueryRequest
export interface MemoryQueryRequest {
  projectId: number
  chapterId?: number
  memoryType?: MemoryType
  foreshadowStatus?: ForeshadowStatus
  minImportance?: number
  startTimeline?: number
  endTimeline?: number
  pageNum?: number
  pageSize?: number
}

// 伏笔解决请求 - 对应 ForeshadowResolveRequest
export interface ForeshadowResolveRequest {
  memoryId: number
  resolutionChapterId: number
  resolutionNote?: string
}
