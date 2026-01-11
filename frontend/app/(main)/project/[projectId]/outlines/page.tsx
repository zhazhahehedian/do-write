'use client'

import { useCallback, useState } from 'react'
import { useParams } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { OutlineList } from '@/components/novel/outline/outline-list'
import { OutlineFormDialog } from '@/components/novel/outline/outline-form-dialog'
import { ChapterGenerateDialog } from '@/components/novel/chapter/chapter-generate-dialog'
import { OutlineExpandDialog } from '@/components/novel/outline/outline-expand-dialog'
import { outlineApi } from '@/lib/api/outline'
import { projectApi } from '@/lib/api/project'
import { chapterApi } from '@/lib/api/chapter'
import { toast } from 'sonner'
import type { Outline } from '@/lib/types/outline'
import type { GenerationTask } from '@/lib/types/wizard'
import { useTaskPolling } from '@/hooks/use-task-polling'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Progress } from '@/components/ui/progress'
import { Button } from '@/components/ui/button'
import { Loader2 } from 'lucide-react'

export default function OutlinesPage() {
  const params = useParams()
  const projectId = params.projectId as string
  const queryClient = useQueryClient()

  const [formDialogOpen, setFormDialogOpen] = useState(false)
  const [editingOutline, setEditingOutline] = useState<Outline | null>(null)
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null)
  const [generateDialogOpen, setGenerateDialogOpen] = useState(false)
  const [generatingOutline, setGeneratingOutline] = useState<Outline | null>(null)
  const [expandDialogOpen, setExpandDialogOpen] = useState(false)
  const [expandingOutline, setExpandingOutline] = useState<Outline | null>(null)

  // 一纲多章：批量生成子章节任务状态
  const [batchTask, setBatchTask] = useState<GenerationTask | null>(null)
  const [batchProgress, setBatchProgress] = useState(0)
  const [batchStep, setBatchStep] = useState('')

  // 获取项目信息以确定 outlineMode
  const { data: project } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.getById(projectId),
  })

  const { data: outlines, isLoading } = useQuery({
    queryKey: ['outlines', projectId],
    queryFn: () => outlineApi.list(projectId),
  })

  const deleteMutation = useMutation({
    mutationFn: outlineApi.delete,
    onSuccess: () => {
      toast.success('大纲已删除')
      setPendingDeleteId(null)
      queryClient.invalidateQueries({ queryKey: ['outlines', projectId] })
    },
    onError: (error: any) => {
      toast.error(error.message || '删除失败')
    },
  })

  const handleAdd = () => {
    setEditingOutline(null)
    setFormDialogOpen(true)
  }

  const handleEdit = (outline: Outline) => {
    setEditingOutline(outline)
    setFormDialogOpen(true)
  }

  const handleGenerateChapter = (outline: Outline) => {
    setGeneratingOutline(outline)
    setGenerateDialogOpen(true)
  }

  const handleExpandOutline = (outline: Outline) => {
    setExpandingOutline(outline)
    setExpandDialogOpen(true)
  }

  const handleBatchTaskComplete = useCallback(async (task: GenerationTask) => {
    setBatchProgress(100)
    setBatchStep('完成')
    toast.success(`子章节生成完成：成功 ${task.result?.completedChapters ?? 0} 个`)
    queryClient.invalidateQueries({ queryKey: ['chapters', projectId] })
    setBatchTask(null)
  }, [projectId, queryClient])

  const handleBatchTaskError = useCallback((task: GenerationTask) => {
    toast.error(task.errorMessage || '子章节生成失败')
    setBatchTask(null)
    setBatchProgress(0)
    setBatchStep('')
  }, [])

  const handleBatchProgress = useCallback((prog: number, step: string) => {
    setBatchProgress(prog)
    setBatchStep(step)
  }, [])

  const { startPolling, stopPolling } = useTaskPolling({
    interval: 2000,
    onComplete: handleBatchTaskComplete,
    onError: handleBatchTaskError,
    onProgress: handleBatchProgress,
  })

  const generateExpandedMutation = useMutation({
    mutationFn: (outline: Outline) => outlineApi.generateExpandedChaptersAsync(outline.id.toString(), {}),
    onSuccess: (task) => {
      setBatchTask(task)
      setBatchProgress(task.progress)
      setBatchStep(task.currentStep)
      startPolling(task.id)
      toast.success('已提交子章节生成任务')
    },
    onError: (error: any) => {
      toast.error(error.message || '提交子章节生成任务失败')
    },
  })

  const handleGenerateExpandedChapters = (outline: Outline) => {
    generateExpandedMutation.mutate(outline)
  }

  const handleCloseBatchDialog = () => {
    stopPolling()
    setBatchTask(null)
    setBatchProgress(0)
    setBatchStep('')
  }

  const handleCancelBatchTask = async () => {
    if (!batchTask) return
    try {
      await chapterApi.cancelTask(batchTask.id)
      toast.success('任务已取消')
    } catch (e: any) {
      toast.error(e.message || '取消任务失败')
    } finally {
      handleCloseBatchDialog()
    }
  }

  const outlineList = outlines || []
  const nextOrderIndex = outlineList.length > 0
    ? Math.max(...outlineList.map(o => o.orderIndex)) + 1
    : 1

  const pendingDeleteOutline = pendingDeleteId
    ? outlineList.find((o) => o.id?.toString() === pendingDeleteId)
    : undefined

  return (
    <>
      <OutlineList
        outlines={outlineList}
        isLoading={isLoading}
        outlineMode={project?.outlineMode}
        onAdd={handleAdd}
        onEdit={handleEdit}
        onDelete={(id) => setPendingDeleteId(id)}
        onGenerateChapter={handleGenerateChapter}
        onExpandOutline={handleExpandOutline}
        onGenerateExpandedChapters={handleGenerateExpandedChapters}
      />

      <OutlineFormDialog
        open={formDialogOpen}
        onOpenChange={setFormDialogOpen}
        projectId={projectId}
        outline={editingOutline}
        nextOrderIndex={nextOrderIndex}
      />

      <ChapterGenerateDialog
        open={generateDialogOpen}
        onOpenChange={setGenerateDialogOpen}
        projectId={projectId}
        outline={generatingOutline}
      />

      <OutlineExpandDialog
        open={expandDialogOpen}
        onOpenChange={setExpandDialogOpen}
        projectId={projectId}
        outline={expandingOutline}
      />

      <Dialog open={!!batchTask} onOpenChange={(open) => { if (!open) handleCloseBatchDialog() }}>
        <DialogContent className="sm:max-w-[520px]">
          <DialogHeader>
            <DialogTitle>正在批量生成子章节</DialogTitle>
          </DialogHeader>

          <div className="space-y-3">
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">{batchStep || '准备中...'}</span>
              <span>{batchProgress}%</span>
            </div>
            <Progress value={batchProgress} className="h-2" />
            <p className="text-xs text-muted-foreground">
              该任务在后台执行，你可以关闭弹窗稍后再看；取消任务会在下一子章节开始前生效。
            </p>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={handleCloseBatchDialog}>
              关闭
            </Button>
            <Button
              variant="destructive"
              onClick={handleCancelBatchTask}
              disabled={!batchTask}
            >
              取消任务
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog
        open={pendingDeleteId !== null}
        onOpenChange={(open) => {
          if (deleteMutation.isPending) return
          if (!open) setPendingDeleteId(null)
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除大纲？</AlertDialogTitle>
            <AlertDialogDescription>
              {pendingDeleteOutline
                ? `删除后将无法恢复，且会永久删除「${pendingDeleteOutline.title}」。`
                : '删除后将无法恢复。'}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMutation.isPending}>取消</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              disabled={deleteMutation.isPending}
              onClick={(event) => {
                event.preventDefault()
                if (!pendingDeleteId) return
                deleteMutation.mutate(pendingDeleteId)
              }}
            >
              {deleteMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
