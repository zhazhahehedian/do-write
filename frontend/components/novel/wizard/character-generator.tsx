'use client'

import { useState, useCallback, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Progress } from '@/components/ui/progress'
import { wizardApi } from '@/lib/api/wizard'
import { characterApi } from '@/lib/api/character'
import { useTaskPolling } from '@/hooks/use-task-polling'
import { Loader2, Users, Check, RefreshCw, User, Building2 } from 'lucide-react'
import { toast } from 'sonner'
import type { Character, RoleType } from '@/lib/types/character'
import type { GenerationTask } from '@/lib/types/wizard'

interface CharacterGeneratorProps {
  projectId: string
  readOnly?: boolean
  initialCharacters?: Character[]
  onComplete?: (characters: Character[]) => void
}

export function CharacterGenerator({
  projectId,
  readOnly = false,
  initialCharacters = [],
  onComplete,
}: CharacterGeneratorProps) {
  const [characters, setCharacters] = useState<Character[]>(initialCharacters)
  const [config, setConfig] = useState({
    protagonistCount: 1,
    supportingCount: 3,
    antagonistCount: 1,
    organizationCount: 1,
  })
  const [isGenerating, setIsGenerating] = useState(false)
  const [progress, setProgress] = useState(0)
  const [currentStep, setCurrentStep] = useState('')

  const queryClient = useQueryClient()

  const { data: loadedCharacters, isLoading: isLoadingCharacters } = useQuery({
    queryKey: ['wizard-characters', projectId],
    queryFn: () => characterApi.listByProject(projectId),
    enabled: !!projectId,
  })

  useEffect(() => {
    if (!loadedCharacters) return
    if (isGenerating) return
    setCharacters(loadedCharacters)
  }, [isGenerating, loadedCharacters])

  // 任务完成时的处理
  const handleTaskComplete = useCallback(async (task: GenerationTask) => {
    try {
      // 从服务器获取最新的角色列表
      const characterList = await characterApi.listByProject(projectId)
      setCharacters(characterList)
      setIsGenerating(false)
      setProgress(100)
      setCurrentStep('完成')

      const count = task.result?.characterCount || characterList.length
      toast.success(`成功生成 ${count} 个角色`)

      // 刷新相关查询
      queryClient.invalidateQueries({ queryKey: ['characters', projectId] })
    } catch (error: any) {
      toast.error('获取角色列表失败')
      setIsGenerating(false)
    }
  }, [projectId, queryClient])

  // 任务失败时的处理
  const handleTaskError = useCallback((task: GenerationTask) => {
    setIsGenerating(false)
    setProgress(0)
    setCurrentStep('')
    toast.error(task.errorMessage || '角色生成失败')
  }, [])

  // 进度更新时的处理
  const handleProgress = useCallback((prog: number, step: string) => {
    setProgress(prog)
    setCurrentStep(step)
  }, [])

  // 使用轮询 hook
  const { startPolling, stopPolling } = useTaskPolling({
    interval: 2000,
    onComplete: handleTaskComplete,
    onError: handleTaskError,
    onProgress: handleProgress,
  })

  // 提交异步任务
  const submitMutation = useMutation({
    mutationFn: () => wizardApi.generateCharactersAsync(projectId, config),
    onSuccess: (task) => {
      setIsGenerating(true)
      setProgress(task.progress)
      setCurrentStep(task.currentStep)
      // 开始轮询任务状态
      startPolling(task.id)
    },
    onError: (error: any) => {
      toast.error(error.message || '提交生成任务失败')
    },
  })

  const handleGenerate = () => {
    if (readOnly) return
    // 清空之前的角色（重新生成场景）
    if (characters.length > 0) {
      setCharacters([])
    }
    submitMutation.mutate()
  }

  const handleCancel = () => {
    stopPolling()
    setIsGenerating(false)
    setProgress(0)
    setCurrentStep('')
  }

  const totalCount =
    config.protagonistCount +
    config.supportingCount +
    config.antagonistCount +
    config.organizationCount

  const isPending = submitMutation.isPending || isGenerating

  return (
    <div className="space-y-6">
      {readOnly && (
        <div className="rounded-lg border border-primary/30 bg-primary/5 px-4 py-3 text-sm text-muted-foreground">
          该步骤已完成，当前为回看模式。
        </div>
      )}

      {/* 生成进度 */}
      {isGenerating && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Loader2 className="h-5 w-5 animate-spin" />
              正在生成角色...
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">{currentStep}</span>
                <span>{progress}%</span>
              </div>
              <Progress value={progress} className="h-2" />
            </div>
            <p className="text-sm text-muted-foreground">
              AI 正在根据世界观设定生成角色，这可能需要 1-2 分钟...
            </p>
            <Button variant="outline" size="sm" onClick={handleCancel}>
              取消
            </Button>
          </CardContent>
        </Card>
      )}

      {isLoadingCharacters && !isGenerating && characters.length === 0 && (
        <div className="flex justify-center py-8">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      )}

      {/* 配置表单 */}
      {!isGenerating && characters.length === 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Users className="h-5 w-5" />
              角色配置
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>主角数量</Label>
                <Input
                  type="number"
                  min={1}
                  max={3}
                  value={config.protagonistCount}
                  onChange={(e) => setConfig(prev => ({
                    ...prev,
                    protagonistCount: parseInt(e.target.value) || 1
                  }))}
                />
                <p className="text-xs text-muted-foreground">故事的核心人物</p>
              </div>
              <div className="space-y-2">
                <Label>配角数量</Label>
                <Input
                  type="number"
                  min={0}
                  max={10}
                  value={config.supportingCount}
                  onChange={(e) => setConfig(prev => ({
                    ...prev,
                    supportingCount: parseInt(e.target.value) || 0
                  }))}
                />
                <p className="text-xs text-muted-foreground">辅助推动剧情的角色</p>
              </div>
              <div className="space-y-2">
                <Label>反派数量</Label>
                <Input
                  type="number"
                  min={0}
                  max={5}
                  value={config.antagonistCount}
                  onChange={(e) => setConfig(prev => ({
                    ...prev,
                    antagonistCount: parseInt(e.target.value) || 0
                  }))}
                />
                <p className="text-xs text-muted-foreground">对抗主角的反面人物</p>
              </div>
              <div className="space-y-2">
                <Label>组织数量</Label>
                <Input
                  type="number"
                  min={0}
                  max={5}
                  value={config.organizationCount}
                  onChange={(e) => setConfig(prev => ({
                    ...prev,
                    organizationCount: parseInt(e.target.value) || 0
                  }))}
                />
                <p className="text-xs text-muted-foreground">门派、公司等组织势力</p>
              </div>
            </div>

            <Button
              onClick={handleGenerate}
              disabled={isPending || readOnly}
              className="w-full"
            >
              {isPending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  正在提交...
                </>
              ) : (
                <>
                  <Users className="mr-2 h-4 w-4" />
                  生成 {totalCount} 个角色
                </>
              )}
            </Button>
          </CardContent>
        </Card>
      )}

      {/* 角色列表展示 */}
      {!isGenerating && characters.length > 0 && (
        <>
          <div className="grid gap-4 md:grid-cols-2">
            {characters.map((char) => (
              <CharacterCard key={char.id} character={char} />
            ))}
          </div>

          {/* 操作按钮 */}
          <div className="flex justify-between">
            <Button
              variant="outline"
              onClick={handleGenerate}
              disabled={isPending || readOnly}
            >
              {isPending ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : (
                <RefreshCw className="mr-2 h-4 w-4" />
              )}
              重新生成
            </Button>
            {!readOnly && (
              <Button onClick={() => onComplete?.(characters)}>
                <Check className="mr-2 h-4 w-4" />
                确认并继续
              </Button>
            )}
          </div>
        </>
      )}
    </div>
  )
}

