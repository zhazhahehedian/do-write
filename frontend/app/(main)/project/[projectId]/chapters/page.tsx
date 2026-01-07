'use client'

import { useParams } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ChapterList } from '@/components/novel/chapter/chapter-list'
import { chapterApi } from '@/lib/api/chapter'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Plus } from 'lucide-react'

export default function ChaptersPage() {
  const params = useParams()
  const projectId = params.projectId as string
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['chapters', projectId],
    queryFn: () => chapterApi.list(projectId, { pageNum: 1, pageSize: 100 }),
  })

  const generateMutation = useMutation({
    mutationFn: (chapterId: string) => chapterApi.regenerate(chapterId, {}), // Using regenerate for manual trigger for now, or need specific generate api call
    onSuccess: () => {
      toast.success('开始生成章节')
      queryClient.invalidateQueries({ queryKey: ['chapters', projectId] })
    },
    onError: (error: any) => {
      toast.error(error.message || '生成失败')
    }
  })

  const deleteMutation = useMutation({
    mutationFn: chapterApi.delete,
    onSuccess: () => {
      toast.success('章节已删除')
      queryClient.invalidateQueries({ queryKey: ['chapters', projectId] })
    },
  })

  return (
    <div className="space-y-6">
       <div className="flex items-center justify-end">
        <Button onClick={() => toast.info('请在向导或大纲中生成章节')}>
          <Plus className="mr-2 h-4 w-4" />
          新增章节
        </Button>
      </div>
      
      <ChapterList
        chapters={data?.list || []}
        projectId={projectId}
        onGenerate={(id) => toast.info('请使用右键菜单或详情页进行生成')}
        onRegenerate={(id) => generateMutation.mutate(id)}
        onDelete={(id) => {
            if(confirm('确定要删除此章节吗？')) deleteMutation.mutate(id)
        }}
      />
    </div>
  )
}