import { request } from "./client"

export interface MemoryStatistics {
  projectId: number
  totalCount: number
  typeCount: Record<string, number>
  pendingForeshadowCount: number
  resolvedForeshadowCount: number
  coveredChapterCount: number
}

export const memoryApi = {
  // 获取记忆统计信息
  getStatistics: (projectId: string) =>
    request<MemoryStatistics>({
      method: "GET",
      url: "/novel/memory/statistics",
      params: { projectId },
    }),
}

