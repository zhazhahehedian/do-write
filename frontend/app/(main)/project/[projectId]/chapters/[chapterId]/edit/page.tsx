'use client'

import { useEffect, useMemo, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { chapterApi } from '@/lib/api/chapter'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Loader2, Save, ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'

export default function ChapterEditPage() {
  const params = useParams()
  const router = useRouter()
  const queryClient = useQueryClient()

  const projectId = params.projectId as string
  const chapterId = params.chapterId as string

  const { data: chapter, isLoading } = useQuery({
    queryKey: ['chapter', chapterId],
    queryFn: () => chapterApi.getDetail(chapterId),
    enabled: !!chapterId,
  })

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')

  useEffect(() => {
    if (!chapter) return
    setTitle(chapter.title || '')
    setContent(chapter.content || '')
  }, [chapter])

  const canEdit = useMemo(() => {
    return chapter?.generationStatus === 'completed'
  }, [chapter?.generationStatus])

  const saveMutation = useMutation({
    mutationFn: () => chapterApi.update(chapterId, { title, content }),
    onSuccess: async () => {
      toast.success('保存成功')
      await queryClient.invalidateQueries({ queryKey: ['chapter', chapterId] })
      await queryClient.invalidateQueries({ queryKey: ['chapters', projectId] })
      router.push(`/project/${projectId}/chapters/${chapterId}`)
    },
    onError: (error: unknown) => {
      toast.error((error as Error)?.message || '保存失败')
    },
  })

  const handleBack = () => {
    router.push(`/project/${projectId}/chapters/${chapterId}`)
  }

  if (isLoading) {
    return (
      <div className="flex h-[400px] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (!chapter) {
    return (
      <div className="container max-w-4xl mx-auto py-8">
        <Card>
          <CardHeader>
            <CardTitle>章节不存在或加载失败</CardTitle>
          </CardHeader>
          <CardContent>
            <Button variant="outline" onClick={() => router.push(`/project/${projectId}/chapters`)}>
              返回章节列表
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="container max-w-4xl mx-auto py-8 space-y-6">
      <div className="flex items-center justify-between gap-4">
        <Button variant="ghost" onClick={handleBack}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          返回
        </Button>
        <Button
          onClick={() => saveMutation.mutate()}
          disabled={!canEdit || saveMutation.isPending}
        >
          {saveMutation.isPending ? (
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          ) : (
            <Save className="mr-2 h-4 w-4" />
          )}
          保存
        </Button>
      </div>

      {!canEdit && (
        <Card className="border-dashed">
          <CardContent className="py-6 text-sm text-muted-foreground">
            当前章节未处于“已完成”状态，暂不支持编辑。
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">编辑章节</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <div className="text-sm font-medium">标题</div>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="请输入章节标题"
              disabled={!canEdit}
            />
          </div>

          <div className="space-y-2">
            <div className="text-sm font-medium">内容</div>
            <Textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="请输入章节内容"
              className="min-h-[420px]"
              disabled={!canEdit}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

