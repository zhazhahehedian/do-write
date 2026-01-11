'use client'

import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { chapterApi } from '@/lib/api/chapter'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { StreamingText } from '@/components/streaming/streaming-text'
import { ArrowLeft, Loader2, Pencil } from 'lucide-react'

export default function ChapterDetailPage() {
  const params = useParams()
  const router = useRouter()
  const projectId = params.projectId as string
  const chapterId = params.chapterId as string

  const { data: chapter, isLoading, error } = useQuery({
    queryKey: ['chapter', chapterId],
    queryFn: () => chapterApi.getDetail(chapterId),
    enabled: !!chapterId,
  })

  if (isLoading) {
    return (
      <div className="flex h-[400px] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error || !chapter) {
    return (
      <div className="container max-w-4xl mx-auto py-8">
        <Card>
          <CardHeader>
            <CardTitle>章节不存在或加载失败</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">
              请返回章节列表重试。
            </p>
            <Button variant="outline" onClick={() => router.push(`/project/${projectId}/chapters`)}>
              返回章节列表
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  const canEdit = chapter.generationStatus === 'completed'

  return (
    <div className="container max-w-4xl mx-auto py-8 space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" asChild>
            <Link href={`/project/${projectId}/chapters`}>
              <ArrowLeft className="h-4 w-4" />
            </Link>
          </Button>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold">
                第{chapter.chapterNumber}章{chapter.subIndex > 0 ? `-${chapter.subIndex}` : ''}
              </h1>
              <Badge variant="outline">{chapter.generationStatus}</Badge>
            </div>
            <p className="text-sm text-muted-foreground mt-1">
              {chapter.title || '未命名章节'}
            </p>
          </div>
        </div>

        {canEdit && (
          <Button asChild>
            <Link href={`/project/${projectId}/chapters/${chapterId}/edit`}>
              <Pencil className="mr-2 h-4 w-4" />
              编辑
            </Link>
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">章节内容</CardTitle>
        </CardHeader>
        <CardContent>
          <StreamingText
            content={chapter.content || '暂无内容'}
            isStreaming={false}
            className="min-h-[400px] max-h-[70vh]"
          />
        </CardContent>
      </Card>
    </div>
  )
}

