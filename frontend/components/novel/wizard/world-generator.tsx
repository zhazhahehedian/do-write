'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { SSEProgressModal } from '@/components/streaming/sse-progress-modal'
import { StreamingText } from '@/components/streaming/streaming-text'
import { useSSEStream } from '@/hooks/use-sse-stream'
import { Sparkles, RefreshCw, Check } from 'lucide-react'

interface WorldGeneratorProps {
  projectId: string
  initialData?: {
    timePeriod?: string
    location?: string
    atmosphere?: string
    rules?: string
  }
  onComplete?: (data: any) => void
}

export function WorldGenerator({
  projectId,
  initialData,
  onComplete,
}: WorldGeneratorProps) {
  const [worldData, setWorldData] = useState(initialData || {})
  const [isEditing, setIsEditing] = useState(false)

  const {
    isStreaming,
    progress,
    message,
    content,
    error,
    startStream,
    abort,
  } = useSSEStream({
    onComplete: () => {
      // 解析生成的内容并保存
      // TODO: 调用保存API，这里暂时假设生成的内容就是worldData的一部分，实际可能需要解析JSON
      // 由于SSE流式返回通常是markdown文本，后端可能会在done时返回结构化数据，或者我们需要解析content
      // 这里简化处理，假设用户会手动调整
    },
  })

  const handleGenerate = async () => {
    await startStream(`/api/novel/wizard/world/generate`, {
      projectId,
    })
  }

  return (
    <div className="space-y-6">
      {/* 生成进度弹窗 */}
      <SSEProgressModal
        open={isStreaming}
        title="正在生成世界观"
        message={message}
        progress={progress}
        onCancel={abort}
      />

      {/* 生成按钮 */}
      {!content && !worldData.timePeriod && (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Sparkles className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-medium mb-2">生成世界观设定</h3>
            <p className="text-muted-foreground text-center mb-6 max-w-md">
              AI 将根据您的小说主题和类型，自动生成时代背景、地点环境、
              氛围基调和世界规则。
            </p>
            <Button onClick={handleGenerate} className="bg-gradient-to-r from-primary to-accent text-white hover:opacity-90">
              <Sparkles className="mr-2 h-4 w-4" />
              开始生成
            </Button>
          </CardContent>
        </Card>
      )}

      {/* 流式内容显示 */}
      {isStreaming && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">生成中...</CardTitle>
          </CardHeader>
          <CardContent>
            <StreamingText
              content={content}
              isStreaming={isStreaming}
              className="min-h-[200px] max-h-[400px]"
            />
          </CardContent>
        </Card>
      )}

      {/* 世界观展示/编辑 */}
      {(worldData.timePeriod || content) && !isStreaming && (
        <div className="grid gap-4 md:grid-cols-2">
          <WorldSection
            title="时代背景"
            content={worldData.timePeriod || ''}
            isEditing={isEditing}
            onChange={(value) => setWorldData(prev => ({ ...prev, timePeriod: value }))}
          />
          <WorldSection
            title="地点环境"
            content={worldData.location || ''}
            isEditing={isEditing}
            onChange={(value) => setWorldData(prev => ({ ...prev, location: value }))}
          />
          <WorldSection
            title="氛围基调"
            content={worldData.atmosphere || ''}
            isEditing={isEditing}
            onChange={(value) => setWorldData(prev => ({ ...prev, atmosphere: value }))}
          />
          <WorldSection
            title="世界规则"
            content={worldData.rules || ''}
            isEditing={isEditing}
            onChange={(value) => setWorldData(prev => ({ ...prev, rules: value }))}
          />
        </div>
      )}

      {/* 操作按钮 */}
      {(worldData.timePeriod || content) && !isStreaming && (
        <div className="flex justify-between items-center">
            <div className="flex gap-2">
                 <Button variant="outline" size="sm" onClick={() => setIsEditing(!isEditing)}>
                    {isEditing ? '预览' : '编辑'}
                 </Button>
            </div>
            <div className="flex gap-2">
                <Button variant="outline" onClick={handleGenerate}>
                    <RefreshCw className="mr-2 h-4 w-4" />
                    重新生成
                </Button>
                <Button onClick={() => onComplete?.(worldData)}>
                    <Check className="mr-2 h-4 w-4" />
                    确认并继续
                </Button>
            </div>
        </div>
      )}

      {/* 错误提示 */}
      {error && (
        <div className="p-4 rounded-lg bg-destructive/10 text-destructive text-sm">
          生成失败: {error}
        </div>
      )}
    </div>
  )
}

interface WorldSectionProps {
  title: string
  content: string
  isEditing: boolean
  onChange: (value: string) => void
}

function WorldSection({ title, content, isEditing, onChange }: WorldSectionProps) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {isEditing ? (
          <Textarea
            value={content}
            onChange={(e) => onChange(e.target.value)}
            className="min-h-[120px]"
          />
        ) : (
          <p className="text-sm text-muted-foreground whitespace-pre-wrap">
            {content || '暂无内容'}
          </p>
        )}
      </CardContent>
    </Card>
  )
}
