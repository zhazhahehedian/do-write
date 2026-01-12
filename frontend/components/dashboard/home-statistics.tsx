"use client"

import * as React from "react"
import Link from "next/link"
import { useQuery } from "@tanstack/react-query"
import { BookOpen, FileText, FolderOpen, Loader2, Sparkles, Users } from "lucide-react"

import { projectApi } from "@/lib/api/project"
import { memoryApi } from "@/lib/api/memory"
import type { ProjectListVO, ProjectStatistics } from "@/lib/types/project"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

const DASHBOARD_PROJECT_KEY = "dashboard_project_id"

function safeNumber(value: number | null | undefined) {
  return Number.isFinite(value as number) ? (value as number) : 0
}

function formatWords(value: number) {
  if (value >= 10000) return `${(value / 10000).toFixed(1)} 万`
  return `${value}`
}

function calcPercent(numerator: number, denominator: number) {
  if (denominator <= 0) return 0
  return Math.max(0, Math.min(100, Math.round((numerator / denominator) * 100)))
}

export function HomeStatistics() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["projects", "dashboard"],
    queryFn: () => projectApi.list({ pageNum: 1, pageSize: 50 }),
  })

  const projects = React.useMemo(() => {
    return (data?.list ?? []) as unknown as ProjectListVO[]
  }, [data])

  const defaultProjectId = React.useMemo(() => {
    if (typeof window === "undefined") return null
    const stored = window.localStorage.getItem(DASHBOARD_PROJECT_KEY)
    if (stored) return stored
    if (projects.length === 0) return null

    const sorted = [...projects].sort((a, b) => {
      const at = new Date(a.updateTime).getTime()
      const bt = new Date(b.updateTime).getTime()
      return bt - at
    })
    return String(sorted[0]?.id ?? "")
  }, [projects])

  const [projectId, setProjectId] = React.useState<string | null>(null)

  React.useEffect(() => {
    if (!projectId && defaultProjectId) {
      setProjectId(defaultProjectId)
    }
  }, [defaultProjectId, projectId])

  const handleProjectChange = React.useCallback((nextId: string) => {
    setProjectId(nextId)
    if (typeof window !== "undefined") {
      window.localStorage.setItem(DASHBOARD_PROJECT_KEY, nextId)
    }
  }, [])

  const { data: statistics, isLoading: isStatisticsLoading } = useQuery({
    queryKey: ["project-statistics", projectId],
    enabled: Boolean(projectId),
    queryFn: () => projectApi.getStatistics(projectId as string),
  })

  const { data: memoryStatistics, isLoading: isMemoryLoading } = useQuery({
    queryKey: ["memory-statistics", projectId],
    enabled: Boolean(projectId),
    queryFn: () => memoryApi.getStatistics(projectId as string),
  })

  const quickProjectId = projectId ?? undefined

  if (isLoading) {
    return (
      <div className="flex h-[360px] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex h-[360px] flex-col items-center justify-center gap-4 text-center">
        <p className="text-muted-foreground">加载首页数据失败：{(error as Error).message}</p>
        <Button asChild>
          <Link href="/projects">去项目列表</Link>
        </Button>
      </div>
    )
  }

  if (projects.length === 0) {
    return (
      <div className="flex h-[360px] flex-col items-center justify-center gap-4 rounded-lg border border-dashed text-center">
        <div className="rounded-full bg-muted p-4">
          <FolderOpen className="h-8 w-8 text-muted-foreground" />
        </div>
        <div>
          <h3 className="text-lg font-medium">暂无项目</h3>
          <p className="text-sm text-muted-foreground">先创建一个项目，再来查看统计概览</p>
        </div>
        <Button asChild>
          <Link href="/projects">创建项目</Link>
        </Button>
      </div>
    )
  }

  const stat = (statistics ?? null) as ProjectStatistics | null
  const totalTargetWords = safeNumber(stat?.targetWords)
  const totalCurrentWords = safeNumber(stat?.currentWords)
  const progressPercent =
    safeNumber(stat?.progressPercent) || calcPercent(totalCurrentWords, totalTargetWords)

  const totalChapters = safeNumber(stat?.totalChapters)

  const totalCharacters = safeNumber(stat?.totalCharacters)
  const totalOutlines = safeNumber(stat?.totalOutlines)

  const memoryTotal = safeNumber(memoryStatistics?.totalCount)
  const foreshadowPending = safeNumber(memoryStatistics?.pendingForeshadowCount)
  const foreshadowResolved = safeNumber(memoryStatistics?.resolvedForeshadowCount)

  const isDetailLoading = Boolean(projectId) && (isStatisticsLoading || isMemoryLoading)

  return (
    <div className="space-y-6 py-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">统计概览</h1>
          <p className="text-sm text-muted-foreground">
            基于你选择的项目展示当前创作进度与关键统计
          </p>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <Select value={projectId ?? ""} onValueChange={handleProjectChange}>
            <SelectTrigger className="w-[260px]">
              <SelectValue placeholder="选择项目" />
            </SelectTrigger>
            <SelectContent>
              {projects
                .slice()
                .sort((a, b) => new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime())
                .map((p) => (
                  <SelectItem key={p.id} value={String(p.id)}>
                    {p.title}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>

          <Button asChild variant="secondary">
            <Link href="/projects">项目列表</Link>
          </Button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-medium text-muted-foreground">写作进度</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="text-2xl font-semibold">{progressPercent}%</div>
            <Progress value={progressPercent} className="h-2" />
            <div className="text-xs text-muted-foreground">
              {formatWords(totalCurrentWords)} / {formatWords(totalTargetWords)} 字
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-medium text-muted-foreground">章节</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex items-center gap-2">
              <FileText className="h-4 w-4 text-muted-foreground" />
              <div className="text-2xl font-semibold">{totalChapters}</div>
            </div>
            <div className="text-xs text-muted-foreground">
              生成后可直接阅读与编辑
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-medium text-muted-foreground">角色 / 大纲</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex items-center gap-2">
              <Users className="h-4 w-4 text-muted-foreground" />
              <div className="text-2xl font-semibold">{totalCharacters}</div>
            </div>
            <div className="text-xs text-muted-foreground">大纲 {totalOutlines}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-medium text-muted-foreground">记忆 / 伏笔</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex items-center gap-2">
              <BookOpen className="h-4 w-4 text-muted-foreground" />
              <div className="text-2xl font-semibold">{memoryTotal}</div>
            </div>
            <div className="text-xs text-muted-foreground">
              待回收 {foreshadowPending} · 已回收 {foreshadowResolved}
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Button asChild disabled={!quickProjectId || isDetailLoading}>
          <Link href={quickProjectId ? `/project/${quickProjectId}/chapters` : "/projects"}>
            <FileText className="mr-2 h-4 w-4" />
            继续写作
          </Link>
        </Button>
        <Button asChild variant="secondary" disabled={!quickProjectId || isDetailLoading}>
          <Link href={quickProjectId ? `/project/wizard/${quickProjectId}` : "/projects"}>
            <Sparkles className="mr-2 h-4 w-4" />
            创作向导
          </Link>
        </Button>
      </div>
    </div>
  )
}
