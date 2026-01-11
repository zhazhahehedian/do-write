'use client'

import { useState, useRef, useCallback } from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Progress } from '@/components/ui/progress'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Loader2, Sparkles, Check, AlertCircle, ChevronDown, ChevronUp } from 'lucide-react'
import { toast } from 'sonner'
import { SSEPostClient } from '@/lib/utils/sse-client'
import { outlineApi } from '@/lib/api/outline'
import type { Outline, ChapterPlan, OutlineExpandPreview } from '@/lib/types/outline'
import { useQueryClient } from '@tanstack/react-query'

interface OutlineExpandDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  projectId: string
  outline: Outline | null
  onSuccess?: () => void
}

type Step = 'config' | 'preview' | 'applying' | 'done'

const strategyOptions = [
  { value: 'balanced', label: '均衡分布', description: '情节节奏均匀，各章节戏剧张力相近' },
  { value: 'climax', label: '高潮集中', description: '前几章铺垫，后几章集中释放戏剧张力' },
  { value: 'detail', label: '细节展开', description: '充分展开每个场景，注重描写和氛围' },
]

export function OutlineExpandDialog({
  open,
  onOpenChange,
  projectId,
  outline,
  onSuccess,
}: OutlineExpandDialogProps) {
  const queryClient = useQueryClient()
  const sseClientRef = useRef<SSEPostClient | null>(null)

  // 步骤状态
  const [step, setStep] = useState<Step>('config')

  // 配置参数
  const [targetChapterCount, setTargetChapterCount] = useState(3)
  const [strategy, setStrategy] = useState<'balanced' | 'climax' | 'detail'>('balanced')
  const [enableSceneAnalysis, setEnableSceneAnalysis] = useState(false)
  const [customRequirements, setCustomRequirements] = useState('')

  // 预览结果
  const [previewResult, setPreviewResult] = useState<OutlineExpandPreview | null>(null)
  const [editingPlans, setEditingPlans] = useState<ChapterPlan[]>([])

  // 加载状态
  const [isGenerating, setIsGenerating] = useState(false)
  const [isApplying, setIsApplying] = useState(false)
  const [progress, setProgress] = useState(0)
  const [progressMessage, setProgressMessage] = useState('')
  const [error, setError] = useState<string | null>(null)

  // 展开/折叠状态
  const [expandedPlans, setExpandedPlans] = useState<Set<number>>(new Set())

  const resetState = useCallback(() => {
    setStep('config')
    setTargetChapterCount(3)
    setStrategy('balanced')
    setEnableSceneAnalysis(false)
    setCustomRequirements('')
    setPreviewResult(null)
    setEditingPlans([])
    setIsGenerating(false)
    setIsApplying(false)
    setProgress(0)
    setProgressMessage('')
    setError(null)
    setExpandedPlans(new Set())
  }, [])

  const handleClose = () => {
    if (isGenerating || isApplying) {
      sseClientRef.current?.abort()
    }
    resetState()
    onOpenChange(false)
  }

  const handleGenerate = async () => {
    if (!outline) return

    setIsGenerating(true)
    setError(null)
    setProgress(0)
    setProgressMessage('准备中...')

    const client = new SSEPostClient()
    sseClientRef.current = client

    const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || ''
    const url = `${baseUrl}/api/novel/outline/${outline.id}/expand/preview`

    try {
      await client.stream<OutlineExpandPreview>(
        url,
        {
          outlineId: outline.id,
          targetChapterCount,
          strategy,
          enableSceneAnalysis,
          customRequirements: customRequirements || undefined,
        },
        {
          onProgress: (message, prog) => {
            setProgress(prog)
            setProgressMessage(message)
          },
          onResult: (data) => {
            setPreviewResult(data)
            setEditingPlans(data.chapterPlans || [])
            setStep('preview')
          },
          onError: (err) => {
            setError(err)
            toast.error('展开生成失败: ' + err)
          },
          onComplete: () => {
            setIsGenerating(false)
          },
        }
      )
    } catch (err: any) {
      setError(err.message || '展开生成失败')
      setIsGenerating(false)
    }
  }

  const handleApply = async () => {
    if (!outline || editingPlans.length === 0) return

    setIsApplying(true)
    setStep('applying')

    try {
      await outlineApi.applyExpansion(outline.id.toString(), {
        outlineId: outline.id,
        chapterPlans: editingPlans,
        force: false,
      })

      setStep('done')
      toast.success(`已创建 ${editingPlans.length} 个子章节`)

      // 刷新数据
      queryClient.invalidateQueries({ queryKey: ['outlines', projectId] })
      queryClient.invalidateQueries({ queryKey: ['chapters', projectId] })

      onSuccess?.()

      // 2秒后自动关闭
      setTimeout(() => {
        handleClose()
      }, 2000)
    } catch (err: any) {
      setError(err.message || '应用展开失败')
      toast.error('应用展开失败: ' + err.message)
      setStep('preview')
    } finally {
      setIsApplying(false)
    }
  }

  const updatePlan = (index: number, field: keyof ChapterPlan, value: any) => {
    setEditingPlans(prev => {
      const updated = [...prev]
      updated[index] = { ...updated[index], [field]: value }
      return updated
    })
  }

  const togglePlanExpand = (index: number) => {
    setExpandedPlans(prev => {
      const next = new Set(prev)
      if (next.has(index)) {
        next.delete(index)
      } else {
        next.add(index)
      }
      return next
    })
  }

  const renderConfigStep = () => (
    <div className="space-y-6">
      <div className="rounded-lg border p-4 bg-muted/50">
        <h4 className="font-medium mb-2">大纲内容</h4>
        <p className="text-sm text-muted-foreground">{outline?.content || '暂无内容'}</p>
      </div>

      <div className="grid gap-4">
        <div className="space-y-2">
          <Label>展开为多少章</Label>
          <Select
            value={targetChapterCount.toString()}
            onValueChange={(v) => setTargetChapterCount(Number(v))}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {[2, 3, 4, 5, 6, 7, 8, 9, 10].map((n) => (
                <SelectItem key={n} value={n.toString()}>
                  {n} 章
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-2">
          <Label>展开策略</Label>
          <Select value={strategy} onValueChange={(v: any) => setStrategy(v)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {strategyOptions.map((opt) => (
                <SelectItem key={opt.value} value={opt.value}>
                  <div>
                    <div>{opt.label}</div>
                  </div>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <p className="text-xs text-muted-foreground">
            {strategyOptions.find((o) => o.value === strategy)?.description}
          </p>
        </div>

        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label>场景分析</Label>
            <p className="text-xs text-muted-foreground">为每章生成详细的场景规划</p>
          </div>
          <Switch
            checked={enableSceneAnalysis}
            onCheckedChange={setEnableSceneAnalysis}
          />
        </div>

        <div className="space-y-2">
          <Label>额外要求（可选）</Label>
          <Textarea
            placeholder="例如：注重人物心理描写、增加悬念设置..."
            value={customRequirements}
            onChange={(e) => setCustomRequirements(e.target.value)}
            rows={3}
          />
        </div>
      </div>

      {isGenerating && (
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm">
            <span>{progressMessage}</span>
            <span>{progress}%</span>
          </div>
          <Progress value={progress} />
        </div>
      )}

      {error && (
        <div className="flex items-center gap-2 text-destructive text-sm">
          <AlertCircle className="h-4 w-4" />
          <span>{error}</span>
        </div>
      )}
    </div>
  )

  const renderPreviewStep = () => (
    <ScrollArea className="h-[400px] pr-4">
      <div className="space-y-4">
        {editingPlans.map((plan, index) => (
          <Card key={index} className="overflow-hidden">
            <CardHeader
              className="cursor-pointer py-3"
              onClick={() => togglePlanExpand(index)}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Badge variant="secondary">第{plan.subIndex}章</Badge>
                  <Input
                    value={plan.title}
                    onChange={(e) => updatePlan(index, 'title', e.target.value)}
                    onClick={(e) => e.stopPropagation()}
                    className="h-7 w-48 text-sm"
                  />
                </div>
                <Button variant="ghost" size="icon" className="h-6 w-6">
                  {expandedPlans.has(index) ? (
                    <ChevronUp className="h-4 w-4" />
                  ) : (
                    <ChevronDown className="h-4 w-4" />
                  )}
                </Button>
              </div>
            </CardHeader>
            {expandedPlans.has(index) && (
              <CardContent className="pt-0 space-y-3">
                <div>
                  <Label className="text-xs">剧情摘要</Label>
                  <Textarea
                    value={plan.plotSummary}
                    onChange={(e) => updatePlan(index, 'plotSummary', e.target.value)}
                    rows={3}
                    className="mt-1 text-sm"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <Label className="text-xs">情绪基调</Label>
                    <Input
                      value={plan.emotionalTone || ''}
                      onChange={(e) => updatePlan(index, 'emotionalTone', e.target.value)}
                      className="mt-1 h-8"
                    />
                  </div>
                  <div>
                    <Label className="text-xs">预估字数</Label>
                    <Input
                      type="number"
                      value={plan.estimatedWords || 3000}
                      onChange={(e) => updatePlan(index, 'estimatedWords', Number(e.target.value))}
                      className="mt-1 h-8"
                    />
                  </div>
                </div>
                {plan.keyEvents && plan.keyEvents.length > 0 && (
                  <div>
                    <Label className="text-xs">关键事件</Label>
                    <div className="flex flex-wrap gap-1 mt-1">
                      {plan.keyEvents.map((event, i) => (
                        <Badge key={i} variant="outline" className="text-xs">
                          {event}
                        </Badge>
                      ))}
                    </div>
                  </div>
                )}
                {plan.characterFocus && plan.characterFocus.length > 0 && (
                  <div>
                    <Label className="text-xs">聚焦角色</Label>
                    <div className="flex flex-wrap gap-1 mt-1">
                      {plan.characterFocus.map((char, i) => (
                        <Badge key={i} variant="secondary" className="text-xs">
                          {char}
                        </Badge>
                      ))}
                    </div>
                  </div>
                )}
              </CardContent>
            )}
          </Card>
        ))}
      </div>
    </ScrollArea>
  )

  const renderApplyingStep = () => (
    <div className="flex flex-col items-center justify-center py-8 space-y-4">
      <Loader2 className="h-8 w-8 animate-spin text-primary" />
      <p className="text-muted-foreground">正在创建章节记录...</p>
    </div>
  )

  const renderDoneStep = () => (
    <div className="flex flex-col items-center justify-center py-8 space-y-4">
      <div className="rounded-full bg-green-100 p-3">
        <Check className="h-8 w-8 text-green-600" />
      </div>
      <p className="text-lg font-medium">展开完成</p>
      <p className="text-muted-foreground">
        已创建 {editingPlans.length} 个子章节，可在章节列表中查看
      </p>
    </div>
  )

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Sparkles className="h-5 w-5" />
            大纲展开为多章
          </DialogTitle>
          <DialogDescription>
            {outline?.title} - 将此大纲节点展开为多个详细的子章节
          </DialogDescription>
        </DialogHeader>

        {step === 'config' && renderConfigStep()}
        {step === 'preview' && renderPreviewStep()}
        {step === 'applying' && renderApplyingStep()}
        {step === 'done' && renderDoneStep()}

        <DialogFooter>
          {step === 'config' && (
            <>
              <Button variant="outline" onClick={handleClose} disabled={isGenerating}>
                取消
              </Button>
              <Button onClick={handleGenerate} disabled={isGenerating}>
                {isGenerating ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    生成中...
                  </>
                ) : (
                  <>
                    <Sparkles className="mr-2 h-4 w-4" />
                    生成规划
                  </>
                )}
              </Button>
            </>
          )}
          {step === 'preview' && (
            <>
              <Button
                variant="outline"
                onClick={() => setStep('config')}
                disabled={isApplying}
              >
                重新生成
              </Button>
              <Button onClick={handleApply} disabled={isApplying}>
                {isApplying ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    应用中...
                  </>
                ) : (
                  <>
                    <Check className="mr-2 h-4 w-4" />
                    确认创建 {editingPlans.length} 章
                  </>
                )}
              </Button>
            </>
          )}
          {step === 'done' && (
            <Button onClick={handleClose}>完成</Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
