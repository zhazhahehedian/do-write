'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { SSEProgressModal } from '@/components/streaming/sse-progress-modal'
import { StreamingText } from '@/components/streaming/streaming-text'
import { useSSEStream } from '@/hooks/use-sse-stream'
import type { Outline } from '@/lib/types/outline'
import { FileText, Sparkles } from 'lucide-react'
import { toast } from 'sonner'

interface ChapterGenerateDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  projectId: string
  outline: Outline | null
  onSuccess?: () => void
}

const writingStyles = [
  { value: 'default', label: '默认风格' },
  { value: 'literary', label: '文学风格' },
  { value: 'web_novel', label: '网文风格' },
  { value: 'light_novel', label: '轻小说风格' },
  { value: 'martial_arts', label: '武侠风格' },
  { value: 'suspense', label: '悬疑风格' },
]

export function ChapterGenerateDialog({
  open,
  onOpenChange,
  projectId,
  outline,
  onSuccess,
}: ChapterGenerateDialogProps) {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [writingStyle, setWritingStyle] = useState('default')
  const [customRequirements, setCustomRequirements] = useState('')

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
    onComplete: () => {
      toast.success('章节生成完成')
      queryClient.invalidateQueries({ queryKey: ['chapters', projectId] })
      onSuccess?.()
    },
  })

  const handleGenerate = async () => {
    if (!outline) return

    await startStream(`/api/novel/chapter/generate`, {
      projectId: projectId,
      outlineId: outline.id.toString(),
      styleCode: writingStyle !== 'default' ? writingStyle : undefined,
      customRequirements: customRequirements.trim() || undefined,
    })
  }

  const handleClose = () => {
    if (isStreaming) {
      abort()
    }
    reset()
    setCustomRequirements('')
    onOpenChange(false)
  }

  const handleViewChapters = () => {
    handleClose()
    router.push(`/project/${projectId}/chapters`)
  }

  if (!outline) return null

  return (
    <>
      <SSEProgressModal
        open={isStreaming && !content}
        title="正在生成章节"
        message={message || '准备中...'}
        progress={progress}
        onCancel={abort}
      />

      <Dialog open={open && !isStreaming} onOpenChange={handleClose}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <FileText className="h-5 w-5" />
              生成章节
            </DialogTitle>
            <DialogDescription>
              基于大纲「{outline.title}」生成章节内容
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>大纲内容</Label>
              <div className="p-3 rounded-md bg-muted text-sm">
                {outline.content || '暂无大纲内容'}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="writingStyle">写作风格</Label>
              <Select value={writingStyle} onValueChange={setWritingStyle}>
                <SelectTrigger>
                  <SelectValue placeholder="选择写作风格" />
                </SelectTrigger>
                <SelectContent>
                  {writingStyles.map(style => (
                    <SelectItem key={style.value} value={style.value}>
                      {style.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="customRequirements">自定义要求（可选）</Label>
              <Textarea
                id="customRequirements"
                value={customRequirements}
                onChange={(e) => setCustomRequirements(e.target.value)}
                placeholder="输入额外的写作要求，如特定的情节走向、情感氛围等"
                rows={3}
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={handleClose}>
              取消
            </Button>
            <Button onClick={handleGenerate}>
              <Sparkles className="mr-2 h-4 w-4" />
              开始生成
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Streaming content display dialog */}
      <Dialog open={isStreaming && !!content} onOpenChange={() => {}}>
        <DialogContent className="sm:max-w-[800px] max-h-[80vh]" showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>正在生成: {outline.title}</DialogTitle>
          </DialogHeader>

          <div className="flex-1 overflow-hidden">
            <StreamingText
              content={content}
              isStreaming={isStreaming}
              className="min-h-[300px] max-h-[50vh]"
            />
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={abort}>
              停止生成
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Completion dialog */}
      <Dialog open={!isStreaming && !!content && !error} onOpenChange={handleClose}>
        <DialogContent className="sm:max-w-[800px] max-h-[80vh]">
          <DialogHeader>
            <DialogTitle>章节生成完成</DialogTitle>
            <DialogDescription>
              「{outline.title}」已生成完成
            </DialogDescription>
          </DialogHeader>

          <div className="flex-1 overflow-hidden">
            <StreamingText
              content={content}
              isStreaming={false}
              className="min-h-[200px] max-h-[50vh]"
            />
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={handleClose}>
              关闭
            </Button>
            <Button onClick={handleViewChapters}>
              查看章节列表
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Error display */}
      {error && (
        <Dialog open={!!error} onOpenChange={handleClose}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>生成失败</DialogTitle>
            </DialogHeader>
            <div className="p-4 rounded-lg bg-destructive/10 text-destructive text-sm">
              {error}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={handleClose}>
                关闭
              </Button>
              <Button onClick={handleGenerate}>
                重试
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </>
  )
}
