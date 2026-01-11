'use client'

import { useState } from 'react'
import { useParams } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { OutlineList } from '@/components/novel/outline/outline-list'
import { OutlineFormDialog } from '@/components/novel/outline/outline-form-dialog'
import { ChapterGenerateDialog } from '@/components/novel/chapter/chapter-generate-dialog'
import { OutlineExpandDialog } from '@/components/novel/outline/outline-expand-dialog'
import { outlineApi } from '@/lib/api/outline'
import { projectApi } from '@/lib/api/project'
import { toast } from 'sonner'
import type { Outline } from '@/lib/types/outline'
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
