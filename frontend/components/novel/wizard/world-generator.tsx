'use client'

import { useState, useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { SSEProgressModal } from '@/components/streaming/sse-progress-modal'
import { StreamingText } from '@/components/streaming/streaming-text'
import { useSSEStream } from '@/hooks/use-sse-stream'
import { projectApi } from '@/lib/api/project'
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
import { Sparkles, RefreshCw, Check, Loader2, Save } from 'lucide-react'
import { toast } from 'sonner'

interface WorldGeneratorProps {
  projectId: string
  readOnly?: boolean
  /** 是否在向导模式下（影响按钮文案） */
  isWizardMode?: boolean
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
  readOnly = false,
  isWizardMode = false,
  initialData,
  onComplete,
}: WorldGeneratorProps) {
  const queryClient = useQueryClient()
  const [worldData, setWorldData] = useState(initialData || {})
  const [isEditing, setIsEditing] = useState(false)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [showRegenerateConfirm, setShowRegenerateConfirm] = useState(false)

  // Sync with initialData when it changes (e.g., after page refresh)
  useEffect(() => {
    if (initialData && Object.values(initialData).some(v => v)) {
      setWorldData(initialData)
    }
  }, [initialData])

  const {
    isStreaming,
    progress,
    message,
    content,
    error,
    startStream,
    abort,
    reset,
  } = useSSEStream({
    onComplete: async () => {
      // Backend automatically parses and saves world data after SSE completes
      // We just need to fetch the updated project to get the structured fields
      setIsRefreshing(true)
      try {
        const updatedProject = await projectApi.getById(projectId)
        if (updatedProject) {
          const newWorldData = {
            timePeriod: updatedProject.worldTimePeriod || '',
            location: updatedProject.worldLocation || '',
            atmosphere: updatedProject.worldAtmosphere || '',
            rules: updatedProject.worldRules || '',
          }
          setWorldData(newWorldData)
          // Invalidate project query cache
          queryClient.invalidateQueries({ queryKey: ['project', projectId] })
          toast.success('世界观生成完成')
        }
      } catch (e) {
        console.error('Failed to fetch updated project:', e)
        toast.error('获取生成结果失败，请刷新页面')
      } finally {
        setIsRefreshing(false)
        reset() // Clear streaming content since we now have structured data
      }
    },
  })

  const handleGenerate = async () => {
    if (readOnly) return
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
            <Button
              onClick={handleGenerate}
              disabled={readOnly}
              className="bg-gradient-to-r from-primary to-accent text-white hover:opacity-90"
            >
              <Sparkles className="mr-2 h-4 w-4" />
              开始生成
            </Button>
          </CardContent>
        </Card>
      )}

      {/* 流式内容显示 */}
      {(isStreaming || isRefreshing) && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              {isRefreshing ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  正在解析数据...
                </>
              ) : (
                '生成中...'
              )}
            </CardTitle>
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
      {worldData.timePeriod && !isStreaming && !isRefreshing && (
        <div className="grid gap-4 md:grid-cols-2 max-h-[50vh] overflow-y-auto pr-2">
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

      {/* 操作按钮 - 固定在底部 */}
      {worldData.timePeriod && !isStreaming && !isRefreshing && !readOnly && (
        <div className="flex justify-between items-center sticky bottom-0 bg-background py-4 border-t mt-4">
            <div className="flex gap-2">
                 <Button variant="outline" size="sm" onClick={() => setIsEditing(!isEditing)}>
                    {isEditing ? '预览' : '编辑'}
                 </Button>
            </div>
            <div className="flex gap-2">
                <Button variant="outline" onClick={() => setShowRegenerateConfirm(true)}>
                    <RefreshCw className="mr-2 h-4 w-4" />
                    重新生成
                </Button>
                {isWizardMode ? (
                  <Button onClick={() => onComplete?.(worldData)}>
                    <Check className="mr-2 h-4 w-4" />
                    确认并继续
                  </Button>
                ) : (
                  isEditing && (
                    <Button onClick={() => {
                      onComplete?.(worldData)
                      setIsEditing(false)
                    }}>
                      <Save className="mr-2 h-4 w-4" />
                      保存修改
                    </Button>
                  )
                )}
            </div>
        </div>
      )}

      {/* 重新生成确认弹窗 */}
      <AlertDialog open={showRegenerateConfirm} onOpenChange={setShowRegenerateConfirm}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认重新生成</AlertDialogTitle>
            <AlertDialogDescription>
              重新生成将覆盖当前的世界观设定内容，此操作无法撤销。确定要继续吗？
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={() => {
              setShowRegenerateConfirm(false)
              handleGenerate()
            }}>
              确认重新生成
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

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
