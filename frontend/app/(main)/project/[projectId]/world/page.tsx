'use client'

import { useParams } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { WorldGenerator } from '@/components/novel/wizard/world-generator'
import { projectApi } from '@/lib/api/project'
import { toast } from 'sonner'
import { Loader2 } from 'lucide-react'

export default function WorldPage() {
  const params = useParams()
  const projectId = params.projectId as string
  const queryClient = useQueryClient()

  const { data: project, isLoading } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.getById(projectId),
  })

  const updateMutation = useMutation({
    mutationFn: (data: any) => projectApi.update(projectId, {
        id: Number(projectId),
        worldTimePeriod: data.timePeriod,
        worldLocation: data.location,
        worldAtmosphere: data.atmosphere,
        worldRules: data.rules,
    }),
    onSuccess: () => {
      toast.success('世界观设定已更新')
      queryClient.invalidateQueries({ queryKey: ['project', projectId] })
    },
    onError: (error: any) => {
      toast.error(error.message || '更新失败')
    }
  })

  if (isLoading) {
    return (
      <div className="flex h-[400px] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (!project) return <div>项目不存在</div>

  const initialData = {
    timePeriod: project.worldTimePeriod,
    location: project.worldLocation,
    atmosphere: project.worldAtmosphere,
    rules: project.worldRules
  }

  return (
    <div className="max-w-4xl mx-auto">
      <WorldGenerator 
        projectId={projectId} 
        initialData={initialData}
        onComplete={(data) => updateMutation.mutate(data)}
      />
    </div>
  )
}