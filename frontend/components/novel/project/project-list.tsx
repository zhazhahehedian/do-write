'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ProjectCard } from './project-card'
import { projectApi } from '@/lib/api/project'
import { Button } from '@/components/ui/button'
import { Loader2, Plus, FolderOpen, AlertCircle, Settings } from 'lucide-react'
import { toast } from 'sonner'
import { ProjectCreateDialog } from './project-create-dialog'
import { useState } from 'react'
import { useApiConfigCheck } from '@/hooks/use-api-config-check'
import Link from 'next/link'
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

export function ProjectList() {
  const queryClient = useQueryClient()
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null)
  const { hasDefaultConfig, isLoading: configLoading } = useApiConfigCheck()

  const { data, isLoading, error } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectApi.list({ pageNum: 1, pageSize: 20 }),
  })

  const deleteMutation = useMutation({
    mutationFn: projectApi.delete,
    onSuccess: () => {
      toast.success('项目已删除')
      setPendingDeleteId(null)
      queryClient.invalidateQueries({ queryKey: ['projects'] })
    },
    onError: (error: any) => {
      toast.error(error.message || '删除失败')
    },
  })

  const handleDelete = (id: string) => {
    setPendingDeleteId(id)
  }

  if (isLoading) {
    return (
      <div className="flex h-[400px] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex h-[400px] flex-col items-center justify-center gap-4 text-center">
        <p className="text-muted-foreground">加载项目失败: {(error as Error).message}</p>
        <Button onClick={() => queryClient.invalidateQueries({ queryKey: ['projects'] })}>
          重试
        </Button>
      </div>
    )
  }

  const projects = data?.list || []
  const pendingDeleteProject = pendingDeleteId
    ? projects.find((project) => project.id?.toString() === pendingDeleteId)
    : undefined

  if (projects.length === 0) {
    return (
      <div className="flex h-[400px] flex-col items-center justify-center gap-4 rounded-lg border border-dashed text-center">
        <div className="rounded-full bg-muted p-4">
          <FolderOpen className="h-8 w-8 text-muted-foreground" />
        </div>
        <div>
          <h3 className="text-lg font-medium">暂无项目</h3>
          <p className="text-sm text-muted-foreground">
            创建一个新项目开始您的创作之旅
          </p>
        </div>
        <Button onClick={() => setCreateDialogOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          创建项目
        </Button>
        <ProjectCreateDialog 
          open={createDialogOpen} 
          onOpenChange={setCreateDialogOpen} 
          onSuccess={() => queryClient.invalidateQueries({ queryKey: ['projects'] })}
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* API 配置提示 */}
      {!configLoading && !hasDefaultConfig && (
        <div className="flex items-center gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-200">
          <AlertCircle className="h-4 w-4 flex-shrink-0" />
          <span>使用创作向导前，请先</span>
          <Link
            href="/settings/api-config"
            className="inline-flex items-center gap-1 font-medium underline underline-offset-4 hover:text-amber-900 dark:hover:text-amber-100"
          >
            <Settings className="h-3.5 w-3.5" />
            配置 API
          </Link>
        </div>
      )}

      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold tracking-tight">我的项目</h2>
        <Button onClick={() => setCreateDialogOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          创建项目
        </Button>
      </div>

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {projects.map((project) => (
          <ProjectCard
            key={project.id}
            project={project}
            onDelete={handleDelete}
          />
        ))}
      </div>
      
      <ProjectCreateDialog 
          open={createDialogOpen} 
          onOpenChange={setCreateDialogOpen} 
          onSuccess={() => queryClient.invalidateQueries({ queryKey: ['projects'] })}
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
            <AlertDialogTitle>确认删除项目？</AlertDialogTitle>
            <AlertDialogDescription>
              {pendingDeleteProject
                ? `删除后将无法恢复，且会永久删除「${pendingDeleteProject.title}」及其相关内容。`
                : '删除后将无法恢复，且会永久删除该项目及其相关内容。'}
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
