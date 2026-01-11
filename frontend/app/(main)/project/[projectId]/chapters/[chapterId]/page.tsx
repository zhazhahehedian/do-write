'use client'

import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { chapterApi } from '@/lib/api/chapter'
import { analysisApi } from '@/lib/api/analysis'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { StreamingText } from '@/components/streaming/streaming-text'
import { SSEProgressModal } from '@/components/streaming/sse-progress-modal'
import { useSSEStream } from '@/hooks/use-sse-stream'
import { ArrowLeft, Loader2, Pencil, ScanText, Sparkles, Square } from 'lucide-react'
import { toast } from 'sonner'
import { useMemo } from 'react'

export default function ChapterDetailPage() {
  const params = useParams()
  const router = useRouter()
  const queryClient = useQueryClient()
  const projectId = params.projectId as string
  const chapterId = params.chapterId as string

  const { data: chapter, isLoading, error } = useQuery({
    queryKey: ['chapter', chapterId],
    queryFn: () => chapterApi.getDetail(chapterId),
    enabled: !!chapterId,
  })

  const { data: analysis } = useQuery({
    queryKey: ['plot-analysis', chapterId],
    queryFn: () => analysisApi.getByChapterId(chapterId),
    enabled: !!chapterId && !!chapter?.content,
    retry: false,
  })

  const analyzeMutation = useMutation({
    mutationFn: () => analysisApi.analyzeChapter(chapterId, false),
    onSuccess: async () => {
      toast.success('剧情分析完成')
      await queryClient.invalidateQueries({ queryKey: ['plot-analysis', chapterId] })
    },
    onError: (err: any) => {
      toast.error(err?.message || '剧情分析失败')
    },
  })

  const {
    isStreaming,
    progress,
    message,
    content,
    error: streamError,
    startStream,
    abort,
  } = useSSEStream({
    onComplete: () => {
      toast.success('章节生成完成')
      queryClient.invalidateQueries({ queryKey: ['chapter', chapterId] })
      queryClient.invalidateQueries({ queryKey: ['chapters', projectId] })
    },
    onError: (err) => {
      toast.error('章节生成失败: ' + err)
    },
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
  const canGenerate = chapter.subIndex > 0 && chapter.generationStatus !== 'completed'
  const isGenerating = isStreaming || chapter.generationStatus === 'generating'

  const canAnalyze = useMemo(() => {
    return !!chapter.content && chapter.generationStatus === 'completed'
  }, [chapter.content, chapter.generationStatus])

  const handleGenerate = async () => {
    if (!chapter.outlineId) {
      toast.error('缺少大纲ID，无法生成')
      return
    }

    await startStream('/api/novel/chapter/generate', {
      projectId,
      outlineId: chapter.outlineId.toString(),
      subIndex: chapter.subIndex,
      styleCode: chapter.styleCode || undefined,
    })
  }

  const showStreamContent = isStreaming || (!!content && chapter.generationStatus !== 'completed')

  return (
    <div className="container max-w-4xl mx-auto py-8 space-y-6">
      <SSEProgressModal
        open={isStreaming && !content}
        title="正在生成章节"
        message={message || '生成中...'}
        progress={progress}
        onCancel={abort}
      />

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

        <div className="flex items-center gap-2">
          {canGenerate && (
            isGenerating ? (
              <Button variant="outline" onClick={abort}>
                <Square className="mr-2 h-4 w-4" />
                停止生成
              </Button>
            ) : (
              <Button onClick={handleGenerate}>
                <Sparkles className="mr-2 h-4 w-4" />
                生成正文
              </Button>
            )
          )}

          {canAnalyze && (
            <Button
              variant="outline"
              disabled={analyzeMutation.isPending}
              onClick={() => analyzeMutation.mutate()}
            >
              {analyzeMutation.isPending ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : (
                <ScanText className="mr-2 h-4 w-4" />
              )}
              剧情分析
            </Button>
          )}

          {canEdit && (
            <Button asChild>
              <Link href={`/project/${projectId}/chapters/${chapterId}/edit`}>
                <Pencil className="mr-2 h-4 w-4" />
                编辑
              </Link>
            </Button>
          )}
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">章节内容</CardTitle>
        </CardHeader>
        <CardContent>
          {streamError && (
            <div className="mb-4 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
              {streamError}
            </div>
          )}
          <StreamingText
            content={showStreamContent ? content : (chapter.content || '暂无内容')}
            isStreaming={isStreaming}
            className="min-h-[400px] max-h-[70vh]"
          />
        </CardContent>
      </Card>

      {!!analysis?.analysisReport && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">剧情分析报告</CardTitle>
          </CardHeader>
          <CardContent>
            <StreamingText
              content={analysis.analysisReport}
              isStreaming={false}
              className="max-h-[70vh]"
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
