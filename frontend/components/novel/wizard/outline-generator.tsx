'use client'

import { useState, useCallback } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Progress } from '@/components/ui/progress'
import { wizardApi } from '@/lib/api/wizard'
import { outlineApi } from '@/lib/api/outline'
import { useTaskPolling } from '@/hooks/use-task-polling'
import { FileText, RefreshCw, Check, Loader2, BookOpen } from 'lucide-react'
import { toast } from 'sonner'
import type { Outline } from '@/lib/types/outline'
import type { GenerationTask } from '@/lib/types/wizard'

interface OutlineGeneratorProps {
  projectId: string
  readOnly?: boolean
  onComplete?: (outlines: Outline[]) => void
}

export function OutlineGenerator({
  projectId,
  readOnly = false,
  onComplete,
}: OutlineGeneratorProps) {
  const [outlineCount, setOutlineCount] = useState(10)
  const [customRequirements, setCustomRequirements] = useState('')
  const [isGenerating, setIsGenerating] = useState(false)
  const [progress, setProgress] = useState(0)
  const [currentStep, setCurrentStep] = useState('')

  const queryClient = useQueryClient()

  // 获取已有大纲列表
  const { data: outlines, isLoading: isLoadingOutlines, refetch } = useQuery({
    queryKey: ['outlines', projectId],
    queryFn: () => outlineApi.list(projectId),
    enabled: !!projectId,
  })

  // 任务完成时的处理
  const handleTaskComplete = useCallback(async (task: GenerationTask) => {
    try {
      // 刷新大纲列表
      await refetch()
      setIsGenerating(false)
      setProgress(100)
      setCurrentStep('完成')

      const count = task.result?.outlineCount || 0
      toast.success(`成功生成 ${count} 个大纲`)

      // 刷新相关查询
      queryClient.invalidateQueries({ queryKey: ['outlines', projectId] })
    } catch (error: any) {
      toast.error('获取大纲列表失败')
      setIsGenerating(false)
    }
  }, [projectId, queryClient, refetch])

  // 任务失败时的处理
  const handleTaskError = useCallback((task: GenerationTask) => {
    setIsGenerating(false)
    setProgress(0)
    setCurrentStep('')
    toast.error(task.errorMessage || '大纲生成失败')
  }, [])

  // 进度更新时的处理
  const handleProgress = useCallback((prog: number, step: string) => {
    setProgress(prog)
    setCurrentStep(step)
  }, [])

  // 使用轮询 hook
  const { startPolling, stopPolling } = useTaskPolling({
    interval: 2000,
    onComplete: handleTaskComplete,
    onError: handleTaskError,
    onProgress: handleProgress,
  })

  // 提交异步任务
  const submitMutation = useMutation({
    mutationFn: () => wizardApi.generateOutlinesAsync(projectId, {
      outlineCount,
      customRequirements: customRequirements || undefined,
    }),
    onSuccess: (task) => {
      setIsGenerating(true)
      setProgress(task.progress)
      setCurrentStep(task.currentStep)
      // 开始轮询任务状态
      startPolling(task.id)
    },
    onError: (error: any) => {
      toast.error(error.message || '提交生成任务失败')
    },
  })

  const handleGenerate = () => {
    if (readOnly) return
    submitMutation.mutate()
  }

  const handleCancel = () => {
    stopPolling()
    setIsGenerating(false)
    setProgress(0)
    setCurrentStep('')
  }

  const outlineList = outlines || []
  const hasOutlines = outlineList.length > 0
  const isPending = submitMutation.isPending || isGenerating

  return (
    <div className="space-y-6">
      {readOnly && (
        <div className="rounded-lg border border-primary/30 bg-primary/5 px-4 py-3 text-sm text-muted-foreground">
          该步骤已完成，当前为回看模式。
        </div>
      )}

      {/* 生成进度 */}
      {isGenerating && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Loader2 className="h-5 w-5 animate-spin" />
              正在生成大纲...
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">{currentStep}</span>
                <span>{progress}%</span>
              </div>
              <Progress value={progress} className="h-2" />
            </div>
            <p className="text-sm text-muted-foreground">
              AI 正在根据世界观和角色设定生成大纲，这可能需要 1-3 分钟...
            </p>
            <Button variant="outline" size="sm" onClick={handleCancel}>
              取消
            </Button>
          </CardContent>
        </Card>
      )}

      {/* 配置表单 - 无大纲且未在生成中 */}
      {!hasOutlines && !isGenerating && (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <FileText className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-medium mb-2">生成故事大纲</h3>
            <p className="text-muted-foreground text-center mb-6 max-w-md">
              AI 将根据世界观和角色设定，生成完整的故事大纲和章节规划。
            </p>

            <div className="w-full max-w-sm space-y-4">
              <div className="space-y-2">
                <Label>大纲数量</Label>
                <Input
                  type="number"
                  min={3}
                  max={50}
                  value={outlineCount}
                  onChange={(e) => setOutlineCount(parseInt(e.target.value) || 10)}
                />
                <p className="text-xs text-muted-foreground">建议 10-30 章，每章对应一个大纲</p>
              </div>

              <div className="space-y-2">
                <Label>自定义要求 (可选)</Label>
                <Textarea
                  placeholder="例如：希望故事有反转结局、注重情感描写..."
                  value={customRequirements}
                  onChange={(e) => setCustomRequirements(e.target.value)}
                  className="resize-none"
                  rows={3}
                />
              </div>

              <Button onClick={handleGenerate} disabled={isPending || readOnly} className="w-full">
                {isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    正在提交...
                  </>
                ) : (
                  <>
                    <FileText className="mr-2 h-4 w-4" />
                    生成 {outlineCount} 个大纲
                  </>
                )}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* 生成完成后显示大纲列表 */}
      {hasOutlines && !isGenerating && (
        <>
          {isLoadingOutlines ? (
            <div className="flex justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-medium flex items-center gap-2">
                  <BookOpen className="h-5 w-5" />
                  大纲列表 ({outlineList.length} 章)
                </h3>
              </div>

              <div className="grid gap-3">
                {outlineList.map((outline: Outline, index: number) => (
                  <OutlineCard key={outline.id} outline={outline} index={index} />
                ))}
              </div>
            </div>
          )}

          {/* 操作按钮 */}
          <div className="flex justify-between">
            <Button variant="outline" onClick={handleGenerate} disabled={isPending || readOnly}>
              {isPending ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : (
                <RefreshCw className="mr-2 h-4 w-4" />
              )}
              重新生成
            </Button>
            {!readOnly && (
              <Button onClick={() => onComplete?.(outlineList)}>
                <Check className="mr-2 h-4 w-4" />
                完成向导
              </Button>
            )}
          </div>
        </>
      )}
    </div>
  )
}

interface OutlineCardProps {
  outline: Outline
  index: number
}

function OutlineCard({ outline, index }: OutlineCardProps) {
  return (
    <Card className="hover:shadow-sm transition-shadow">
      <CardContent className="py-3 px-4">
        <div className="flex items-start gap-3">
          <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-sm font-medium text-primary">
            {index + 1}
          </div>
          <div className="flex-1 min-w-0">
            <h4 className="font-medium text-sm truncate">{outline.title}</h4>
            {outline.content && (
              <p className="text-sm text-muted-foreground mt-1 line-clamp-2">
                {outline.content}
              </p>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