interface CharacterCardProps {
  character: Character
}

function CharacterCard({ character }: CharacterCardProps) {
  const isOrg = character.isOrganization === 1

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm flex items-center justify-between">
          <div className="flex items-center gap-2">
            {isOrg ? (
              <Building2 className="h-4 w-4 text-muted-foreground" />
            ) : (
              <User className="h-4 w-4 text-muted-foreground" />
            )}
            <span>{character.name}</span>
          </div>
          <span className="text-xs font-normal text-muted-foreground px-2 py-1 bg-muted rounded">
            {isOrg ? '组织' : translateRoleType(character.roleType)}
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent className="text-sm text-muted-foreground space-y-2">
        {isOrg ? (
          <>
            {character.organizationType && (
              <p><span className="font-medium text-foreground">类型:</span> {character.organizationType}</p>
            )}
            {character.organizationPurpose && (
              <p><span className="font-medium text-foreground">宗旨:</span> {character.organizationPurpose}</p>
            )}
          </>
        ) : (
          <>
            <div className="flex gap-4 text-xs">
              {character.age && <span>年龄: {character.age}</span>}
              {character.gender && <span>性别: {character.gender}</span>}
            </div>
            {character.personality && (
              <p><span className="font-medium text-foreground">性格:</span> {character.personality}</p>
            )}
            {character.background && (
              <p className="line-clamp-2"><span className="font-medium text-foreground">背景:</span> {character.background}</p>
            )}
          </>
        )}
      </CardContent>
    </Card>
  )
}

function translateRoleType(roleType?: RoleType): string {
  switch (roleType) {
    case 'protagonist': return '主角'
    case 'supporting': return '配角'
    case 'antagonist': return '反派'
    default: return '角色'
  }
}
