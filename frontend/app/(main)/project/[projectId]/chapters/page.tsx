'use client'

import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ChapterList } from '@/components/novel/chapter/chapter-list'
import { chapterApi } from '@/lib/api/chapter'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Loader2, Plus, FileText } from 'lucide-react'
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

export default function ChaptersPage() {
  const params = useParams()
  const router = useRouter()
  const projectId = params.projectId as string
  const queryClient = useQueryClient()
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['chapters', projectId],
    queryFn: () => chapterApi.list(projectId, { pageNum: 1, pageSize: 100 }),
  })

  const deleteMutation = useMutation({
    mutationFn: chapterApi.delete,
    onSuccess: () => {
      toast.success('章节已删除')
      setPendingDeleteId(null)
      queryClient.invalidateQueries({ queryKey: ['chapters', projectId] })
    },
    onError: (error: any) => {
      toast.error(error.message || '删除失败')
    },
  })

  const handleNavigateToOutlines = () => {
    router.push(`/project/${projectId}/outlines`)
  }

  const chapters = data?.list || []
  const pendingDeleteChapter = pendingDeleteId
    ? chapters.find((chapter: any) => chapter.id?.toString() === pendingDeleteId)
    : undefined

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="text-sm text-muted-foreground">
          共 {chapters.length} 章
        </div>
        <Button onClick={handleNavigateToOutlines}>
          <Plus className="mr-2 h-4 w-4" />
          从大纲生成章节
        </Button>
      </div>

      {chapters.length === 0 && !isLoading ? (
        <div className="flex h-[400px] flex-col items-center justify-center gap-4 rounded-lg border border-dashed text-center">
          <div className="rounded-full bg-muted p-4">
            <FileText className="h-8 w-8 text-muted-foreground" />
          </div>
          <div>
            <h3 className="text-lg font-medium">暂无章节</h3>
            <p className="text-sm text-muted-foreground">
              前往大纲页面，从大纲生成章节内容
            </p>
          </div>
          <Button onClick={handleNavigateToOutlines}>
            <Plus className="mr-2 h-4 w-4" />
            从大纲生成章节
          </Button>
        </div>
      ) : (
        <ChapterList
          chapters={chapters}
          projectId={projectId}
          onDelete={(id) => setPendingDeleteId(id)}
        />
      )}

      <AlertDialog
        open={pendingDeleteId !== null}
        onOpenChange={(open) => {
          if (deleteMutation.isPending) return
          if (!open) setPendingDeleteId(null)
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除章节？</AlertDialogTitle>
            <AlertDialogDescription>
              {pendingDeleteChapter
                ? `删除后将无法恢复，且会永久删除「${pendingDeleteChapter.title ?? '该章节'}」。`
                : '删除后将无法恢复，且会永久删除该章节。'}
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
    </div>
  )
}
