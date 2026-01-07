'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ProjectCard } from './project-card'
import { projectApi } from '@/lib/api/project'
import { Button } from '@/components/ui/button'
import { Loader2, Plus, FolderOpen } from 'lucide-react'
import Link from 'next/link'
import { toast } from 'sonner'
import { ProjectCreateDialog } from './project-create-dialog'
import { useState } from 'react'

export function ProjectList() {
  const queryClient = useQueryClient()
  const [createDialogOpen, setCreateDialogOpen] = useState(false)

  const { data, isLoading, error } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectApi.list({ pageNum: 1, pageSize: 20 }),
  })

  const deleteMutation = useMutation({
    mutationFn: projectApi.delete,
    onSuccess: () => {
      toast.success('项目已删除')
      queryClient.invalidateQueries({ queryKey: ['projects'] })
    },
    onError: (error: any) => {
      toast.error(error.message || '删除失败')
    },
  })

  const handleDelete = (id: string) => {
    if (confirm('确定要删除这个项目吗？此操作无法撤销。')) {
      deleteMutation.mutate(id)
    }
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
    </div>
  )
}
